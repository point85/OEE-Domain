package org.point85.domain.plant;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.ScriptEngine;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.script.ResolverFunction;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquipmentEventResolver {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(EquipmentEventResolver.class);

	// reason cache
	private final ConcurrentMap<String, Reason> reasonCache = new ConcurrentHashMap<>();

	// material cache
	private final ConcurrentMap<String, Material> materialCache = new ConcurrentHashMap<>();

	// resolvers by source id
	private final ConcurrentMap<PlantEntity, List<EventResolver>> resolverCache = new ConcurrentHashMap<>();

	// engine to evaluate java script
	private final ScriptEngine scriptEngine;

	public EquipmentEventResolver() {
		scriptEngine = new NashornScriptEngineFactory().getScriptEngine();
	}

	public void clearCache() {
		reasonCache.clear();
		materialCache.clear();
		resolverCache.clear();
	}

	public ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	private void cacheResolvers() throws Exception {
		if (resolverCache.size() == 0) {
			// query db
			List<EventResolver> resolvers = PersistenceService.instance().fetchEventResolvers();

			for (EventResolver resolver : resolvers) {

				PlantEntity entity = resolver.getPlantEntity();

				List<EventResolver> equipmentResolvers = resolverCache.get(entity);

				if (equipmentResolvers == null) {
					equipmentResolvers = new ArrayList<>();
					resolverCache.put(entity, equipmentResolvers);
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

		if (configuredResolver == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.resolver", sourceId));
		}

		return configuredResolver;
	}

	public OeeEvent invokeResolver(EventResolver eventResolver, OeeContext context, Object sourceValue,
			OffsetDateTime dateTime) throws Exception {

		String sourceId = eventResolver.getSourceId();
		OeeEventType resolverType = eventResolver.getType();
		String script = eventResolver.getScript();
		DataCollector collector = eventResolver.getCollector();

		if (sourceValue == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.source.value", sourceId,
					eventResolver.getPlantEntity().getName()));
		}

		if (script == null || script.length() == 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.script", sourceId,
					eventResolver.getPlantEntity().getName()));
		}

		if (logger.isInfoEnabled()) {
			logger.info("Invoking script resolver for source id " + sourceId + " and type " + resolverType
					+ " with source value " + sourceValue);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("for script \n" + script);
		}

		// result of script execution
		ResolverFunction resolverFunction = new ResolverFunction(script);
		Object result = resolverFunction.invoke(getScriptEngine(), context, sourceValue, eventResolver);

		if (logger.isInfoEnabled()) {
			logger.info("Result: " + result);
		}

		// set last value. Could have been set in script.
		if (eventResolver.getLastValue() == null) {
			eventResolver.setLastValue(sourceValue);
		}

		// a null result means to ignore the script execution
		if (result == null) {
			return null;
		}

		// create event
		OeeEvent event = new OeeEvent(eventResolver.getEquipment(), eventResolver.getLastValue(), result);

		// an event time could have been set in the resolver
		OffsetDateTime eventTime = dateTime;
		if (eventResolver.getTimestamp() != null) {
			eventTime = eventResolver.getTimestamp();
		}

		if (eventTime == null) {
			eventTime = OffsetDateTime.now();
		}
		event.setStartTime(eventTime);

		if (collector != null) {
			event.setCollector(collector.getName());
		}

		// resolver type
		event.setEventType(resolverType);

		// source id
		event.setSourceId(sourceId);

		// set shift and team
		setShift(event, eventResolver, eventTime);

		// set material
		setMaterial(event, eventResolver, context, sourceValue, result);

		// set job
		setJob(event, eventResolver, context, result);

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

			case PROD_GOOD:
			case PROD_REJECT:
			case PROD_STARTUP: {
				// the script could have set a reason name (e.g. for reject production)
				String reasonName = eventResolver.getReason();
				processProduction(event, resolverType, context, reasonName);
				break;
			}

			case CUSTOM:
			default:
				break;
			}
		}

		if (logger.isInfoEnabled()) {
			logger.info("Resolved event. " + event.toString());
		}

		return event;
	}

	private void setShift(OeeEvent event, EventResolver eventResolver, OffsetDateTime eventTime) throws Exception {
		// set shift
		WorkSchedule schedule = eventResolver.getPlantEntity().findWorkSchedule();
		Shift shift = null;
		Team team = null;

		if (schedule != null) {
			List<ShiftInstance> shiftInstances = schedule.getShiftInstancesForTime(eventTime.toLocalDateTime());

			if (!shiftInstances.isEmpty()) {
				// pick first one
				shift = shiftInstances.get(0).getShift();
				team = shiftInstances.get(0).getTeam();
			}
		}
		event.setShift(shift);
		event.setTeam(team);
	}

	private void setMaterial(OeeEvent event, EventResolver eventResolver, OeeContext context, Object sourceValue,
			Object result) throws Exception {
		Material material = null;
		Equipment equipment = eventResolver.getEquipment();

		if (equipment == null) {
			return;
		}

		if (eventResolver.getType().equals(OeeEventType.MATL_CHANGE)) {
			// set material from event
			material = fetchMaterial((String) sourceValue);
			context.setMaterial(equipment, material);
		} else {
			// get material from context
			material = context.getMaterial(equipment);

			if (material == null) {
				// query for last setup
				OeeEvent setup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

				if (setup != null) {
					// material
					material = setup.getMaterial();
					context.setMaterial(equipment, material);

					// job name
					context.setJob(equipment, setup.getJob());
				}
			}

			if (material == null && equipment.getDefaultEquipmentMaterial() != null) {
				// use the default material if defined
				Material defaultMaterial = equipment.getDefaultEquipmentMaterial().getMaterial();

				if (defaultMaterial != null) {
					context.setMaterial(equipment, defaultMaterial);
				} else {
					logger.warn("The produced material for this event is not defined.");
				}
			}
		}
		event.setMaterial(material);
	}

	private void setJob(OeeEvent event, EventResolver eventResolver, OeeContext context, Object result) {
		String job = null;
		Equipment equipment = eventResolver.getEquipment();

		if (equipment != null) {
			if (eventResolver.getType().equals(OeeEventType.JOB_CHANGE)) {
				// set job from event
				job = (String) result;
				context.setJob(equipment, job);
			} else {
				// set job from context
				job = context.getJob(equipment);
			}
		}
		event.setJob(job);
	}

	// production counts
	private void processProduction(OeeEvent resolvedEvent, OeeEventType resolverType, OeeContext context,
			String reasonName) throws Exception {
		Object outputValue = resolvedEvent.getOutputValue();
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
			throw new Exception(DomainLocalizer.instance().getErrorString("can.not.convert", outputValue,
					outputValue.getClass().getSimpleName()));
		}

		// get UOM from material and equipment
		Material producedMaterial = resolvedEvent.getMaterial();

		if (producedMaterial == null) {
			EquipmentMaterial eqm = resolvedEvent.getEquipment().getDefaultEquipmentMaterial();

			if (eqm != null) {
				producedMaterial = eqm.getMaterial();

				// set material into context too
				context.setMaterial(eqm.getEquipment(), producedMaterial);

				if (logger.isInfoEnabled()) {
					logger.info("Produced material is not defined.  Using default of " + producedMaterial.getName());
				}
			}
		}

		UnitOfMeasure uom = resolvedEvent.getEquipment().getUOM(producedMaterial, resolverType);
		resolvedEvent.setAmount(amount);
		resolvedEvent.setUOM(uom);

		if (logger.isInfoEnabled()) {
			logger.info(resolverType.toString() + " amount is " + amount + " " + uom);
		}

		// set the quality reason
		if (reasonName != null) {
			Reason reason = fetchReason(reasonName);
			resolvedEvent.setReason(reason);
		}
	}

	private Reason fetchReason(String reasonName) throws Exception {
		Reason reason = this.reasonCache.get(reasonName);

		if (reason == null) {
			// fetch from database
			reason = PersistenceService.instance().fetchReasonByName(reasonName);

			// cache it
			if (reason != null) {
				reasonCache.put(reason.getName(), reason);
			} else {
				throw new Exception(DomainLocalizer.instance().getErrorString("invalid.reason", reasonName));
			}
		}
		return reason;
	}

	// availability and production
	private Reason processReason(OeeEvent resolvedItem) throws Exception {
		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("invalid.code", resolvedItem.getOutputValue()));
		}

		// get the reason
		String reasonName = (String) resolvedItem.getOutputValue();
		Reason reason = fetchReason(reasonName);
		resolvedItem.setReason(reason);

		return reason;
	}

	// job
	private String processJob(OeeEvent resolvedItem) throws Exception {
		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("invalid.job", resolvedItem.getOutputValue()));
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
				throw new Exception(DomainLocalizer.instance().getErrorString("no.material", materialName));
			}
		}
		return material;
	}

	// material
	private Material processMaterial(OeeEvent resolvedItem) throws Exception {

		if (!(resolvedItem.getOutputValue() instanceof String)) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("invalid.material", resolvedItem.getOutputValue()));
		}

		// get the material
		String materialName = (String) resolvedItem.getOutputValue();
		Material material = fetchMaterial(materialName);
		resolvedItem.setMaterial(material);
		return material;
	}

}
