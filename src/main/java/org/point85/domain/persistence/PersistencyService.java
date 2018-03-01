package org.point85.domain.persistence;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.point85.domain.collector.BaseEvent;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.SetupHistory;
import org.point85.domain.http.HttpSource;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.NamedObject;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.script.ScriptResolverType;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitOfMeasure.MeasurementType;
import org.point85.domain.uom.UnitType;
import org.point85.domain.web.WebSource;
import org.point85.domain.workschedule.Rotation;
import org.point85.domain.workschedule.Team;
import org.point85.domain.workschedule.WorkSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistencyService {
	// JPA persistence unit name
	private static final String PERSISTENCE_UNIT = "OEE";

	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// entity manager factory
	private EntityManagerFactory emf;

	// singleton service
	private static PersistencyService persistencyService;

	private PersistencyService() {

	}

	public static PersistencyService instance() {
		if (persistencyService == null) {
			persistencyService = new PersistencyService();
		}
		return persistencyService;
	}

	public boolean contains(EntityManager em, KeyedObject entity) {
		return em.contains(entity);
	}

	public boolean isLoaded(KeyedObject entity) {
		return emf.getPersistenceUnitUtil().isLoaded(entity);
	}

	// create the EntityManager
	private EntityManager createEntityManager() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
		}
		return emf.createEntityManager();
	}

	// execute the named query
	List<?> executeNamedQuery(String queryName, Map<String, Object> parameters) {
		Query query = createEntityManager().createNamedQuery(queryName);

		if (parameters != null) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}
		return query.getResultList();
	}

	public List<String> fetchPlantEntityNames() {
		@SuppressWarnings("unchecked")
		List<String> values = (List<String>) executeNamedQuery(PlantEntity.ENTITY_NAMES, null);

		return values;
	}

	// fetch persistent object by its primary key
	public Object fetchByKey(Class<?> clazz, Long key) throws Exception {
		Object found = createEntityManager().find(clazz, key);
		return found;
	}

	// fetch NamedObject by its name
	public NamedObject fetchByName(String queryName, String objectName) throws Exception {
		NamedObject result = null;

		if (objectName != null) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("name", objectName);

			result = (NamedObject) fetchObject(queryName, parameters);
		}
		return result;
	}

	// fetch list of PersistentObjects by names
	public List<PlantEntity> fetchEntitiesByName(List<String> names) throws Exception {
		Query query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_BY_NAME_LIST);
		query.setParameter("names", names);

		@SuppressWarnings("unchecked")
		List<PlantEntity> entities = query.getResultList();

		return entities;
	}

	public List<ScriptResolver> fetchScriptResolvers() {
		Query query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_ALL);

		@SuppressWarnings("unchecked")
		List<ScriptResolver> resolvers = query.getResultList();

		return resolvers;
	}

	public List<String> fetchResolverSourceIds(String equipmentName, DataSourceType sourceType) {
		Query query = createEntityManager().createNamedQuery(Equipment.EQUIPMENT_SOURCE_IDS);
		query.setParameter("name", equipmentName);
		query.setParameter("type", sourceType);

		@SuppressWarnings("unchecked")
		List<String> sources = query.getResultList();

		return sources;
	}

	public List<DataSource> fetchDataSources(DataSourceType sourceType) {
		Query query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE);
		query.setParameter("type", sourceType);

		@SuppressWarnings("unchecked")
		List<DataSource> sources = query.getResultList();

		return sources;
	}

	// fetch persistent object by a named query
	public Object fetchObject(String queryName, Map<String, Object> parameters) throws Exception {

		Object entity = null;

		Query query = createEntityManager().createNamedQuery(queryName);

		if (parameters != null) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}

		try {
			entity = query.getSingleResult();
		} catch (Exception e) {
			// not id db yet
		}
		return entity;
	}

	// remove the PersistentObject from the persistence context
	public void evict(KeyedObject object) {
		if (object == null) {
			return;
		}
		createEntityManager().detach(object);
	}

	// save the Persistent Object to the database
	public Object save(KeyedObject object) throws Exception {
		EntityManager em = createEntityManager();
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

	// insert the object into the database
	public void persist(BaseEvent object) throws Exception {
		logger.info("Persisting object of class " + object.getClass().getSimpleName());

		EntityManager em = createEntityManager();
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

	// delete the PersistentObjectfrom the database
	public void delete(KeyedObject keyed) throws Exception {

		logger.info("Deleting persistent object of class " + keyed.getClass().getSimpleName() + " with key "
				+ keyed.getKey());

		if (keyed instanceof WorkSchedule) {
			// check for plant entity references
			List<PlantEntity> entities = fetchEntityCrossReferences((WorkSchedule) keyed);

			if (entities.size() > 0) {
				String refs = "";
				for (PlantEntity entity : entities) {
					if (refs.length() > 0) {
						refs += ", ";
					}
					refs += entity.getName();
				}
				throw new Exception("WorkSchedule " + ((WorkSchedule) keyed).getName()
						+ " is being referenced by plant entities " + refs);
			}
		}

		EntityManager em = createEntityManager();
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
		Query query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ALL);

		@SuppressWarnings("unchecked")
		List<PlantEntity> entities = query.getResultList();

		return entities;
	}

	// top-level plant entities
	public List<PlantEntity> fetchTopPlantEntities() {
		Query query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ROOTS);

		@SuppressWarnings("unchecked")
		List<PlantEntity> entities = query.getResultList();

		return entities;
	}

	public List<String> fetchTopPlantEntityNames() {
		Query query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ROOT_NAMES);

		@SuppressWarnings("unchecked")
		List<String> entities = query.getResultList();

		return entities;
	}

	public List<DataCollector> fetchCollectorsByHostAndState(List<String> hostNames, List<CollectorState> states) {
		Query query = createEntityManager().createNamedQuery(DataCollector.COLLECTOR_BY_HOST_BY_STATE);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);

		@SuppressWarnings("unchecked")
		List<DataCollector> collectors = query.getResultList();

		return collectors;
	}

	public List<DataCollector> fetchCollectorsByState(List<CollectorState> states) {
		Query query = createEntityManager().createNamedQuery(DataCollector.COLLECTOR_BY_STATE);
		query.setParameter("states", states);

		@SuppressWarnings("unchecked")
		List<DataCollector> collectors = query.getResultList();

		return collectors;
	}

	public List<ScriptResolver> fetchScriptResolversByHost(List<String> hostNames, List<CollectorState> states)
			throws Exception {
		Query query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_BY_HOST);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);

		@SuppressWarnings("unchecked")
		List<ScriptResolver> resolvers = query.getResultList();

		return resolvers;
	}

	public List<ScriptResolver> fetchScriptResolversByCollector(List<String> definitionNames) throws Exception {
		Query query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_BY_COLLECTOR);
		query.setParameter("names", definitionNames);

		@SuppressWarnings("unchecked")
		List<ScriptResolver> resolvers = query.getResultList();

		return resolvers;
	}

	public List<Material> fetchMaterialsByCategory(String category) throws Exception {
		Query query = createEntityManager().createNamedQuery(Material.MATLS_BY_CATEGORY);
		query.setParameter("category", category);

		@SuppressWarnings("unchecked")
		List<Material> materials = query.getResultList();

		return materials;
	}

	public List<DataCollector> fetchAllDataCollectors() {
		Query query = createEntityManager().createNamedQuery(DataCollector.COLLECT_ALL);

		@SuppressWarnings("unchecked")
		List<DataCollector> configs = query.getResultList();

		return configs;
	}

	public List<Material> fetchAllMaterials() {
		Query query = createEntityManager().createNamedQuery(Material.MATL_ALL);

		@SuppressWarnings("unchecked")
		List<Material> material = query.getResultList();

		return material;
	}

	public List<String> fetchMaterialCategories() {
		Query query = createEntityManager().createNamedQuery(Material.MATL_CATEGORIES);

		@SuppressWarnings("unchecked")
		List<String> categories = query.getResultList();

		return categories;
	}

	public List<Reason> fetchAllReasons() {
		Query query = createEntityManager().createNamedQuery(Reason.REASON_ALL);

		@SuppressWarnings("unchecked")
		List<Reason> reasons = query.getResultList();

		return reasons;
	}

	// top-level reasons
	public List<Reason> fetchTopReasons() {
		Query query = createEntityManager().createNamedQuery(Reason.REASON_ROOTS);

		@SuppressWarnings("unchecked")
		List<Reason> reasons = query.getResultList();

		return reasons;
	}

	public List<String> fetchProgIds() {
		Query query = createEntityManager().createNamedQuery(OpcDaSource.DA_PROG_IDS);

		@SuppressWarnings("unchecked")
		List<String> ids = query.getResultList();

		return ids;
	}

	public List<OpcUaSource> fetchOpcUaSources() {
		Query query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE);
		query.setParameter("type", DataSourceType.OPC_UA);

		@SuppressWarnings("unchecked")
		List<OpcUaSource> ids = query.getResultList();

		return ids;
	}

	public List<HttpSource> fetchHttpSources() {
		Query query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE);
		query.setParameter("type", DataSourceType.HTTP);

		@SuppressWarnings("unchecked")
		List<HttpSource> sources = query.getResultList();

		return sources;
	}

	public List<MessagingSource> fetchMessagingSources() {
		Query query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE);
		query.setParameter("type", DataSourceType.MESSAGING);

		@SuppressWarnings("unchecked")
		List<MessagingSource> sources = query.getResultList();

		return sources;
	}

	public List<WebSource> fetchWebSources() {
		Query query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE);
		query.setParameter("type", DataSourceType.WEB);

		@SuppressWarnings("unchecked")
		List<WebSource> sources = query.getResultList();

		return sources;
	}

	// ******************** work schedule related *******************************
	public List<WorkSchedule> fetchWorkSchedules() {
		@SuppressWarnings("unchecked")
		List<WorkSchedule> values = (List<WorkSchedule>) executeNamedQuery(WorkSchedule.WS_SCHEDULES, null);
		return values;
	}

	public List<String> fetchWorkScheduleNames() {
		@SuppressWarnings("unchecked")
		List<String> values = (List<String>) executeNamedQuery(WorkSchedule.WS_NAMES, null);
		return values;
	}

	// fetch WorkSchedule by its primary key
	public WorkSchedule fetchWorkScheduleByKey(Long key) throws Exception {
		return (WorkSchedule) fetchByKey(WorkSchedule.class, key);
	}

	// fetch WorkSchedule by its unique name
	public WorkSchedule fetchWorkScheduleByName(String name) throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", name);

		return fetchWorkSchedule(WorkSchedule.WS_BY_NAME, parameters);
	}

	// fetch WorkSchedule by a named query
	public WorkSchedule fetchWorkSchedule(String queryName, Map<String, Object> parameters) throws Exception {
		Query query = createEntityManager().createNamedQuery(queryName);

		if (parameters != null) {
			for (Entry<String, Object> entry : parameters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}

		return (WorkSchedule) query.getSingleResult();
	}

	// fetch Team by its primary key
	public Team fetchTeamByKey(Long key) throws Exception {
		return (Team) fetchByKey(Team.class, key);
	}

	public OffsetDateTime fetchDatabaseTime() {
		String mssqlQuery = "select convert(nvarchar(100), SYSDATETIMEOFFSET(), 126)";
		Query query = createEntityManager().createNativeQuery(mssqlQuery);
		String result = (String) query.getSingleResult();
		OffsetDateTime time = OffsetDateTime.parse(result, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return time;
	}

	// get any Team references to the Rotation
	public List<Team> fetchTeamCrossReferences(Rotation rotation) throws Exception {
		Long key = rotation.getKey();

		Query query = createEntityManager().createNamedQuery(WorkSchedule.WS_ROT_XREF);
		query.setParameter(1, key);

		@SuppressWarnings("unchecked")
		List<Long> keys = (List<Long>) query.getResultList();

		List<Team> referencingTeams = new ArrayList<>(keys.size());

		// get the referenced Teams
		for (Long primaryKey : keys) {
			Team referencing = fetchTeamByKey(primaryKey);
			referencingTeams.add(referencing);
		}
		return referencingTeams;
	}

	@SuppressWarnings("unchecked")
	public List<PlantEntity> fetchEntityCrossReferences(WorkSchedule schedule) {
		Query query = createEntityManager().createNamedQuery(WorkSchedule.WS_ENT_XREF);
		query.setParameter("schedule", schedule);

		return (List<PlantEntity>) query.getResultList();
	}

	// ******************** unit of measure related ***************************
	// get symbols and names in this category
	public List<Object[]> fetchUomSymbolsAndNames(String category) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("category", category);

		@SuppressWarnings("unchecked")
		List<Object[]> values = (List<Object[]>) executeNamedQuery(UnitOfMeasure.UOM_CAT_SYMBOLS, parameters);

		return values;
	}

	// fetch all defined categories
	public List<String> fetchCategories() {
		@SuppressWarnings("unchecked")
		List<String> categories = (List<String>) executeNamedQuery(UnitOfMeasure.UOM_CATEGORIES, null);
		Collections.sort(categories);
		return categories;
	}

	// query for UOM based on its unique symbol
	public UnitOfMeasure fetchUOMBySymbol(String symbol) throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("symbol", symbol);

		UnitOfMeasure uom = (UnitOfMeasure) fetchObject(UnitOfMeasure.UOM_BY_SYMBOL, parameters);

		return uom;
	}

	public List<UnitOfMeasure> fetchUomsByCategory(String category) throws Exception {
		Query query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_BY_CATEGORY);
		query.setParameter("category", category);

		@SuppressWarnings("unchecked")
		List<UnitOfMeasure> uoms = query.getResultList();

		return uoms;
	}

	// fetch UOM by its enumeration
	public UnitOfMeasure fetchUOMByUnit(Unit unit) throws Exception {
		UnitOfMeasure uom = null;

		if (unit == null) {
			return uom;
		}

		// check in the database
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("unit", unit);

		// fetch by Unit enum
		Object result = fetchObject(UnitOfMeasure.UOM_BY_UNIT, parameters);

		if (result != null) {
			uom = (UnitOfMeasure) result;
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
			fetched = fetchUOMBySymbol(id);

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
			fetched = fetchUOMBySymbol(id);

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
			fetched = fetchUOMBySymbol(id);

			if (fetched != null) {
				uom1 = fetched;
			}

			// multiplicand
			UnitOfMeasure uom2 = uom.getMultiplicand();
			id = uom2.getSymbol();
			UnitOfMeasure fetched2 = fetchUOMBySymbol(id);

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
			fetched = fetchUOMBySymbol(id);

			if (fetched != null) {
				uom1 = fetched;
			}

			// divisor
			UnitOfMeasure uom2 = uom.getDivisor();
			id = uom2.getSymbol();
			UnitOfMeasure fetched2 = fetchUOMBySymbol(id);

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
			fetched = fetchUOMBySymbol(id);

			if (fetched != null) {
				// already in database
				uom.setPowerUnit(fetched, uom.getPowerExponent());
			}

			// units referenced by the power base
			fetchReferencedUnits(referenced);
		}
	}

	public SetupHistory fetchLastHistory(Equipment equipment, ScriptResolverType type) {
		Query query = createEntityManager().createNamedQuery(SetupHistory.LAST_RECORD);
		query.setParameter("equipment", equipment);
		query.setParameter("type", type);
		query.setMaxResults(1);

		SetupHistory history = null;
		try {
			history = (SetupHistory) query.getSingleResult();
		} catch (Exception e) {
			// no history yet
		}

		return history;
	}

	// fetch symbols and their names for this UOM type
	public List<Object[]> fetchSymbolsAndNames(UnitType unitType) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("type", unitType);

		@SuppressWarnings("unchecked")
		List<Object[]> values = (List<Object[]>) executeNamedQuery(UnitOfMeasure.UOM_SYMBOLS, parameters);

		return values;
	}

	public List<EquipmentMaterial> fetchEquipmentMaterials(UnitOfMeasure uom) throws Exception {
		Query query = createEntityManager().createNamedQuery(EquipmentMaterial.EQM_XREF);
		query.setParameter("uom", uom);

		@SuppressWarnings("unchecked")
		List<EquipmentMaterial> eqms = (List<EquipmentMaterial>) query.getResultList();

		return eqms;
	}
	
	public List<UnitOfMeasure> fetchUomCrossReferences(UnitOfMeasure uom) throws Exception {
		Query query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_XREF);
		query.setParameter("uom", uom);

		@SuppressWarnings("unchecked")
		List<UnitOfMeasure> uoms = (List<UnitOfMeasure>) query.getResultList();

		return uoms;
	}

}
