package org.point85.domain.persistence;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.point85.domain.collector.BaseEvent;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.collector.SetupHistory;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.script.ScriptResolverType;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitOfMeasure.MeasurementType;
import org.point85.domain.uom.UnitType;

public class PersistencyService {
	// JPA persistence unit name
	private static final String PERSISTENCE_UNIT = "OEE";

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

	public EntityManagerFactory getEntityManagerFactory() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
		}
		return emf;
	}

	// create the EntityManager
	public EntityManager createEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}

	public List<String> fetchPlantEntityNames() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_NAMES, String.class);
		return query.getResultList();
	}

	public PlantEntity fetchPlantEntityByName(String name) {
		PlantEntity entity = null;
		TypedQuery<PlantEntity> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_BY_NAME,
				PlantEntity.class);
		List<PlantEntity> entities = query.getResultList();

		if (entities.size() == 1) {
			entity = entities.get(0);
		}
		return entity;
	}

	// fetch list of PersistentObjects by names
	public List<PlantEntity> fetchEntitiesByName(List<String> names) throws Exception {
		TypedQuery<PlantEntity> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_BY_NAME_LIST,
				PlantEntity.class);
		query.setParameter("names", names);
		return query.getResultList();
	}

	public List<ScriptResolver> fetchScriptResolvers() {
		TypedQuery<ScriptResolver> query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_ALL,
				ScriptResolver.class);
		return query.getResultList();
	}

	public List<String> fetchResolverSourceIds(String equipmentName, DataSourceType sourceType) {
		TypedQuery<String> query = createEntityManager().createNamedQuery(Equipment.EQUIPMENT_SOURCE_IDS, String.class);
		query.setParameter("name", equipmentName);
		query.setParameter("type", sourceType);
		return query.getResultList();
	}

	public List<DataSource> fetchDataSources(DataSourceType sourceType) {
		TypedQuery<DataSource> query = createEntityManager().createNamedQuery(DataSource.SRC_BY_TYPE, DataSource.class);
		query.setParameter("type", sourceType);
		return query.getResultList();
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
		TypedQuery<PlantEntity> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ALL,
				PlantEntity.class);
		return query.getResultList();
	}

	// top-level plant entities
	public List<PlantEntity> fetchTopPlantEntities() {
		TypedQuery<PlantEntity> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ROOTS,
				PlantEntity.class);
		List<PlantEntity> entities = query.getResultList();

		return entities;
	}

	public List<String> fetchTopPlantEntityNames() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(PlantEntity.ENTITY_ROOT_NAMES, String.class);
		return query.getResultList();
	}

	public List<DataCollector> fetchCollectorsByHostAndState(List<String> hostNames, List<CollectorState> states) {
		TypedQuery<DataCollector> query = createEntityManager()
				.createNamedQuery(DataCollector.COLLECTOR_BY_HOST_BY_STATE, DataCollector.class);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<DataCollector> fetchCollectorsByState(List<CollectorState> states) {
		TypedQuery<DataCollector> query = createEntityManager().createNamedQuery(DataCollector.COLLECTOR_BY_STATE,
				DataCollector.class);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<ScriptResolver> fetchScriptResolversByHost(List<String> hostNames, List<CollectorState> states)
			throws Exception {
		TypedQuery<ScriptResolver> query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_BY_HOST,
				ScriptResolver.class);
		query.setParameter("names", hostNames);
		query.setParameter("states", states);
		return query.getResultList();
	}

	public List<ScriptResolver> fetchScriptResolversByCollector(List<String> definitionNames) throws Exception {
		TypedQuery<ScriptResolver> query = createEntityManager().createNamedQuery(ScriptResolver.RESOLVER_BY_COLLECTOR,
				ScriptResolver.class);
		query.setParameter("names", definitionNames);
		return query.getResultList();
	}

	public List<Material> fetchMaterialsByCategory(String category) throws Exception {
		TypedQuery<Material> query = createEntityManager().createNamedQuery(Material.MATLS_BY_CATEGORY, Material.class);
		query.setParameter("category", category);
		return query.getResultList();
	}

	public List<DataCollector> fetchAllDataCollectors() {
		TypedQuery<DataCollector> query = createEntityManager().createNamedQuery(DataCollector.COLLECT_ALL,
				DataCollector.class);
		return query.getResultList();
	}

	public List<Material> fetchAllMaterials() {
		TypedQuery<Material> query = createEntityManager().createNamedQuery(Material.MATL_ALL, Material.class);
		return query.getResultList();
	}

	public List<String> fetchMaterialCategories() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(Material.MATL_CATEGORIES, String.class);
		return query.getResultList();
	}

	public Material fetchMaterialByName(String materialName) {
		Material material = null;
		TypedQuery<Material> query = createEntityManager().createNamedQuery(Material.MATL_BY_NAME, Material.class);
		List<Material> materials = query.getResultList();

		if (materials.size() == 1) {
			material = materials.get(0);
		}
		return material;
	}

	public Material fetchMaterialByKey(Long key) throws Exception {
		return createEntityManager().find(Material.class, key);
	}

	public Reason fetchReasonByName(String name) {
		Reason reason = null;
		TypedQuery<Reason> query = createEntityManager().createNamedQuery(Reason.REASON_BY_NAME, Reason.class);
		List<Reason> reasons = query.getResultList();

		if (reasons.size() == 1) {
			reason = reasons.get(0);
		}
		return reason;
	}

	public Reason fetchReasonByKey(Long key) throws Exception {
		return createEntityManager().find(Reason.class, key);
	}

	public List<Reason> fetchAllReasons() {
		TypedQuery<Reason> query = createEntityManager().createNamedQuery(Reason.REASON_ALL, Reason.class);
		return query.getResultList();
	}

	// top-level reasons
	public List<Reason> fetchTopReasons() {
		TypedQuery<Reason> query = createEntityManager().createNamedQuery(Reason.REASON_ROOTS, Reason.class);
		return query.getResultList();
	}

	public List<String> fetchProgIds() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(OpcDaSource.DA_PROG_IDS, String.class);
		return query.getResultList();
	}

	public OpcDaSource fetchOpcDaSourceByName(String name) {
		OpcDaSource source = null;
		TypedQuery<OpcDaSource> query = createEntityManager().createNamedQuery(OpcDaSource.DA_SRC_BY_NAME,
				OpcDaSource.class);
		query.setParameter("name", name);

		List<OpcDaSource> sources = query.getResultList();

		if (sources.size() == 1) {
			source = sources.get(0);
		}
		return source;
	}

	public OpcUaSource fetchOpcUaSourceByName(String name) {
		OpcUaSource source = null;
		TypedQuery<OpcUaSource> query = createEntityManager().createNamedQuery(OpcUaSource.UA_SRC_BY_NAME,
				OpcUaSource.class);
		query.setParameter("name", name);

		List<OpcUaSource> sources = query.getResultList();

		if (sources.size() == 1) {
			source = sources.get(0);
		}
		return source;
	}

	// ******************** work schedule related *******************************
	public WorkSchedule fetchScheduleByKey(Long key) throws Exception {
		return createEntityManager().find(WorkSchedule.class, key);
	}

	public List<WorkSchedule> fetchWorkSchedules() {
		TypedQuery<WorkSchedule> query = createEntityManager().createNamedQuery(WorkSchedule.WS_SCHEDULES,
				WorkSchedule.class);
		return query.getResultList();
	}

	public List<String> fetchWorkScheduleNames() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(WorkSchedule.WS_NAMES, String.class);
		return query.getResultList();
	}

	// fetch Team by its primary key
	public Team fetchTeamByKey(Long key) throws Exception {
		return createEntityManager().find(Team.class, key);
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
		TypedQuery<Team> query = createEntityManager().createNamedQuery(WorkSchedule.WS_ROT_XREF, Team.class);
		query.setParameter("rotation", rotation);
		return query.getResultList();
	}

	public List<PlantEntity> fetchEntityCrossReferences(WorkSchedule schedule) {
		TypedQuery<PlantEntity> query = createEntityManager().createNamedQuery(WorkSchedule.WS_ENT_XREF,
				PlantEntity.class);
		query.setParameter("schedule", schedule);
		return query.getResultList();
	}

	// ******************** unit of measure related ***************************
	public UnitOfMeasure fetchUomByKey(Long key) throws Exception {
		return createEntityManager().find(UnitOfMeasure.class, key);
	}

	// get symbols and names in this category
	public List<String[]> fetchUomSymbolsAndNamesByCategory(String category) {
		TypedQuery<String[]> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_CAT_SYMBOLS,
				String[].class);
		query.setParameter("category", category);
		return query.getResultList();
	}

	// fetch symbols and their names for this UOM type
	public List<String[]> fetchUomSymbolsAndNamesByType(UnitType unitType) {
		TypedQuery<String[]> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_SYMBOLS, String[].class);
		query.setParameter("type", unitType);
		return query.getResultList();
	}

	// fetch all defined categories
	public List<String> fetchCategories() {
		TypedQuery<String> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_CATEGORIES, String.class);
		return query.getResultList();
	}

	// query for UOM based on its unique symbol
	public UnitOfMeasure fetchUOMBySymbol(String symbol) throws Exception {
		TypedQuery<UnitOfMeasure> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_BY_SYMBOL,
				UnitOfMeasure.class);
		query.setParameter("symbol", symbol);
		return query.getSingleResult();
	}

	public List<UnitOfMeasure> fetchUomsByCategory(String category) throws Exception {
		TypedQuery<UnitOfMeasure> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_BY_CATEGORY,
				UnitOfMeasure.class);
		query.setParameter("category", category);
		return query.getResultList();
	}

	// fetch UOM by its enumeration
	public UnitOfMeasure fetchUOMByUnit(Unit unit) throws Exception {
		UnitOfMeasure uom = null;

		if (unit == null) {
			return uom;
		}

		// fetch by Unit enum
		TypedQuery<UnitOfMeasure> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_BY_UNIT,
				UnitOfMeasure.class);
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
		TypedQuery<SetupHistory> query = createEntityManager().createNamedQuery(SetupHistory.LAST_RECORD,
				SetupHistory.class);
		query.setParameter("equipment", equipment);
		query.setParameter("type", type);
		query.setMaxResults(1);
		List<SetupHistory> histories = query.getResultList();

		SetupHistory history = null;
		if (histories.size() == 1) {
			history = histories.get(0);
		}

		return history;
	}

	public List<EquipmentMaterial> fetchEquipmentMaterials(UnitOfMeasure uom) throws Exception {
		TypedQuery<EquipmentMaterial> query = createEntityManager().createNamedQuery(EquipmentMaterial.EQM_XREF,
				EquipmentMaterial.class);
		query.setParameter("uom", uom);
		return query.getResultList();
	}

	public List<UnitOfMeasure> fetchUomCrossReferences(UnitOfMeasure uom) throws Exception {
		TypedQuery<UnitOfMeasure> query = createEntityManager().createNamedQuery(UnitOfMeasure.UOM_XREF,
				UnitOfMeasure.class);
		query.setParameter("uom", uom);
		return query.getResultList();
	}

	public DataCollector fetchCollectorByName(String name) {
		DataCollector collector = null;
		TypedQuery<DataCollector> query = createEntityManager().createNamedQuery(DataCollector.COLLECT_BY_NAME,
				DataCollector.class);
		List<DataCollector> collectors = query.getResultList();

		if (collectors.size() == 1) {
			collector = collectors.get(0);
		}
		return collector;
	}

}
