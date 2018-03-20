package org.point85.domain.persistence;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
import org.point85.domain.collector.AvailabilityHistory;
import org.point85.domain.collector.AvailabilitySummary;
import org.point85.domain.collector.BaseRecord;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.ProductionHistory;
import org.point85.domain.collector.ProductionSummary;
import org.point85.domain.collector.SetupHistory;
import org.point85.domain.http.HttpSource;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.schedule.NonWorkingPeriod;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.RotationSegment;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitOfMeasure.MeasurementType;
import org.point85.domain.uom.UnitType;
import org.point85.domain.web.WebSource;

public class PersistenceService {
	// time in sec to wait for EntityManagerFactory creation to complete
	private static final int EMF_CREATION_TO_SEC = 15;

	// entity manager factory
	private EntityManagerFactory emf;

	// singleton service
	private static PersistenceService persistencyService;

	private CompletableFuture<EntityManagerFactory> emfFuture;

	private Map<String, Boolean> namedQueryMap;

	private PersistenceService() {
		namedQueryMap = new ConcurrentHashMap<>();
	}

	public synchronized static PersistenceService instance() {
		if (persistencyService == null) {
			persistencyService = new PersistenceService();
		}
		return persistencyService;
	}

	public void initialize(String jdbcUrl, String userName, String password) {
		// create EM on a a background thread
		emfFuture = CompletableFuture.supplyAsync(() -> {
			try {
				createContainerManagedEntityManagerFactory(jdbcUrl, userName, password);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return emf;
		});
	}

	public void close() {
		if (emf != null && emf.isOpen()) {
			emf.close();
		}
	}

	public EntityManagerFactory getEntityManagerFactory() {
		if (emf == null && emfFuture != null) {
			try {
				emf = emfFuture.get(EMF_CREATION_TO_SEC, TimeUnit.SECONDS);
			} catch (Exception e) {
				e.printStackTrace();
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

	// fetch list of PersistentObjects by names
	public List<PlantEntity> fetchEntitiesByName(List<String> names) throws Exception {
		final String ENTITY_BY_NAME_LIST = "ENTITY.ByNameList";

		if (namedQueryMap.get(ENTITY_BY_NAME_LIST) == null) {
			createNamedQuery(ENTITY_BY_NAME_LIST, "SELECT ent FROM PlantEntity ent WHERE ent.name IN :names");
		}

		TypedQuery<PlantEntity> query = getEntityManager().createNamedQuery(ENTITY_BY_NAME_LIST, PlantEntity.class);
		query.setParameter("names", names);
		return query.getResultList();
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
	public Object save(KeyedObject object) throws Exception {
		EntityManager em = getEntityManager();
		EntityTransaction txn = null;

		try {
			txn = em.getTransaction();
			txn.begin();

			// merge this entity into the PU and save
			Object merged = em.merge(object);

			// commit transaction
			txn.commit();

			return merged;
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
				e.printStackTrace();
			}
			throw new Exception(e.getMessage());
		} finally {
			em.close();
		}
	}

	// insert the record into the database
	public void persist(BaseRecord object) throws Exception {
		EntityManager em = getEntityManager();
		EntityTransaction txn = null;

		try {
			txn = em.getTransaction();
			txn.begin();

			// insert object
			em.persist(object);

			// commit transaction
			txn.commit();
		} catch (Exception e) {
			// roll back transaction
			if (txn != null && txn.isActive()) {
				txn.rollback();
				e.printStackTrace();
			}
			throw new Exception(e.getMessage());
		} finally {
			em.close();
		}
	}

	public void checkReferences(KeyedObject keyed) throws Exception {
		if (keyed instanceof Rotation) {
			Rotation rotation = (Rotation) keyed;

			// check for team reference
			List<Team> referencingTeams = fetchTeamCrossReferences(rotation);

			if (referencingTeams.size() != 0) {
				String refs = "";

				for (Team team : referencingTeams) {
					if (refs.length() > 0) {
						refs += ", ";
					}
					refs += team.getName();
				}
				throw new Exception(
						"Rotation " + rotation.getName() + " cannot be deleted.  It is referenced by team(s) " + refs);
			}
		} else if (keyed instanceof CollectorDataSource) {
			CollectorDataSource source = (CollectorDataSource) keyed;

			// check for script resolver references
			List<EventResolver> resolvers = fetchResolverCrossReferences(source);

			if (resolvers.size() > 0) {
				String refs = "";
				for (EventResolver resolver : resolvers) {
					if (refs.length() > 0) {
						refs += ", ";
					}
					refs += resolver.getSourceId();
				}
				throw new Exception("Collector source " + source.getName()
						+ " cannot be deleted.  It is being referenced by these script resolvers: " + refs);
			}
		} else if (keyed instanceof WorkSchedule) {
			WorkSchedule schedule = (WorkSchedule) keyed;

			// check for plant entity references
			List<PlantEntity> entities = fetchEntityCrossReferences(schedule);

			if (entities.size() > 0) {
				String refs = "";
				for (PlantEntity entity : entities) {
					if (refs.length() > 0) {
						refs += ", ";
					}
					refs += entity.getName();
				}
				throw new Exception("Work schedule " + schedule.getName()
						+ " cannot be deleted.  It is being referenced by these plant entities: " + refs);
			}
		} else if (keyed instanceof UnitOfMeasure) {
			UnitOfMeasure uom = (UnitOfMeasure) keyed;

			// check for usage by equipment material
			List<EquipmentMaterial> eqms = fetchEquipmentMaterials(uom);

			if (eqms.size() != 0) {
				String refs = "";
				for (int i = 0; i < eqms.size(); i++) {
					if (i > 0) {
						refs += ", ";
					}

					refs += eqms.get(i).getEquipment().getName();
				}
				throw new Exception("Unit of measure " + uom.getSymbol()
						+ " cannot be deleted.  It is being referenced by this equipment: " + refs);
			}

			// check for usage by UOM
			List<UnitOfMeasure> uoms = PersistenceService.instance().fetchUomCrossReferences(uom);

			if (uoms.size() != 0) {
				String refs = "";
				for (int i = 0; i < uoms.size(); i++) {
					if (i > 0) {
						refs += ", ";
					}

					refs += uoms.get(i).getSymbol();
				}
				throw new Exception("Unit of measure " + uom.getSymbol()
						+ " cannot be deleted.  It is being referenced by these units of measure: " + refs);
			}
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
				e.printStackTrace();
			}
			throw new Exception(e.getMessage());
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

	// fetch Team by its primary key
	public Team fetchTeamByKey(Long key) throws Exception {
		return getEntityManager().find(Team.class, key);
	}

	public OffsetDateTime fetchDatabaseTime() {
		String mssqlQuery = "select convert(nvarchar(100), SYSDATETIMEOFFSET(), 126)";
		Query query = getEntityManager().createNativeQuery(mssqlQuery);
		String result = (String) query.getSingleResult();
		OffsetDateTime time = OffsetDateTime.parse(result, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return time;
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
		return getEntityManager().find(UnitOfMeasure.class, key);
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
			createNamedQuery(UOM_CATEGORIES, "SELECT DISTINCT uom.category FROM UnitOfMeasure uom");
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
		return query.getResultList();
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

	public SetupHistory fetchLastSetupHistory(Equipment equipment) {
		final String LAST_SETUP = "Setup.Last";

		if (namedQueryMap.get(LAST_SETUP) == null) {
			createNamedQuery(LAST_SETUP,
					"SELECT hist FROM SetupHistory hist WHERE hist.equipment = :equipment ORDER BY hist.sourceTimestamp DESC");
		}

		TypedQuery<SetupHistory> query = getEntityManager().createNamedQuery(LAST_SETUP, SetupHistory.class);
		query.setParameter("equipment", equipment);
		query.setMaxResults(1);
		List<SetupHistory> histories = query.getResultList();

		SetupHistory history = null;
		if (histories.size() == 1) {
			history = histories.get(0);
		}

		return history;
	}

	public List<EquipmentMaterial> fetchEquipmentMaterials(UnitOfMeasure uom) throws Exception {
		final String EQM_XREF = "EQM.XRef";

		if (namedQueryMap.get(EQM_XREF) == null) {
			createNamedQuery(EQM_XREF,
					"SELECT eqm FROM EquipmentMaterial eqm WHERE runRateUOM = :uom OR rejectUOM = :uom");
		}

		TypedQuery<EquipmentMaterial> query = getEntityManager().createNamedQuery(EQM_XREF, EquipmentMaterial.class);
		query.setParameter("uom", uom);
		return query.getResultList();
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

	private void createContainerManagedEntityManagerFactory(String jdbcUrl, String userName, String password)
			throws Exception {
		// create the PU info
		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl("OEE", getEntityClassNames(),
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

		emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(persistenceUnitInfo,
				configuration);
	}

	private String[] getMappingFileNames() {
		return null;
	}

	private Class<?>[] getEntityClasses() {
		return new Class<?>[] { DataCollector.class, CollectorDataSource.class, AvailabilityHistory.class,
				ProductionHistory.class, SetupHistory.class, HttpSource.class, MessagingSource.class, OpcDaSource.class,
				OpcUaSource.class, WebSource.class, Area.class, Enterprise.class, Equipment.class,
				EquipmentMaterial.class, Material.class, PlantEntity.class, ProductionLine.class, Reason.class,
				Site.class, WorkCell.class, EventResolver.class, UnitOfMeasure.class, NonWorkingPeriod.class,
				Rotation.class, RotationSegment.class, Shift.class, Team.class, WorkSchedule.class,
				AvailabilitySummary.class, ProductionSummary.class };
	}

	private List<String> getEntityClassNames() {
		return Arrays.asList(getEntityClasses()).stream().map(Class::getName).collect(Collectors.toList());
	}

	private Properties createProperties(String jdbcUrl, String userName, String password) throws Exception {
		DatabaseType databaseType = null;

		if (jdbcUrl.contains("jdbc:sqlserver")) {
			databaseType = DatabaseType.MSSQL;
		} else if (jdbcUrl.contains("jdbc:oracle")) {
			databaseType = DatabaseType.ORACLE;
		} else if (jdbcUrl.contains("jdbc:hsqldb")) {
			databaseType = DatabaseType.HSQL;
		} else if (jdbcUrl.contains("jdbc:mysql")) {
			databaseType = DatabaseType.MYSQL;
		} else if (jdbcUrl.contains("jdbc:mysql")) {
			databaseType = DatabaseType.POSTGRES;
		} else {
			throw new Exception("Invalid JDBC URL: " + jdbcUrl);
		}

		Properties properties = new Properties();

		if (databaseType.equals(DatabaseType.MSSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect");
			properties.put("javax.persistence.jdbc.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} else if (databaseType.equals(DatabaseType.ORACLE)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
			properties.put("javax.persistence.jdbc.driver", "oracle.jdbc.driver.OracleDriver");
		} else if (databaseType.equals(DatabaseType.HSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			properties.put("javax.persistence.jdbc.driver", "org.hsqldb.jdbc.JDBCDriver");
		} else if (databaseType.equals(DatabaseType.MYSQL)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL57Dialect");
			properties.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
		} else if (databaseType.equals(DatabaseType.POSTGRES)) {
			properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
			properties.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
		}

		// jdbc connection
		properties.put("javax.persistence.jdbc.url", jdbcUrl);
		properties.put("javax.persistence.jdbc.user", userName);
		properties.put("javax.persistence.jdbc.password", password);

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

	public List<AvailabilitySummary> fetchAvailabilitySummary(Equipment equipment, OffsetDateTime from,
			OffsetDateTime to) {
		final String AVAIL_RECORDS = "Availability.FromTo";

		if (namedQueryMap.get(AVAIL_RECORDS) == null) {
			createNamedQuery(AVAIL_RECORDS,
					"SELECT a FROM AvailabilitySummary a WHERE a.equipment = :equipment AND (a.startTime BETWEEN :from AND :to)  AND (a.endTime BETWEEN :from AND :to)");
		}

		TypedQuery<AvailabilitySummary> query = getEntityManager().createNamedQuery(AVAIL_RECORDS,
				AvailabilitySummary.class);
		query.setParameter("equipment", equipment);
		query.setParameter("from", from);
		query.setParameter("to", to);

		List<AvailabilitySummary> summaries = query.getResultList();

		return summaries;
	}

}
