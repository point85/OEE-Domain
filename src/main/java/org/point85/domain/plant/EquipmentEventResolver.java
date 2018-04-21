package org.point85.domain.plant;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.EventType;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ResolverFunction;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquipmentEventResolver {
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// Nashorn script engine
	public static final String SCRIPT_ENGINE_NAME = "nashorn";

	// reason cache
	private ConcurrentMap<String, Reason> reasonCache = new ConcurrentHashMap<>();

	// material cache
	private ConcurrentMap<String, Material> materialCache = new ConcurrentHashMap<>();

	// resolvers by source id
	private ConcurrentMap<Equipment, List<EventResolver>> resolverCache = new ConcurrentHashMap<>();

	private ScriptEngine scriptEngine;

	public EquipmentEventResolver() {
		scriptEngine = new ScriptEngineManager().getEngineByName(SCRIPT_ENGINE_NAME);
	}

	public void clearCache() {
		reasonCache.clear();
		materialCache.clear();
		resolverCache.clear();
	}

	public ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	private void cacheResolvers() {
		if (resolverCache.size() == 0) {
			// query db
			List<EventResolver> resolvers = PersistenceService.instance().fetchEventResolvers();

			for (EventResolver resolver : resolvers) {

				Equipment equipment = resolver.getEquipment();

				List<EventResolver> equipmentResolvers = resolverCache.get(equipment);

				if (equipmentResolvers == null) {
					equipmentResolvers = new ArrayList<>();
					resolverCache.put(equipment, equipmentResolvers);
				}
				equipmentResolvers.add(resolver);
			}
		}
	}

	// find the resolver(s) by type
	public List<EventResolver> getResolvers(Equipment equipment) throws Exception {
		cacheResolvers();

		return resolverCache.get(equipment);
	}

	// find the resolver by source id (must be unique)
	public EventResolver getResolver(String sourceId) throws Exception {
		cacheResolvers();

		Collection<List<EventResolver>> equipmentResolvers = resolverCache.values();

		EventResolver configuredResolver = null;

		for (List<EventResolver> resolvers : equipmentResolvers) {
			for (EventResolver resolver : resolvers) {
				if (resolver.getSourceId().equals(sourceId)) {
					configuredResolver = resolver;
					break;
				}
			}
		}

		return configuredResolver;
	}

	public OeeEvent invokeResolver(EventResolver eventResolver, OeeContext context, Object sourceValue,
			OffsetDateTime dateTime) throws Exception {

		Equipment equipment = eventResolver.getEquipment();
		String sourceId = eventResolver.getSourceId();
		EventType resolverType = eventResolver.getType();
		String script = eventResolver.getScript();

		// event durations must exceed the update period
		if (eventResolver.getLastTimestamp() != null) {
			DataSourceType fromSource = eventResolver.getDataSource().getDataSourceType();

			if (!fromSource.equals(DataSourceType.OPC_UA) && !fromSource.equals(DataSourceType.OPC_DA)) {
				Duration delta = Duration.between(eventResolver.getLastTimestamp(), dateTime);
				Duration threshold = Duration.ofMillis(eventResolver.getUpdatePeriod());

				if (delta.compareTo(threshold) != 1) {
					throw new Exception("The event duration of " + DomainUtils.formatDuration(delta) + " for source id "
							+ sourceId + " for equipment " + equipment.getName() + " must exceed the threshold of "
							+ DomainUtils.formatDuration(threshold));
				}
			}
		}

		if (script == null) {
			logger.warn("The event script is not defined for source id " + sourceId + " for equipment "
					+ equipment.getName());
		}

		if (logger.isInfoEnabled()) {
			logger.info("Invoking script resolver for source id " + sourceId + " and type " + resolverType
					+ " with source value " + sourceValue + " for script \n" + script);
		}

		ResolverFunction resolverFunction = new ResolverFunction(script);

		// for production counts
		if (eventResolver.getLastValue() == null) {
			eventResolver.setLastValue(sourceValue);
		}

		// result of script
		Object result = resolverFunction.invoke(getScriptEngine(), context, sourceValue, eventResolver.getLastValue());

		if (logger.isInfoEnabled()) {
			logger.info("Result: " + result);
		}

		// save last value
		eventResolver.setLastValue(sourceValue);
		eventResolver.setLastTimestamp(dateTime);

		// set shift
		WorkSchedule schedule = equipment.findWorkSchedule();
		Shift shift = null;

		if (schedule != null) {
			List<ShiftInstance> shiftInstances = schedule.getShiftInstancesForTime(dateTime.toLocalDateTime());

			if (shiftInstances.size() > 0) {
				// pick first one
				shift = shiftInstances.get(0).getShift();
			}
		}

		// set material
		Material material = null;
		if (eventResolver.getType().equals(EventType.MATL_CHANGE)) {
			// set material from event
			material = fetchMaterial((String) sourceValue);
			context.setMaterial(equipment, material);
		} else {
			// set material from context
			material = context.getMaterial(equipment);
		}

		// set job
		String job = null;
		if (eventResolver.getType().equals(EventType.JOB_CHANGE)) {
			// set job from event
			job = (String) sourceValue;
			context.setJob(equipment, job);
		} else {
			// set job from context
			job = context.getJob(equipment);
		}

		// fill in resolution
		OeeEvent event = new OeeEvent(equipment, sourceValue, result);

		// specific processing
		if (result != null) {
			switch (resolverType) {
			case AVAILABILITY: {
				processReason(event);
				break;
			}
			case JOB_CHANGE: {
				processJob(event);
				break;
			}
			case MATL_CHANGE: {
				processMaterial(event);
				break;
			}
			case OTHER:
				break;
			case PROD_GOOD:
			case PROD_REJECT:
			case PROD_STARTUP: {
				processProduction(event, resolverType, material, context);
				break;
			}
			default:
				break;
			}
		}

		// common attributes
		event.setEventType(resolverType);
		event.setItemId(sourceId);
		event.setStartTime(dateTime);
		event.setShift(shift);

		if (logger.isInfoEnabled()) {
			logger.info(event.toString());
		}

		return event;
	}

	// production counts
	private void processProduction(OeeEvent resolvedItem, EventType resolverType, Material material, OeeContext context)
			throws Exception {
		Object outputValue = resolvedItem.getOutputValue();
		Double amount = null;

		if (outputValue instanceof String) {
			// convert to double
			amount = Double.valueOf((String) outputValue);
		} else if (outputValue instanceof Double) {
			amount = (Double) outputValue;
		} else if (outputValue instanceof Integer) {
			amount = Double.valueOf((Integer) outputValue);
		} else if (outputValue instanceof Float) {
			amount = Double.valueOf((Float) outputValue);
		} else if (outputValue instanceof Long) {
			amount = Double.valueOf((Long) outputValue);
		} else if (outputValue instanceof Short) {
			amount = Double.valueOf((Short) outputValue);
		} else if (outputValue instanceof Byte) {
			amount = Double.valueOf((Byte) outputValue);
		} else {
			throw new Exception("The result " + outputValue + " cannot be converted to a number.");
		}

		// get UOM from material and equipment
		if (material == null) {
			EquipmentMaterial eqm = resolvedItem.getEquipment().getDefaultEquipmentMaterial();

			if (eqm != null) {
				material = eqm.getMaterial();

				// set material into context too
				context.setMaterial(eqm.getEquipment(), material);

				if (logger.isInfoEnabled()) {
					logger.info("Produced material is not defined.  Using default of " + material.getName());
				}
			}
		}

		UnitOfMeasure uom = resolvedItem.getEquipment().getUOM(material, resolverType);
		resolvedItem.setAmount(amount);
		resolvedItem.setUOM(uom);
	}

	// availability
	private Reason processReason(OeeEvent resolvedItem) throws Exception {
		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception("The result " + resolvedItem.getOutputValue() + " is not a reason code.");
		}

		// get the reason
		String reasonName = (String) resolvedItem.getOutputValue();

		Reason reason = this.reasonCache.get(reasonName);

		if (reason == null) {
			// fetch from database
			reason = PersistenceService.instance().fetchReasonByName(reasonName);

			// cache it
			if (reason != null) {
				reasonCache.put(reason.getName(), reason);
			} else {
				throw new Exception(reasonName + " is not a valid reason.");
			}
		}
		resolvedItem.setReason(reason);

		return reason;
	}

	// job
	private String processJob(OeeEvent resolvedItem) throws Exception {
		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception("The result " + resolvedItem.getOutputValue() + " is not a job identifier.");
		}
		String job = (String) resolvedItem.getOutputValue();
		resolvedItem.setJob(job);
		return job;
	}

	private Material fetchMaterial(String materialName) throws Exception {
		Material material = null;

		if (materialName == null) {
			return material;
		}

		material = materialCache.get(materialName);

		if (material == null) {
			// fetch from database
			material = PersistenceService.instance().fetchMaterialByName(materialName);

			// cache it
			if (material != null) {
				materialCache.put(material.getName(), material);
			} else {
				throw new Exception("Material " + materialName + " not found in database.");
			}
		}
		return material;
	}

	// material
	private Material processMaterial(OeeEvent resolvedItem) throws Exception {

		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception(resolvedItem.getOutputValue() + " is not the name of a material.");
		}

		// get the material
		String materialName = (String) resolvedItem.getOutputValue();
		Material material = fetchMaterial(materialName);
		resolvedItem.setMaterial(material);
		return material;
	}

}
