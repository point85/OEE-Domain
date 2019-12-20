package org.point85.domain.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.db.DatabaseEvent;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.db.DatabaseEventStatus;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpSource;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.jms.JmsSource;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.EntitySchedule;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.schedule.ExceptionPeriod;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.RotationSegment;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.MeasurementType;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * PersistenceService handles all database I/O. It is a singleton.
 *
 */
public final class PersistenceService {
	// logger
	private static Logger logger;

	// persistence unit name for OEE tables
	private static final String PU_NAME = "OEE";

	// persistence unit name for interface table
	private static final String DB_PU_NAME = "OEE_DB_IF";

	// time in sec to wait for EntityManagerFactory creation to complete
	private static final int EMF_CREATION_TO_SEC = 30;

	// entity manager factory
	private EntityManagerFactory emf;

	// singleton service
	private static PersistenceService persistenceService;

	// the EMF future
	private CompletableFuture<EntityManagerFactory> emfFuture;

	// map of named queries
	private final Map<String, Boolean> namedQueryMap;

	// cached JDBC connection info
	private static String jdbcConnection;
	private static String jdbcUserName;
	private static String jdbcPassword;

	private PersistenceService() {
		namedQueryMap = new ConcurrentHashMap<>();
	}

	public synchronized static PersistenceService instance() {
		if (persistenceService == null) {
			persistenceService = new PersistenceService();
		}
		return persistenceService;
	}

	public synchronized static PersistenceService create() {
		return new PersistenceService();
	}

	private Logger getLogger() {
		if (logger == null) {
			logger = LoggerFactory.getLogger(PersistenceService.class);
		}
		return logger;
	}

	public void initialize(String jdbcUrl, String userName, String password) {
		// cache connection info
		jdbcConnection = jdbcUrl;
		jdbcUserName = userName;
		jdbcPassword = password;

		// create EM on a a background thread
		emfFuture = CompletableFuture.supplyAsync(() -> {
			try {
				// create the EMF
				createContainerManagedEntityManagerFactory(jdbcUrl, userName, password);

				// cache base UOMs
				primeUomCache();
			} catch (Exception e) {
				getLogger().error(e.getMessage());
			}
			return emf;
		});
	}

	private void primeUomCache() throws Exception {
		// load cache with fundamental units
		fetchUomByUnit(Unit.SECOND);
		fetchUomByUnit(Unit.METRE);
		fetchUomByUnit(Unit.CELSIUS);
	}

	public void close() {
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
	}

	private EntityManagerFactory getEntityManagerFactory() {
		if (emf == null && emfFuture != null) {
			try {
				emf = emfFuture.get(EMF_CREATION_TO_SEC, TimeUnit.SECONDS);
			} catch (Exception e) {
				getLogger()
						.error("Unable to create an EntityManagerFactory after " + EMF_CREATION_TO_SEC + " seconds.");

				if (e.getMessage() != null) {
					getLogger().error(e.getMessage());
				} else {
					e.printStackTrace();
				}
			} finally {
				emfFuture = null;
			}
		}
		return emf;
	}

	// get the EntityManager
	public EntityManager getEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}

	public List<String> fetchPlantEntityNames() {
		final String ENTITY_NAMES = "ENTITY.Names";

		if (namedQueryMap.get(ENTITY_NAMES) == null) {
			createNamedQuery(ENTITY_NAMES, "SELECT ent.name FROM PlantEntity ent");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(ENTITY_NAMES, String.class);
		return query.getResultList();
	}

	public PlantEntity fetchPlantEntityByName(String name) {
		final String ENTITY_BY_NAME = "ENTITY.Names";

		if (namedQueryMap.get(ENTITY_BY_NAME) == null) {
			createNamedQuery(ENTITY_BY_NAME, "SELECT ent FROM PlantEntity ent WHERE ent.name = :name");
		}

		PlantEntity entity = null;
		TypedQuery<PlantEntity> query = getEntityManager().createNamedQuery(ENTITY_BY_NAME, PlantEntity.class);
		query.setParameter("name", name);

		List<PlantEntity> entities = query.getResultList();

		if (entities.size() == 1) {
			entity = entities.get(0);
		}
		return entity;
	}

	public List<EventResolver> fetchEventResolvers() {
		final String RESOLVER_ALL = "RESOLVER.All";

		if (namedQueryMap.get(RESOLVER_ALL) == null) {
			createNamedQuery(RESOLVER_ALL, "SELECT er FROM EventResolver er");
		}

		TypedQuery<EventResolver> query = getEntityManager().createNamedQuery(RESOLVER_ALL, EventResolver.class);
		return query.getResultList();
	}

	public List<String> fetchResolverSourceIds(String equipmentName, DataSourceType sourceType) {
		final String EQUIPMENT_SOURCE_IDS = "EQUIP.SourceIds";

		if (namedQueryMap.get(EQUIPMENT_SOURCE_IDS) == null) {
			createNamedQuery(EQUIPMENT_SOURCE_IDS,
					"SELECT er.sourceId FROM EventResolver er JOIN er.equipment eq JOIN er.dataSource ds WHERE eq.name = :name AND ds.sourceType = :type");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(EQUIPMENT_SOURCE_IDS, String.class);
		query.setParameter("name", equipmentName);
		query.setParameter("type", sourceType);
		return query.getResultList();
	}

	public List<CollectorDataSource> fetchDataSources(DataSourceType sourceType) {
		final String SRC_BY_TYPE = "DS.ByType";

		if (namedQueryMap.get(SRC_BY_TYPE) == null) {
			createNamedQuery(SRC_BY_TYPE, "SELECT source FROM CollectorDataSource source WHERE sourceType = :type");
		}

		TypedQuery<CollectorDataSource> query = getEntityManager().createNamedQuery(SRC_BY_TYPE,
				CollectorDataSource.class);
		query.setParameter("type", sourceType);
		return query.getResultList();
	}

	// remove the PersistentObject from the persistence context
	public void evict(KeyedObject object) {
		if (object == null) {
			return;
		}
		getEntityManager().detach(object);
	}

	// save the Persistent Object to the database
	public KeyedObject save(KeyedObject keyed) throws Exception {
		EntityManager em = getEntityManager();
		EntityTransaction txn = null;

		try {
			txn = em.getTransaction();
			txn.begin();

			// merge this entity into the PU and save
			KeyedObject merged = em.merge(keyed);

			// commit transaction
			txn.commit();

			return merged;
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			em.close();
		}
	}

	// save the Persistent Object to the database
	public List<KeyedObject> save(List<KeyedObject> objects) throws Exception {
		EntityManager em = getEntityManager();
		EntityTransaction txn = null;
		List<KeyedObject> mergedObjects = new ArrayList<>();

		try {
			txn = em.getTransaction();
			txn.begin();

			// merge this entity into the PU and save
			for (KeyedObject object : objects) {
				KeyedObject merged = em.merge(object);
				mergedObjects.add(merged);
			}

			// commit transaction
			txn.commit();

			return mergedObjects;
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			em.close();
		}
	}

	private void checkRotationReferences(Rotation rotation) throws Exception {
		// check for team reference
		List<Team> referencingTeams = fetchTeamCrossReferences(rotation);

		if (!referencingTeams.isEmpty()) {
			String refs = "";

			for (Team team : referencingTeams) {
				if (refs.length() > 0) {
					refs += ", ";
				}
				refs += team.getName();
			}
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.rotation", rotation.getName(), refs));
		}
	}

	private void checkDataSourceReferences(CollectorDataSource source) throws Exception {
		// check for script resolver references
		List<EventResolver> resolvers = fetchResolverCrossReferences(source);

		if (!resolvers.isEmpty()) {
			String refs = "";
			for (EventResolver resolver : resolvers) {
				if (refs.length() > 0) {
					refs += ", ";
				}
				refs += resolver.getSourceId();
			}
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.collector", source.getName(), refs));
		}
	}

	private long fetchRotationSegmentCount(Shift shift) throws Exception {
		final String SEG_SHIFT_XREF = "Seg.Shift.XRef";

		if (namedQueryMap.get(SEG_SHIFT_XREF) == null) {
			createNamedQuery(SEG_SHIFT_XREF, "SELECT COUNT(rs) FROM RotationSegment rs WHERE startingShift = :shift");
		}

		Query query = getEntityManager().createNamedQuery(SEG_SHIFT_XREF);
		query.setParameter("shift", shift);
		return (long) query.getSingleResult();
	}

	private void checkShiftReferences(Shift shift) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(shift);

		if (count > 0) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.event.shift", shift.getName(), count));
		}

		// by rotation segment
		count = fetchRotationSegmentCount(shift);

		if (count > 0) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.rs.shift", shift.getName(), count));
		}
	}

	private void checkTeamReferences(Team team) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(team);

		if (count > 0) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.event.team", team.getName(), count));
		}
	}

	private void checkWorkScheduleReferences(WorkSchedule schedule) throws Exception {
		// check for plant entity references
		List<PlantEntity> entities = fetchEntityCrossReferences(schedule);

		if (!entities.isEmpty()) {
			String refs = "";
			for (PlantEntity entity : entities) {
				if (refs.length() > 0) {
					refs += ", ";
				}
				refs += entity.getName();
			}
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.schedule", schedule.getName(), refs));
		}

		// check for shift references
		for (Shift shift : schedule.getShifts()) {
			checkShiftReferences(shift);
		}

		// check for team references
		for (Team team : schedule.getTeams()) {
			checkTeamReferences(team);
		}
	}

	private void checkUomReferences(UnitOfMeasure uom) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(uom);

		if (count > 0) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.event.uom", uom.getSymbol(), count));
		}

		// check for usage by equipment material
		List<EquipmentMaterial> eqms = fetchEquipmentMaterials(uom);

		if (!eqms.isEmpty()) {
			String refs = "";
			for (int i = 0; i < eqms.size(); i++) {
				if (i > 0) {
					refs += ", ";
				}

				refs += eqms.get(i).getEquipment().getName();
			}
			throw new Exception(DomainLocalizer.instance().getErrorString("can.not.delete.uom", uom.getSymbol(), refs));
		}

		// check for usage by UOM
		List<UnitOfMeasure> uoms = PersistenceService.instance().fetchUomCrossReferences(uom);

		if (!uoms.isEmpty()) {
			String refs = "";
			for (int i = 0; i < uoms.size(); i++) {
				if (!uom.equals(uoms.get(i))) {
					if (i > 0) {
						refs += ", ";
					}

					refs += uoms.get(i).getSymbol();
				}
			}

			if (refs.length() > 0) {
				throw new Exception(
						DomainLocalizer.instance().getErrorString("can.not.delete.ref.uom", uom.getSymbol(), refs));
			}
		}
	}

	private void checkMaterialReferences(Material material) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(material);

		if (count > 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("can.not.delete.event.material",
					material.getName(), count));
		}

		// check for usage by equipment material
		List<EquipmentMaterial> eqms = fetchEquipmentMaterials(material);

		if (!eqms.isEmpty()) {
			String refs = "";
			for (int i = 0; i < eqms.size(); i++) {
				if (i > 0) {
					refs += ", ";
				}

				refs += eqms.get(i).getEquipment().getName();
			}
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.material", material.getName(), refs));
		}
	}

	private void checkEquipmentReferences(Equipment equipment) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(equipment);

		if (count > 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("can.not.delete.event.equip",
					equipment.getName(), count));
		}
	}

	private void checkReasonReferences(Reason reason) throws Exception {
		// check for usage by OEE event
		long count = fetchEventCount(reason);

		if (count > 0) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.event.reason", reason.getName(), count));
		}
	}

	private void checkDataCollectorReferences(DataCollector collector) throws Exception {
		// check for script resolver references
		List<EventResolver> resolvers = fetchResolverCrossReferences(collector);

		if (!resolvers.isEmpty()) {
			String refs = "";
			for (EventResolver resolver : resolvers) {
				if (refs.length() > 0) {
					refs += ", ";
				}
				refs += resolver.getSourceId();
			}
			throw new Exception(
					DomainLocalizer.instance().getErrorString("can.not.delete.collector", collector.getName(), refs));
		}
	}

	public void checkReferences(KeyedObject keyed) throws Exception {
		if (keyed instanceof Rotation) {
			checkRotationReferences((Rotation) keyed);
		} else if (keyed instanceof CollectorDataSource) {
			checkDataSourceReferences((CollectorDataSource) keyed);
		} else if (keyed instanceof WorkSchedule) {
			checkWorkScheduleReferences((WorkSchedule) keyed);
		} else if (keyed instanceof UnitOfMeasure) {
			checkUomReferences((UnitOfMeasure) keyed);
		} else if (keyed instanceof Material) {
			checkMaterialReferences((Material) keyed);
		} else if (keyed instanceof Equipment) {
			checkEquipmentReferences((Equipment) keyed);
		} else if (keyed instanceof DataCollector) {
			checkDataCollectorReferences((DataCollector) keyed);
		} else if (keyed instanceof Reason) {
			checkReasonReferences((Reason) keyed);
		} else if (keyed instanceof Shift) {
			checkShiftReferences((Shift) keyed);
		} else if (keyed instanceof Team) {
			checkTeamReferences((Team) keyed);
		}
	}

	// delete the PersistentObjectfrom the database
	public void delete(KeyedObject keyed) throws Exception {
		checkReferences(keyed);

		EntityManager em = getEntityManager();
		EntityTransaction txn = null;

		try {
			// start transaction
			txn = em.getTransaction();
			txn.begin();

			// delete
			Object po = em.find(keyed.getClass(), keyed.getKey());
			em.remove(po);

			// commit transaction
			txn.commit();
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			em.close();
		}
	}

	// all entities
	public List<PlantEntity> fetchAllPlantEntities() {
		final String ENTITY_ALL = "ENTITY.All";

		if (namedQueryMap.get(ENTITY_ALL) == null) {
			createNamedQuery(ENTITY_ALL, "SELECT ent FROM PlantEntity ent");
		}

		TypedQuery<PlantEntity> query = getEntityManager().createNamedQuery(ENTITY_ALL, PlantEntity.class);
		return query.getResultList();
	}

	private void createNamedQuery(String name, String jsql) {
		Query query = getEntityManager().createQuery(jsql);
		getEntityManagerFactory().addNamedQuery(name, query);
		namedQueryMap.put(name, true);
	}

	// top-level plant entities
	public List<PlantEntity> fetchTopPlantEntities() {
		final String ENTITY_ROOTS = "ENTITY.Roots";

		if (namedQueryMap.get(ENTITY_ROOTS) == null) {
			createNamedQuery(ENTITY_ROOTS, "SELECT ent FROM PlantEntity ent WHERE ent.parent IS NULL");
		}

		TypedQuery<PlantEntity> query = getEntityManager().createNamedQuery(ENTITY_ROOTS, PlantEntity.class);
		return query.getResultList();
	}

	public List<DataCollector> fetchCollectorsByHostAndState(List<String> hostNames, List<CollectorState> states) {
		final String COLLECTOR_BY_HOST_BY_STATE = "COLLECT.ByStateByHost";

		if (namedQueryMap.get(COLLECTOR_BY_HOST_BY_STATE) == null) {
			createNamedQuery(COLLECTOR_BY_HOST_BY_STATE,
					"SELECT collector FROM DataCollector collector WHERE collector.host IN :names AND collector.state IN :states");
		}

		TypedQuery<DataCollector> query = getEntityManager().createNamedQuery(COLLECTOR_BY_HOST_BY_STATE,
				DataCollector.class);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<DataCollector> fetchCollectorsByState(List<CollectorState> states) {
		final String COLLECTOR_BY_STATE = "COLLECT.ByState";

		if (namedQueryMap.get(COLLECTOR_BY_STATE) == null) {
			createNamedQuery(COLLECTOR_BY_STATE,
					"SELECT collector FROM DataCollector collector WHERE collector.state IN :states");
		}

		TypedQuery<DataCollector> query = getEntityManager().createNamedQuery(COLLECTOR_BY_STATE, DataCollector.class);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<EventResolver> fetchEventResolversByHost(List<String> hostNames, List<CollectorState> states)
			throws Exception {
		final String RESOLVER_BY_HOST = "RESOLVER.ByHost";

		if (namedQueryMap.get(RESOLVER_BY_HOST) == null) {
			createNamedQuery(RESOLVER_BY_HOST,
					"SELECT er FROM EventResolver er WHERE er.collector.host IN :names AND er.collector.state IN :states");
		}

		TypedQuery<EventResolver> query = getEntityManager().createNamedQuery(RESOLVER_BY_HOST, EventResolver.class);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<EventResolver> fetchEventResolversByCollector(List<String> definitionNames) throws Exception {
		final String RESOLVER_BY_COLLECTOR = "RESOLVER.ByCollector";

		if (namedQueryMap.get(RESOLVER_BY_COLLECTOR) == null) {
			createNamedQuery(RESOLVER_BY_COLLECTOR,
					"SELECT er FROM EventResolver er WHERE er.collector.name IN :names");
		}

		TypedQuery<EventResolver> query = getEntityManager().createNamedQuery(RESOLVER_BY_COLLECTOR,
				EventResolver.class);
		query.setParameter("names", definitionNames);
		return query.getResultList();
	}

	public List<Material> fetchMaterialsByCategory(String category) throws Exception {
		final String MATLS_BY_CATEGORY = "MATL.ByCategory";

		if (namedQueryMap.get(MATLS_BY_CATEGORY) == null) {
			createNamedQuery(MATLS_BY_CATEGORY, "SELECT matl FROM Material matl WHERE matl.category = :category");
		}

		TypedQuery<Material> query = getEntityManager().createNamedQuery(MATLS_BY_CATEGORY, Material.class);
		query.setParameter("category", category);
		return query.getResultList();
	}

	public List<DataCollector> fetchAllDataCollectors() {
		final String COLLECT_ALL = "COLLECT.All";

		if (namedQueryMap.get(COLLECT_ALL) == null) {
			createNamedQuery(COLLECT_ALL, "SELECT collector FROM DataCollector collector");
		}

		TypedQuery<DataCollector> query = getEntityManager().createNamedQuery(COLLECT_ALL, DataCollector.class);
		return query.getResultList();
	}

	public List<Material> fetchAllMaterials() {
		final String MATL_ALL = "MATL.All";

		if (namedQueryMap.get(MATL_ALL) == null) {
			createNamedQuery(MATL_ALL, "SELECT matl FROM Material matl");
		}

		TypedQuery<Material> query = getEntityManager().createNamedQuery(MATL_ALL, Material.class);
		return query.getResultList();
	}

	public List<String> fetchMaterialCategories() {
		final String MATL_CATEGORIES = "MATL.Categories";

		if (namedQueryMap.get(MATL_CATEGORIES) == null) {
			createNamedQuery(MATL_CATEGORIES,
					"SELECT DISTINCT matl.category FROM Material matl WHERE matl.category IS NOT NULL");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(MATL_CATEGORIES, String.class);
		return query.getResultList();
	}

	public Material fetchMaterialByName(String name) {
		final String MATL_BY_NAME = "MATL.ByName";

		if (namedQueryMap.get(MATL_BY_NAME) == null) {
			createNamedQuery(MATL_BY_NAME, "SELECT matl FROM Material matl WHERE matl.name = :name");
		}

		Material material = null;
		TypedQuery<Material> query = getEntityManager().createNamedQuery(MATL_BY_NAME, Material.class);
		query.setParameter("name", name);
		List<Material> materials = query.getResultList();

		if (materials.size() == 1) {
			material = materials.get(0);
		}
		return material;
	}

	public Material fetchMaterialByKey(Long key) throws Exception {
		return getEntityManager().find(Material.class, key);
	}

	public OeeEvent fetchEventByKey(Long key) throws Exception {
		return getEntityManager().find(OeeEvent.class, key);
	}

	public Reason fetchReasonByName(String name) {
		final String REASON_BY_NAME = "REASON.ByName";

		if (namedQueryMap.get(REASON_BY_NAME) == null) {
			createNamedQuery(REASON_BY_NAME, "SELECT reason FROM Reason reason WHERE reason.name = :name");
		}

		Reason reason = null;
		TypedQuery<Reason> query = getEntityManager().createNamedQuery(REASON_BY_NAME, Reason.class);
		query.setParameter("name", name);

		List<Reason> reasons = query.getResultList();

		if (reasons.size() == 1) {
			reason = reasons.get(0);
		}
		return reason;
	}

	public Reason fetchReasonByKey(Long key) throws Exception {
		return getEntityManager().find(Reason.class, key);
	}

	public List<Reason> fetchAllReasons() {
		final String REASON_ALL = "REASON.All";

		if (namedQueryMap.get(REASON_ALL) == null) {
			createNamedQuery(REASON_ALL, "SELECT reason FROM Reason reason");
		}

		TypedQuery<Reason> query = getEntityManager().createNamedQuery(REASON_ALL, Reason.class);
		return query.getResultList();
	}

	// top-level reasons
	public List<Reason> fetchTopReasons() {
		final String REASON_ROOTS = "REASON.Roots";

		if (namedQueryMap.get(REASON_ROOTS) == null) {
			createNamedQuery(REASON_ROOTS, "SELECT reason FROM Reason reason WHERE reason.parent IS NULL");
		}

		TypedQuery<Reason> query = getEntityManager().createNamedQuery(REASON_ROOTS, Reason.class);
		return query.getResultList();
	}

	public List<String> fetchProgIds() {
		final String DA_PROG_IDS = "OPCDA.ProgIds";

		if (namedQueryMap.get(DA_PROG_IDS) == null) {
			createNamedQuery(DA_PROG_IDS, "SELECT source.name FROM OpcDaSource source");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(DA_PROG_IDS, String.class);
		return query.getResultList();
	}

	public OpcDaSource fetchOpcDaSourceByName(String name) {
		final String DA_SRC_BY_NAME = "OPCDA.ByName";

		if (namedQueryMap.get(DA_SRC_BY_NAME) == null) {
			createNamedQuery(DA_SRC_BY_NAME, "SELECT source FROM OpcDaSource source WHERE source.name = :name");
		}

		OpcDaSource source = null;
		TypedQuery<OpcDaSource> query = getEntityManager().createNamedQuery(DA_SRC_BY_NAME, OpcDaSource.class);
		query.setParameter("name", name);

		List<OpcDaSource> sources = query.getResultList();

		if (sources.size() == 1) {
			source = sources.get(0);
		}
		return source;
	}

	public OpcUaSource fetchOpcUaSourceByName(String name) {
		final String UA_SRC_BY_NAME = "OPCUA.ByName";

		if (namedQueryMap.get(UA_SRC_BY_NAME) == null) {
			createNamedQuery(UA_SRC_BY_NAME, "SELECT source FROM OpcUaSource source WHERE source.name = :name");
		}

		OpcUaSource source = null;
		TypedQuery<OpcUaSource> query = getEntityManager().createNamedQuery(UA_SRC_BY_NAME, OpcUaSource.class);
		query.setParameter("name", name);

		List<OpcUaSource> sources = query.getResultList();

		if (sources.size() == 1) {
			source = sources.get(0);
		}
		return source;
	}

	public WorkSchedule fetchScheduleByKey(Long key) throws Exception {
		return getEntityManager().find(WorkSchedule.class, key);
	}

	public List<WorkSchedule> fetchWorkSchedules() {
		final String WS_SCHEDULES = "WS.Schedules";

		if (namedQueryMap.get(WS_SCHEDULES) == null) {
			createNamedQuery(WS_SCHEDULES, "SELECT ws FROM WorkSchedule ws");
		}

		TypedQuery<WorkSchedule> query = getEntityManager().createNamedQuery(WS_SCHEDULES, WorkSchedule.class);
		return query.getResultList();
	}

	public List<String> fetchWorkScheduleNames() {
		final String WS_NAMES = "WS.Names";

		if (namedQueryMap.get(WS_NAMES) == null) {
			createNamedQuery(WS_NAMES, "SELECT ws.name FROM WorkSchedule ws");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(WS_NAMES, String.class);
		return query.getResultList();
	}

	public WorkSchedule fetchWorkScheduleByName(String name) {
		final String WS_BY_NAME = "WS.ByName";

		if (namedQueryMap.get(WS_BY_NAME) == null) {
			createNamedQuery(WS_BY_NAME, "SELECT ws FROM WorkSchedule ws WHERE ws.name = :name");
		}

		WorkSchedule schedule = null;
		TypedQuery<WorkSchedule> query = getEntityManager().createNamedQuery(WS_BY_NAME, WorkSchedule.class);
		query.setParameter("name", name);
		List<WorkSchedule> schedules = query.getResultList();

		if (schedules.size() == 1) {
			schedule = schedules.get(0);
		}
		return schedule;
	}

	// fetch Team by its primary key
	public Team fetchTeamByKey(Long key) throws Exception {
		return getEntityManager().find(Team.class, key);
	}

	// get any Team references to the Rotation
	public List<Team> fetchTeamCrossReferences(Rotation rotation) throws Exception {
		final String WS_ROT_XREF = "WS.ROT.CrossRef";

		if (namedQueryMap.get(WS_ROT_XREF) == null) {
			createNamedQuery(WS_ROT_XREF, "SELECT team FROM Team team WHERE rotation = :rotation");
		}

		TypedQuery<Team> query = getEntityManager().createNamedQuery(WS_ROT_XREF, Team.class);
		query.setParameter("rotation", rotation);
		return query.getResultList();
	}

	public List<PlantEntity> fetchEntityCrossReferences(WorkSchedule schedule) {
		final String WS_ENT_XREF = "WS.ENT.CrossRef";

		if (namedQueryMap.get(WS_ENT_XREF) == null) {
			createNamedQuery(WS_ENT_XREF, "SELECT ent FROM PlantEntity ent WHERE ent.workSchedule = :schedule");
		}

		TypedQuery<PlantEntity> query = getEntityManager().createNamedQuery(WS_ENT_XREF, PlantEntity.class);
		query.setParameter("schedule", schedule);
		return query.getResultList();
	}

	public UnitOfMeasure fetchUomByKey(Long key) throws Exception {
		UnitOfMeasure uom = getEntityManager().find(UnitOfMeasure.class, key);

		// cache it
		if (uom != null) {
			MeasurementSystem.instance().registerUnit(uom);
		}

		return uom;
	}

	// get symbols and names in this category
	@SuppressWarnings("unchecked")
	public List<String[]> fetchUomSymbolsAndNamesByCategory(String category) {
		final String UOM_CAT_SYMBOLS = "UOM.SymbolsInCategory";

		if (namedQueryMap.get(UOM_CAT_SYMBOLS) == null) {
			createNamedQuery(UOM_CAT_SYMBOLS,
					"SELECT uom.symbol, uom.name FROM UnitOfMeasure uom WHERE uom.category = :category");
		}

		Query query = getEntityManager().createNamedQuery(UOM_CAT_SYMBOLS);
		query.setParameter("category", category);
		return (List<String[]>) query.getResultList();
	}

	// fetch symbols and their names for this UOM type
	@SuppressWarnings("unchecked")
	public List<String[]> fetchUomSymbolsAndNamesByType(UnitType unitType) {
		final String UOM_SYMBOLS = "UOM.Symbols";

		if (namedQueryMap.get(UOM_SYMBOLS) == null) {
			createNamedQuery(UOM_SYMBOLS,
					"SELECT uom.symbol, uom.name FROM UnitOfMeasure uom WHERE uom.unit IS NULL AND uom.unitType = :type");
		}

		Query query = getEntityManager().createNamedQuery(UOM_SYMBOLS);
		query.setParameter("type", unitType);

		return (List<String[]>) query.getResultList();
	}

	// fetch all defined categories
	public List<String> fetchUomCategories() {
		final String UOM_CATEGORIES = "UOM.Categories";

		if (namedQueryMap.get(UOM_CATEGORIES) == null) {
			createNamedQuery(UOM_CATEGORIES,
					"SELECT DISTINCT uom.category FROM UnitOfMeasure uom WHERE uom.category IS NOT NULL");
		}

		TypedQuery<String> query = getEntityManager().createNamedQuery(UOM_CATEGORIES, String.class);
		return query.getResultList();
	}

	// query for UOM based on its unique symbol
	public UnitOfMeasure fetchUomBySymbol(String symbol) throws Exception {
		final String UOM_BY_SYMBOL = "UOM.BySymbol";

		if (namedQueryMap.get(UOM_BY_SYMBOL) == null) {
			createNamedQuery(UOM_BY_SYMBOL, "SELECT uom FROM UnitOfMeasure uom WHERE uom.symbol = :symbol");
		}

		TypedQuery<UnitOfMeasure> query = getEntityManager().createNamedQuery(UOM_BY_SYMBOL, UnitOfMeasure.class);
		query.setParameter("symbol", symbol);

		List<UnitOfMeasure> uoms = query.getResultList();

		UnitOfMeasure uom = null;

		if (uoms.size() == 1) {
			uom = uoms.get(0);

			// also cache it
			MeasurementSystem.instance().registerUnit(uom);
		}
		return uom;
	}

	public List<UnitOfMeasure> fetchUomsByCategory(String category) throws Exception {
		final String UOM_BY_CATEGORY = "UOM.ByCategory";

		if (namedQueryMap.get(UOM_BY_CATEGORY) == null) {
			createNamedQuery(UOM_BY_CATEGORY, "SELECT uom FROM UnitOfMeasure uom WHERE uom.category = :category");
		}

		TypedQuery<UnitOfMeasure> query = getEntityManager().createNamedQuery(UOM_BY_CATEGORY, UnitOfMeasure.class);
		query.setParameter("category", category);
		List<UnitOfMeasure> uoms = query.getResultList();

		// cache them
		for (UnitOfMeasure uom : uoms) {
			MeasurementSystem.instance().registerUnit(uom);
		}

		return uoms;
	}

	// fetch UOM by its enumeration
	public UnitOfMeasure fetchUomByUnit(Unit unit) throws Exception {
		final String UOM_BY_UNIT = "UOM.ByUnit";

		if (namedQueryMap.get(UOM_BY_UNIT) == null) {
			createNamedQuery(UOM_BY_UNIT, "SELECT uom FROM UnitOfMeasure uom WHERE uom.unit = :unit");
		}

		UnitOfMeasure uom = null;

		// fetch by Unit enum
		TypedQuery<UnitOfMeasure> query = getEntityManager().createNamedQuery(UOM_BY_UNIT, UnitOfMeasure.class);
		query.setParameter("unit", unit);

		List<UnitOfMeasure> uoms = query.getResultList();

		if (uoms.size() == 1) {
			uom = uoms.get(0);

			// also cache it
			MeasurementSystem.instance().registerUnit(uom);
		} else {
			// not in db, get from pre-defined units
			uom = MeasurementSystem.instance().getUOM(unit);
		}

		// fetch units that it references
		fetchReferencedUnits(uom);

		return uom;
	}

	// fetch recursively all referenced units to make them managed in the
	// persistence unit
	public void fetchReferencedUnits(UnitOfMeasure uom) throws Exception {
		String id = null;
		UnitOfMeasure referenced = null;
		UnitOfMeasure fetched = null;

		// abscissa unit
		referenced = uom.getAbscissaUnit();
		if (referenced != null && !uom.isTerminal()) {
			id = referenced.getSymbol();
			fetched = fetchUomBySymbol(id);

			if (fetched != null) {
				// already in database
				uom.setAbscissaUnit(fetched);
			}

			// units referenced by the abscissa
			fetchReferencedUnits(referenced);
		}

		// bridge abscissa unit
		referenced = uom.getBridgeAbscissaUnit();
		if (referenced != null) {
			id = referenced.getSymbol();
			fetched = fetchUomBySymbol(id);

			if (fetched != null) {
				// already in database
				uom.setBridgeConversion(uom.getBridgeScalingFactor(), fetched, uom.getBridgeOffset());
			}
		}

		// UOM1 and UOM2
		if (uom.getMeasurementType().equals(MeasurementType.PRODUCT)) {
			// multiplier
			UnitOfMeasure uom1 = uom.getMultiplier();
			id = uom1.getSymbol();
			fetched = fetchUomBySymbol(id);

			if (fetched != null) {
				uom1 = fetched;
			}

			// multiplicand
			UnitOfMeasure uom2 = uom.getMultiplicand();
			id = uom2.getSymbol();
			UnitOfMeasure fetched2 = fetchUomBySymbol(id);

			if (fetched2 != null) {
				uom2 = fetched2;
			}

			uom.setProductUnits(uom1, uom2);

			// units referenced by UOM1 & 2
			fetchReferencedUnits(uom1);
			fetchReferencedUnits(uom2);

		} else if (uom.getMeasurementType().equals(MeasurementType.QUOTIENT)) {
			// dividend
			UnitOfMeasure uom1 = uom.getDividend();
			id = uom1.getSymbol();
			fetched = fetchUomBySymbol(id);

			if (fetched != null) {
				uom1 = fetched;
			}

			// divisor
			UnitOfMeasure uom2 = uom.getDivisor();
			id = uom2.getSymbol();
			UnitOfMeasure fetched2 = fetchUomBySymbol(id);

			if (fetched2 != null) {
				uom2 = fetched2;
			}

			uom.setQuotientUnits(uom1, uom2);

			// units referenced by UOM1 & 2
			fetchReferencedUnits(uom1);
			fetchReferencedUnits(uom2);

		} else if (uom.getMeasurementType().equals(MeasurementType.POWER)) {
			referenced = uom.getPowerBase();
			id = referenced.getSymbol();
			fetched = fetchUomBySymbol(id);

			if (fetched != null) {
				// already in database
				uom.setPowerUnit(fetched, uom.getPowerExponent());
			}

			// units referenced by the power base
			fetchReferencedUnits(referenced);
		}
	}

	public List<EquipmentMaterial> fetchEquipmentMaterials(UnitOfMeasure uom) throws Exception {
		final String EQM_UOM_XREF = "EQM.UOM.XRef";

		if (namedQueryMap.get(EQM_UOM_XREF) == null) {
			createNamedQuery(EQM_UOM_XREF,
					"SELECT eqm FROM EquipmentMaterial eqm WHERE runRateUOM = :uom OR rejectUOM = :uom");
		}

		TypedQuery<EquipmentMaterial> query = getEntityManager().createNamedQuery(EQM_UOM_XREF,
				EquipmentMaterial.class);
		query.setParameter("uom", uom);
		return query.getResultList();
	}

	public List<EquipmentMaterial> fetchEquipmentMaterials(Material material) throws Exception {
		final String EQM_MAT_XREF = "EQM.Mat.XRef";

		if (namedQueryMap.get(EQM_MAT_XREF) == null) {
			createNamedQuery(EQM_MAT_XREF, "SELECT eqm FROM EquipmentMaterial eqm WHERE material = :material");
		}

		TypedQuery<EquipmentMaterial> query = getEntityManager().createNamedQuery(EQM_MAT_XREF,
				EquipmentMaterial.class);
		query.setParameter("material", material);
		return query.getResultList();
	}

	public long fetchEventCount(Material material) throws Exception {
		final String EVENT_MAT_XREF = "Event.Mat.XRef";

		if (namedQueryMap.get(EVENT_MAT_XREF) == null) {
			createNamedQuery(EVENT_MAT_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE material = :material");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_MAT_XREF);
		query.setParameter("material", material);
		return (long) query.getSingleResult();
	}

	public long fetchEventCount(Equipment equipment) throws Exception {
		final String EVENT_EQ_XREF = "Event.Equip.XRef";

		if (namedQueryMap.get(EVENT_EQ_XREF) == null) {
			createNamedQuery(EVENT_EQ_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE equipment = :equipment");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_EQ_XREF);
		query.setParameter("equipment", equipment);
		return (long) query.getSingleResult();
	}

	public long fetchEventCount(UnitOfMeasure uom) throws Exception {
		final String EVENT_UOM_XREF = "Event.Uom.XRef";

		if (namedQueryMap.get(EVENT_UOM_XREF) == null) {
			createNamedQuery(EVENT_UOM_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE uom = :uom");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_UOM_XREF);
		query.setParameter("uom", uom);
		return (long) query.getSingleResult();
	}

	public long fetchEventCount(Reason reason) throws Exception {
		final String EVENT_REASON_XREF = "Event.Reason.XRef";

		if (namedQueryMap.get(EVENT_REASON_XREF) == null) {
			createNamedQuery(EVENT_REASON_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE reason = :reason");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_REASON_XREF);
		query.setParameter("reason", reason);
		return (long) query.getSingleResult();
	}

	public long fetchEventCount(Shift shift) throws Exception {
		final String EVENT_SHIFT_XREF = "Event.Shift.XRef";

		if (namedQueryMap.get(EVENT_SHIFT_XREF) == null) {
			createNamedQuery(EVENT_SHIFT_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE shift = :shift");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_SHIFT_XREF);
		query.setParameter("shift", shift);
		return (long) query.getSingleResult();
	}

	public long fetchEventCount(Team team) throws Exception {
		final String EVENT_TEAM_XREF = "Event.Team.XRef";

		if (namedQueryMap.get(EVENT_TEAM_XREF) == null) {
			createNamedQuery(EVENT_TEAM_XREF, "SELECT COUNT(event) FROM OeeEvent event WHERE team = :team");
		}

		Query query = getEntityManager().createNamedQuery(EVENT_TEAM_XREF);
		query.setParameter("team", team);
		return (long) query.getSingleResult();
	}

	public List<UnitOfMeasure> fetchUomCrossReferences(UnitOfMeasure uom) throws Exception {
		final String UOM_XREF = "UOM.CrossRef";

		if (namedQueryMap.get(UOM_XREF) == null) {
			createNamedQuery(UOM_XREF,
					"SELECT uom FROM UnitOfMeasure uom WHERE uom1 = :uom OR uom2 = :uom OR abscissaUnit = :uom OR bridgeAbscissaUnit = :uom");
		}

		TypedQuery<UnitOfMeasure> query = getEntityManager().createNamedQuery(UOM_XREF, UnitOfMeasure.class);
		query.setParameter("uom", uom);
		return query.getResultList();
	}

	public DataCollector fetchCollectorByName(String name) {
		final String COLLECT_BY_NAME = "COLLECT.ByName";

		if (namedQueryMap.get(COLLECT_BY_NAME) == null) {
			createNamedQuery(COLLECT_BY_NAME,
					"SELECT collector FROM DataCollector collector WHERE collector.name = :name");
		}

		DataCollector collector = null;
		TypedQuery<DataCollector> query = getEntityManager().createNamedQuery(COLLECT_BY_NAME, DataCollector.class);
		query.setParameter("name", name);

		List<DataCollector> collectors = query.getResultList();

		if (collectors.size() == 1) {
			collector = collectors.get(0);
		}
		return collector;
	}

	public List<EventResolver> fetchResolverCrossReferences(CollectorDataSource source) {
		final String COLLECT_RES_XREF = "COLLECT.Resolver.CrossRef";

		if (namedQueryMap.get(COLLECT_RES_XREF) == null) {
			createNamedQuery(COLLECT_RES_XREF,
					"SELECT resolver FROM EventResolver resolver WHERE resolver.dataSource = :source");
		}

		TypedQuery<EventResolver> query = getEntityManager().createNamedQuery(COLLECT_RES_XREF, EventResolver.class);
		query.setParameter("source", source);
		return query.getResultList();
	}

	public List<EventResolver> fetchResolverCrossReferences(DataCollector collector) {
		final String COLLECT_RES_XREF = "Collector.Resolver.CrossRef";

		if (namedQueryMap.get(COLLECT_RES_XREF) == null) {
			createNamedQuery(COLLECT_RES_XREF,
					"SELECT resolver FROM EventResolver resolver WHERE resolver.collector = :collector");
		}

		TypedQuery<EventResolver> query = getEntityManager().createNamedQuery(COLLECT_RES_XREF, EventResolver.class);
		query.setParameter("collector", collector);
		return query.getResultList();
	}

	private void createContainerManagedEntityManagerFactory(String jdbcUrl, String userName, String password)
			throws Exception {

		// create the PU info
		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(PU_NAME, getEntityClassNames(),
				createProperties(jdbcUrl, userName, password));

		// add any mapping files
		String[] fileNames = getMappingFileNames();
		if (fileNames != null) {
			persistenceUnitInfo.getMappingFileNames().addAll(Arrays.asList(fileNames));
		}

		// PU configuration map
		Map<String, Object> configuration = new HashMap<>();

		Integrator integrator = getIntegrator();
		if (integrator != null) {
			configuration.put("hibernate.integrator_provider",
					(IntegratorProvider) () -> Collections.singletonList(integrator));
		}

		// create the EntityManagerFactory
		emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(persistenceUnitInfo,
				configuration);
	}

	public void connectToDatabaseEventServer(String jdbcUrl, String userName, String password) throws Exception {
		// create the PU info
		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(DB_PU_NAME,
				getDatabaseEventEntityClassNames(), createProperties(jdbcUrl, userName, password));

		// add any mapping files
		String[] fileNames = getMappingFileNames();
		if (fileNames != null) {
			persistenceUnitInfo.getMappingFileNames().addAll(Arrays.asList(fileNames));
		}

		// PU configuration map
		Map<String, Object> configuration = new HashMap<>();

		Integrator integrator = getIntegrator();
		if (integrator != null) {
			configuration.put("hibernate.integrator_provider",
					(IntegratorProvider) () -> Collections.singletonList(integrator));
		}

		// create the EntityManagerFactory
		emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(persistenceUnitInfo,
				configuration);
	}

	public boolean isConnected() {
		return emf != null ? true : false;
	}

	private String[] getMappingFileNames() {
		// placeholder for mapping files
		return null;
	}

	private Class<?>[] getEntityClasses() {
		return new Class<?>[] { DataCollector.class, CollectorDataSource.class, OeeEvent.class, HttpSource.class,
				RmqSource.class, JmsSource.class, MqttSource.class, DatabaseEventSource.class, FileEventSource.class,
				OpcDaSource.class, OpcUaSource.class, Area.class, Enterprise.class, Equipment.class,
				EquipmentMaterial.class, Material.class, PlantEntity.class, ProductionLine.class, Reason.class,
				Site.class, WorkCell.class, EventResolver.class, UnitOfMeasure.class, ExceptionPeriod.class,
				Rotation.class, RotationSegment.class, Shift.class, Team.class, WorkSchedule.class, ModbusSource.class,
				EntitySchedule.class };
	}

	private Class<?>[] getDatabaseEventEntityClasses() {
		return new Class<?>[] { DatabaseEvent.class };
	}

	private List<String> getDatabaseEventEntityClassNames() {
		return Arrays.asList(getDatabaseEventEntityClasses()).stream().map(Class::getName).collect(Collectors.toList());
	}

	private List<String> getEntityClassNames() {
		return Arrays.asList(getEntityClasses()).stream().map(Class::getName).collect(Collectors.toList());
	}

	private Properties createProperties(String jdbcUrl, String userName, String password) throws Exception {
		DatabaseType databaseType = null;

		if (jdbcUrl.contains("sqlserver")) {
			databaseType = DatabaseType.MSSQL;
		} else if (jdbcUrl.contains("oracle")) {
			databaseType = DatabaseType.ORACLE;
		} else if (jdbcUrl.contains("hsqldb")) {
			databaseType = DatabaseType.HSQL;
		} else if (jdbcUrl.contains("mysql")) {
			databaseType = DatabaseType.MYSQL;
		} else if (jdbcUrl.contains("postgresql")) {
			databaseType = DatabaseType.POSTGRES;
		} else {
			throw new Exception(DomainLocalizer.instance().getErrorString("bad.jdbc", jdbcUrl));
		}

		Properties properties = new Properties();

		if (databaseType.equals(DatabaseType.MSSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");
			properties.put("javax.persistence.jdbc.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} else if (databaseType.equals(DatabaseType.ORACLE)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle12cDialect");
			properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.driver.OracleDriver");
		} else if (databaseType.equals(DatabaseType.HSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			properties.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbc.JDBCDriver");
		} else if (databaseType.equals(DatabaseType.MYSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
			properties.put("javax.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
		} else if (databaseType.equals(DatabaseType.POSTGRES)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
			properties.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
		}

		// jdbc connection
		properties.put("javax.persistence.jdbc.url", jdbcUrl);
		properties.put("javax.persistence.jdbc.user", userName);

		if (password != null) {
			properties.put("javax.persistence.jdbc.password", password);
		}

		// lazy loading without a transaction
		properties.put("hibernate.enable_lazy_load_no_trans", "true");

		// multiple representations of the same entity are being merged
		properties.put("hibernate.event.merge.entity_copy_observer", "allow");

		// Hikari connection pool
		properties.put("hibernate.hikari.minimumIdle", "1");
		properties.put("hibernate.hikari.maximumPoolSize", "20");
		properties.put("hibernate.hikari.idleTimeout", "60000");
		properties.put("hibernate.connection.provider_class",
				"org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

		return properties;
	}

	private Integrator getIntegrator() {
		return null;
	}

	public List<OeeEvent> fetchAvailability(Equipment equipment, OffsetDateTime from, OffsetDateTime to) {
		final String AVAIL_RECORDS = "Availability.FromTo";

		if (namedQueryMap.get(AVAIL_RECORDS) == null) {
			createNamedQuery(AVAIL_RECORDS,
					"SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type "
							+ "AND (e.startTime.localDateTime >= :from AND e.startTime.localDateTime < :to) ORDER BY e.startTime.localDateTime ASC");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(AVAIL_RECORDS, OeeEvent.class);
		query.setParameter("type", OeeEventType.AVAILABILITY);
		query.setParameter("equipment", equipment);
		query.setParameter("from", from.toLocalDateTime());
		query.setParameter("to", to.toLocalDateTime());

		return query.getResultList();
	}

	public List<OeeEvent> fetchProduction(Equipment equipment, Material material, OffsetDateTime from,
			OffsetDateTime to) {
		final String PROD_RECORDS = "Production.FromTo";

		if (namedQueryMap.get(PROD_RECORDS) == null) {
			createNamedQuery(PROD_RECORDS, "SELECT e FROM OeeEvent e WHERE e.equipment = :equipment "
					+ "AND e.eventType IN :types AND (e.startTime.localDateTime >= :from AND e.startTime.localDateTime < :to) AND e.material = :material ORDER BY e.startTime.localDateTime ASC");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(PROD_RECORDS, OeeEvent.class);

		query.setParameter("types", OeeEventType.getProductionTypes());
		query.setParameter("equipment", equipment);
		query.setParameter("material", material);
		query.setParameter("from", from.toLocalDateTime());
		query.setParameter("to", to.toLocalDateTime());

		return query.getResultList();
	}

	public List<OeeEvent> fetchSetupsForPeriod(Equipment equipment, OffsetDateTime from, OffsetDateTime to) {
		final String SETUP_PERIOD = "Setup.Period";

		if (namedQueryMap.get(SETUP_PERIOD) == null) {
			createNamedQuery(SETUP_PERIOD,
					"SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type "
							+ "AND e.startTime.localDateTime  <= :to AND (e.endTime.localDateTime  >= :from OR e.endTime.localDateTime IS NULL)");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(SETUP_PERIOD, OeeEvent.class);
		query.setParameter("type", OeeEventType.MATL_CHANGE);
		query.setParameter("equipment", equipment);
		query.setParameter("from", from.toLocalDateTime());
		query.setParameter("to", to.toLocalDateTime());

		return query.getResultList();
	}

	public List<OeeEvent> fetchSetupsForPeriodAndMaterial(Equipment equipment, OffsetDateTime from, OffsetDateTime to,
			Material material) {
		final String SETUP_PERIOD_MATL = "Setup.Period.Material";

		if (namedQueryMap.get(SETUP_PERIOD_MATL) == null) {
			createNamedQuery(SETUP_PERIOD_MATL,
					"SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type "
							+ "AND e.startTime.localDateTime  <= :to AND (e.endTime.localDateTime  >= :from OR e.endTime.localDateTime IS NULL) AND e.material = :matl");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(SETUP_PERIOD_MATL, OeeEvent.class);
		query.setParameter("type", OeeEventType.MATL_CHANGE);
		query.setParameter("equipment", equipment);
		query.setParameter("from", from.toLocalDateTime());
		query.setParameter("to", to.toLocalDateTime());
		query.setParameter("matl", material);

		return query.getResultList();
	}

	public OeeEvent fetchLastBoundEvent(Equipment equipment, OeeEventType type, OffsetDateTime dateTime) {
		final String LAST_EVENT = "Event.Last.Bound";

		if (namedQueryMap.get(LAST_EVENT) == null) {
			createNamedQuery(LAST_EVENT,
					"SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type "
							+ "AND e.startTime.localDateTime <= :dateTime ORDER BY e.startTime.localDateTime DESC");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(LAST_EVENT, OeeEvent.class);
		query.setParameter("equipment", equipment);
		query.setParameter("type", type);
		query.setParameter("dateTime", dateTime.toLocalDateTime());
		query.setMaxResults(1);
		List<OeeEvent> records = query.getResultList();

		OeeEvent record = null;
		if (records.size() == 1) {
			record = records.get(0);
		}

		return record;
	}

	public OeeEvent fetchLastEvent(Equipment equipment, OeeEventType type) {
		final String LAST_EVENT = "Event.Last";

		if (namedQueryMap.get(LAST_EVENT) == null) {
			createNamedQuery(LAST_EVENT,
					"SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type ORDER BY e.startTime.localDateTime DESC");
		}

		TypedQuery<OeeEvent> query = getEntityManager().createNamedQuery(LAST_EVENT, OeeEvent.class);
		query.setParameter("equipment", equipment);
		query.setParameter("type", type);
		query.setMaxResults(1);
		List<OeeEvent> records = query.getResultList();

		OeeEvent record = null;
		if (records.size() == 1) {
			record = records.get(0);
		}

		return record;
	}

	public int purge(Equipment equipment, OffsetDateTime cutoff) throws Exception {
		EntityManager em = getEntityManager();

		// preserve active setup records
		final String PURGE_OEE = "Oee.Purge";

		if (namedQueryMap.get(PURGE_OEE) == null) {
			createNamedQuery(PURGE_OEE,
					"DELETE FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType != :type AND e.startTime.localDateTime < :cutoff");
		}

		Query purgeOee = em.createNamedQuery(PURGE_OEE);
		purgeOee.setParameter("equipment", equipment);
		purgeOee.setParameter("cutoff", cutoff.toLocalDateTime());
		purgeOee.setParameter("type", OeeEventType.MATL_CHANGE);

		// purge inactive setup records
		final String PURGE_MATL = "Matl.Purge";

		if (namedQueryMap.get(PURGE_MATL) == null) {
			createNamedQuery(PURGE_MATL,
					"DELETE FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type AND e.endTime.localDateTime IS NOT NULL AND e.endTime.localDateTime < :cutoff");
		}

		Query purgeMaterial = em.createNamedQuery(PURGE_MATL);
		purgeMaterial.setParameter("cutoff", cutoff.toLocalDateTime());
		purgeMaterial.setParameter("equipment", equipment);
		purgeMaterial.setParameter("type", OeeEventType.MATL_CHANGE);

		EntityTransaction txn = null;

		try {
			// start transaction
			txn = em.getTransaction();
			txn.begin();

			// execute the deletions
			int deletedCount = purgeOee.executeUpdate();
			purgeMaterial.executeUpdate();

			// commit transaction
			txn.commit();

			return deletedCount;
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			em.close();
		}
	}

	/**
	 * Execute the SQL insert, update or delete
	 * 
	 * @param sql SQL insert, update or delete statement
	 * @return Number of rows inserted
	 */
	public int executeUpdate(String sql) {
		EntityManager em = getEntityManager();

		EntityTransaction txn = null;

		try {
			// start transaction
			txn = em.getTransaction();
			txn.begin();

			// execute the deletions
			int updatedCount = em.createNativeQuery(sql).executeUpdate();

			// commit transaction
			txn.commit();

			return updatedCount;
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
			throw e;
		} finally {
			em.close();
		}
	}

	/**
	 * Execute the SQL query
	 * 
	 * @param sql SQL select statement
	 * @return JSON string of result list
	 */
	@SuppressWarnings("unchecked")
	public String executeQuery(String sql) {
		List<Object[]> rowList = getEntityManager().createNativeQuery(sql).getResultList();
		Gson gson = new Gson();
		return gson.toJson(rowList);
	}

	/**
	 * Fetch database interface table events with the specified status
	 * 
	 * @param status {@link DatabaseEventStatus}
	 * @return List of {@link DatabaseEvent}
	 */
	public List<DatabaseEvent> fetchDatabaseEvents(DatabaseEventStatus status) {
		final String NEW_EVENTS = "DATABASE_EVENT.NEW";

		if (namedQueryMap.get(NEW_EVENTS) == null) {
			createNamedQuery(NEW_EVENTS,
					"SELECT event FROM DatabaseEvent event WHERE status = :status ORDER BY event.eventTime.localDateTime ASC");
		}

		TypedQuery<DatabaseEvent> query = getEntityManager().createNamedQuery(NEW_EVENTS, DatabaseEvent.class);
		query.setParameter("status", status);

		return query.getResultList();
	}

	/**
	 * Fetch database interface table events with the specified status and source id
	 * 
	 * @param status   {@link DatabaseEventStatus}
	 * @param sourceId event source identifier
	 * @return List of {@link DatabaseEvent}
	 */
	public List<DatabaseEvent> fetchDatabaseEvents(DatabaseEventStatus status, String sourceId) {
		final String NEW_EVENTS_SOURCE = "DATABASE_EVENT.NEW.SOURCE";

		if (namedQueryMap.get(NEW_EVENTS_SOURCE) == null) {
			createNamedQuery(NEW_EVENTS_SOURCE,
					"SELECT event FROM DatabaseEvent event WHERE status = :status AND sourceId = :sourceId ORDER BY event.eventTime.localDateTime ASC");
		}

		TypedQuery<DatabaseEvent> query = getEntityManager().createNamedQuery(NEW_EVENTS_SOURCE, DatabaseEvent.class);
		query.setParameter("status", status);
		query.setParameter("sourceId", sourceId);

		return query.getResultList();
	}

	/**
	 * Fetch OEE events for the equipment and event type over the specified period
	 * 
	 * @param equipment {@link Equipment}
	 * @param type      {@link OeeEventType}
	 * @param from      starting date and time
	 * @param to        ending date and time
	 * @return List of {@link OeeEvent}
	 */
	public List<OeeEvent> fetchEvents(Equipment equipment, OeeEventType type, OffsetDateTime from, OffsetDateTime to) {
		String qry = "SELECT e FROM OeeEvent e WHERE e.equipment = :equipment AND e.eventType = :type ";

		if (from != null) {
			qry += "AND e.startTime.localDateTime >= :from ";
		}

		if (to != null) {
			qry += "AND e.startTime.localDateTime < :to ";
		}
		qry += " ORDER BY e.startTime.localDateTime ASC";

		TypedQuery<OeeEvent> query = getEntityManager().createQuery(qry, OeeEvent.class);
		query.setParameter("type", type);
		query.setParameter("equipment", equipment);

		if (from != null) {
			query.setParameter("from", from.toLocalDateTime());
		}

		if (to != null) {
			query.setParameter("to", to.toLocalDateTime());
		}

		return query.getResultList();
	}

	public static String getJdbcConnection() {
		return jdbcConnection;
	}

	public static String getUserName() {
		return jdbcUserName;
	}

	public static String getUserPassword() {
		return jdbcPassword;
	}
}
