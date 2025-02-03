package org.point85.domain.exim;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.dto.AreaDto;
import org.point85.domain.dto.CronSourceDto;
import org.point85.domain.dto.DataCollectorDto;
import org.point85.domain.dto.DatabaseSourceDto;
import org.point85.domain.dto.EmailSourceDto;
import org.point85.domain.dto.EnterpriseDto;
import org.point85.domain.dto.EquipmentDto;
import org.point85.domain.dto.FileSourceDto;
import org.point85.domain.dto.HttpSourceDto;
import org.point85.domain.dto.JmsSourceDto;
import org.point85.domain.dto.KafkaSourceDto;
import org.point85.domain.dto.MaterialDto;
import org.point85.domain.dto.ModbusSourceDto;
import org.point85.domain.dto.MqttSourceDto;
import org.point85.domain.dto.OpcDaSourceDto;
import org.point85.domain.dto.OpcUaSourceDto;
import org.point85.domain.dto.ProductionLineDto;
import org.point85.domain.dto.ProficySourceDto;
import org.point85.domain.dto.ReasonDto;
import org.point85.domain.dto.RmqSourceDto;
import org.point85.domain.dto.SiteDto;
import org.point85.domain.dto.UnitOfMeasureDto;
import org.point85.domain.dto.WebSocketSourceDto;
import org.point85.domain.dto.WorkCellDto;
import org.point85.domain.dto.WorkScheduleDto;
import org.point85.domain.email.EmailSource;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpSource;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.jms.JmsSource;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.NamedObject;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.RotationSegment;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.socket.WebSocketSource;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton service to restore design-time objects from a file that was backed
 * up from another database.
 *
 */
public class Importer extends BaseExportImport {
	private static final Logger logger = LoggerFactory.getLogger(Importer.class);

	// singleton
	private static Importer importer;

	private Importer() {
		// Singleton
	}

	// objects in the database
	private Map<String, PlantEntity> entityMap = new HashMap<>();
	private Map<String, Reason> reasonMap = new HashMap<>();
	private Map<String, Material> materialMap = new HashMap<>();
	private Map<String, DataCollector> collectorMap = new HashMap<>();
	private Map<String, WorkSchedule> scheduleMap = new HashMap<>();

	/**
	 * Obtain an instance of an Importer
	 * 
	 * @return {@link Importer}
	 */
	public static synchronized Importer instance() {
		if (importer == null) {
			importer = new Importer();
		}
		return importer;
	}

	/**
	 * Restore the contents of the backup file to the database
	 * 
	 * @param file File with design-time objects
	 * @throws Exception Exception
	 */
	public synchronized void restore(File file) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Restoring backup from " + file.getCanonicalPath());
		}

		Path fileLocation = Paths.get(file.getCanonicalPath());
		byte[] bytes = Files.readAllBytes(fileLocation);

		String json = DomainUtils.gunzip(bytes);

		if (logger.isTraceEnabled()) {
			logger.trace(json);
		}

		ExportImportContent content = gson.fromJson(json, ExportImportContent.class);

		// data sources
		restoreDataSources(content);

		if (!content.getDataCollectors().isEmpty()) {
			restoreDataCollectors(content);
		}

		if (!content.getMaterials().isEmpty()) {
			restoreMaterials(content);
		}

		if (!content.getReasons().isEmpty()) {
			restoreReasons(content);
		}

		if (!content.getUOMs().isEmpty()) {
			restoreUOMs(content);
		}

		if (!content.getWorkSchedules().isEmpty()) {
			restoreWorkSchedules(content);
		}

		// entities
		restorePlantEntities(content);
	}
	
	private void checkExistance(NamedObject obj, String name, Boolean forCreate) throws Exception {
		if (forCreate != null) {
			if (forCreate && obj != null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("already.exists", name));
			}
				
			if (!forCreate && obj == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", name));
			}
		}
	}

	public List<KeyedObject> restoreMaterials(ExportImportContent content) throws Exception {
		// cache all materials
		List<Material> dbMaterials = PersistenceService.instance().fetchAllMaterials();

		materialMap.clear();
		for (Material dbMaterial : dbMaterials) {
			materialMap.put(dbMaterial.getName(), dbMaterial);
		}

		List<KeyedObject> toSaveMaterials = new ArrayList<>();

		// iterate over each exported material
		for (MaterialDto dto : content.getMaterials()) {
			Material material = materialMap.get(dto.getName());
			
			checkExistance(material, dto.getName(), content.isForCreate());

			if (material == null) {
				material = new Material(dto);
			} else {
				material.setAttributes(dto);
			}
			toSaveMaterials.add(material);

			if (logger.isInfoEnabled()) {
				logger.info("Imported material: " + dto.getName());
			}
		}

		if (!toSaveMaterials.isEmpty()) {
			PersistenceService.instance().save(toSaveMaterials);
		}
		return toSaveMaterials;
	}

	private void addChildReasons(Reason parent, List<ReasonDto> childDtos)
			throws Exception {
		// iterate over each child
		for (ReasonDto childDto : childDtos) {
			Reason childReason = reasonMap.get(childDto.getName());

			if (childReason == null) {
				// create reason
				childReason = new Reason(childDto);

				// add children
				parent.getChildren().add(childReason);

				// set parent
				childReason.setParent(parent);

				if (logger.isInfoEnabled()) {
					logger.info("Imported reason: " + childDto.getName());
				}
			}

			// recurse
			addChildReasons(childReason, childDto.getChildren());
		}
	}

	public List<KeyedObject> restoreReasons(ExportImportContent content) throws Exception {
		// cache all reasons
		List<Reason> dbReasons = PersistenceService.instance().fetchAllReasons();

		reasonMap.clear();
		for (Reason dbReason : dbReasons) {
			reasonMap.put(dbReason.getName(), dbReason);
		}

		// reasons to be saved
		List<KeyedObject> toSaveReasons = new ArrayList<>();

		// imported reasons
		for (ReasonDto dto : content.getReasons()) {
			Reason reason = reasonMap.get(dto.getName());
			checkExistance(reason, dto.getName(), content.isForCreate());
			
			Reason parent = null;
			if (dto.getParent()!= null) {
				parent = reasonMap.get(dto.getParent());
			}

			if (reason == null) {
				// create
				reason = new Reason(dto);
			} else {
				// update
				reason.setAttributes(dto);
			}
			toSaveReasons.add(reason);
			
			if (parent != null) {
				reason.setParent(parent);
			}

			// children
			addChildReasons(reason, dto.getChildren());

			if (logger.isInfoEnabled()) {
				logger.info("Imported reason: " + dto.getName());
			}
		}

		// save reasons
		if (!toSaveReasons.isEmpty()) {
			PersistenceService.instance().save(toSaveReasons);
		}
		
		return toSaveReasons;
	}

	public List<KeyedObject>  restorePlantEntities(ExportImportContent content) throws Exception {
		// cache all entities
		List<PlantEntity> dbPlantEntities = PersistenceService.instance().fetchAllPlantEntities();

		entityMap.clear();
		for (PlantEntity dbPlantEntity : dbPlantEntities) {
			entityMap.put(dbPlantEntity.getName(), dbPlantEntity);
		}

		List<KeyedObject> toSavePlantEntities = new ArrayList<>();

		// enterprises
		for (EnterpriseDto enterpriseDto : content.getEnterprises()) {
			Enterprise enterprise = (Enterprise) entityMap.get(enterpriseDto.getName());
			checkExistance(enterprise, enterpriseDto.getName(), content.isForCreate());

			if (enterprise == null) {
				// create
				enterprise = new Enterprise(enterpriseDto);
			} else {
				// update
				enterprise.setAttributes(enterpriseDto);
			}

			// add sites
			if (enterpriseDto.getSites() != null) {
				addSites(enterprise, enterpriseDto.getSites());
			}

			toSavePlantEntities.add(enterprise);

			if (logger.isInfoEnabled()) {
				logger.info("Imported enterprise: " + enterpriseDto.getName());
			}
		}

		// sites
		for (SiteDto siteDto : content.getSites()) {
			// get existing site
			Site site = (Site) entityMap.get(siteDto.getName());
			checkExistance(site, siteDto.getName(), content.isForCreate());

			if (site == null) {
				// create
				site = new Site(siteDto);
			} else {
				// update
				site.setAttributes(siteDto);
			}
			
			if (siteDto.getParent() != null) {
				PlantEntity parent = entityMap.get(siteDto.getParent());
				if (parent == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", siteDto.getParent()));
				}
				site.setParent(parent);
			}

			// add areas
			if (siteDto.getAreas() != null) {
				addAreas(site, siteDto.getAreas());
			}

			toSavePlantEntities.add(site);

			if (logger.isInfoEnabled()) {
				logger.info("Imported site: " + siteDto.getName());
			}
		}

		// areas
		for (AreaDto areaDto : content.getAreas()) {
			// get existing area
			Area area = (Area) entityMap.get(areaDto.getName());
			checkExistance(area, areaDto.getName(), content.isForCreate());

			if (area == null) {
				// create
				area = new Area(areaDto);
			} else {
				// update
				area.setAttributes(areaDto);
			}
			
			if (areaDto.getParent() != null) {
				PlantEntity parent = entityMap.get(areaDto.getParent());
				if (parent == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", areaDto.getParent()));
				}
				area.setParent(parent);
			}

			// add production lines
			if (areaDto.getProductionLines() != null) {
				addProductionLines(area, areaDto.getProductionLines());
			}

			toSavePlantEntities.add(area);

			if (logger.isInfoEnabled()) {
				logger.info("Imported area: " + areaDto.getName());
			}
		}

		// production lines
		for (ProductionLineDto lineDto : content.getProductionLines()) {
			// get existing line
			ProductionLine line = (ProductionLine) entityMap.get(lineDto.getName());
			checkExistance(line, lineDto.getName(), content.isForCreate());

			if (line == null) {
				// create
				line = new ProductionLine(lineDto);
			} else {
				// update
				line.setAttributes(lineDto);
			}
			
			if (lineDto.getParent() != null) {
				PlantEntity parent = entityMap.get(lineDto.getParent());
				if (parent == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", lineDto.getParent()));
				}
				line.setParent(parent);
			}

			// add work cells
			if (lineDto.getWorkCells() != null) {
				addWorkCells(line, lineDto.getWorkCells());
			}

			toSavePlantEntities.add(line);

			if (logger.isInfoEnabled()) {
				logger.info("Imported line: " + lineDto.getName());
			}
		}

		// work cells
		for (WorkCellDto cellDto : content.getWorkCells()) {
			// get existing work cell
			WorkCell cell = (WorkCell) entityMap.get(cellDto.getName());
			checkExistance(cell, cellDto.getName(), content.isForCreate());

			if (cell == null) {
				// new work cell
				cell = new WorkCell(cellDto);
			} else {
				cell.setAttributes(cellDto);
			}
			
			if (cellDto.getParent() != null) {
				PlantEntity parent = entityMap.get(cellDto.getParent());
				if (parent == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", cellDto.getParent()));
				}
				cell.setParent(parent);
			}

			// add equipment
			if (cellDto.getEquipment() != null) {
				addEquipment(cell, cellDto.getEquipment());
			}

			// save work cell
			toSavePlantEntities.add(cell);

			if (logger.isInfoEnabled()) {
				logger.info("Imported cell: " + cellDto.getName());
			}
		}

		// equipment
		for (EquipmentDto equipmentDto : content.getEquipment()) {
			// get existing equipment
			Equipment equipment = (Equipment) entityMap.get(equipmentDto.getName());
			checkExistance(equipment, equipmentDto.getName(), content.isForCreate());

			if (equipment == null) {
				// new equipment
				equipment = new Equipment(equipmentDto);
			} else {
				equipment.setAttributes(equipmentDto);
			}
						
			if (equipmentDto.getParent() != null) {
				PlantEntity parent = entityMap.get(equipmentDto.getParent());
				if (parent == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("does.not.exist", equipmentDto.getParent()));
				}
				equipment.setParent(parent);
			}

			// save equipment
			toSavePlantEntities.add(equipment);

			if (logger.isInfoEnabled()) {
				logger.info("Imported equipment: " + equipmentDto.getName());
			}
		}

		// save new entities
		if (!toSavePlantEntities.isEmpty()) {
			PersistenceService.instance().save(toSavePlantEntities);
		}
		
		return toSavePlantEntities;
	}

	public boolean addSites(Enterprise enterprise, List<SiteDto> siteDtos) throws Exception {
		boolean modified = false;

		for (SiteDto siteDto : siteDtos) {
			Site site = null;

			// check for site already in database
			if (entityMap.containsKey(siteDto.getName())) {
				site = (Site) entityMap.get(siteDto.getName());
			} else {
				// create entity
				site = new Site(siteDto);

				connectParentChild(enterprise, site);

				modified = true;
			}

			// add areas
			modified = addAreas(site, siteDto.getAreas());

			if (logger.isInfoEnabled()) {
				logger.info("Imported site: " + siteDto.getName());
			}
		}
		return modified;
	}

	public boolean addAreas(Site site, List<AreaDto> areaDtos) throws Exception {
		boolean modified = false;

		// iterate over each area
		for (AreaDto areaDto : areaDtos) {
			Area area = null;

			// check for area already in database
			if (entityMap.containsKey(areaDto.getName())) {
				area = (Area) entityMap.get(areaDto.getName());
			} else {
				// create entity
				area = new Area(areaDto);

				connectParentChild(site, area);

				modified = true;
			}

			// add lines
			modified = addProductionLines(area, areaDto.getProductionLines());

			if (logger.isInfoEnabled()) {
				logger.info("Imported area: " + areaDto.getName());
			}
		}
		return modified;
	}

	public boolean addProductionLines(Area area, List<ProductionLineDto> lineDtos) throws Exception {
		boolean modified = false;

		// iterate over each line
		for (ProductionLineDto lineDto : lineDtos) {
			ProductionLine line = null;

			// check for line already in database
			if (entityMap.containsKey(lineDto.getName())) {
				// already in DB
				line = (ProductionLine) entityMap.get(lineDto.getName());
			} else {
				// create entity
				line = new ProductionLine(lineDto);

				connectParentChild(area, line);

				modified = true;
			}

			// add cells
			modified = addWorkCells(line, lineDto.getWorkCells());

			if (logger.isInfoEnabled()) {
				logger.info("Imported line: " + lineDto.getName());
			}
		}
		return modified;
	}

	public boolean addWorkCells(ProductionLine line, List<WorkCellDto> workCellDtos) throws Exception {
		boolean modified = false;

		// iterate over each work cell
		for (WorkCellDto workCellDto : workCellDtos) {
			WorkCell cell = null;

			// check for cell already in database
			if (entityMap.containsKey(workCellDto.getName())) {
				// already in DB
				cell = (WorkCell) entityMap.get(workCellDto.getName());
			} else {
				// create entity
				cell = new WorkCell(workCellDto);

				connectParentChild(line, cell);

				modified = true;
			}

			// add equipment
			modified = addEquipment(cell, workCellDto.getEquipment());

			if (logger.isInfoEnabled()) {
				logger.info("Imported work cell: " + workCellDto.getName());
			}
		}
		return modified;
	}

	public boolean addEquipment(WorkCell cell, List<EquipmentDto> equipmentDtos) throws Exception {
		boolean modified = false;

		// iterate over each equipment
		for (EquipmentDto equipmentDto : equipmentDtos) {
			Equipment equipment = null;

			// check for equipment already in database
			if (entityMap.containsKey(equipmentDto.getName())) {
				// already in DB
				equipment = (Equipment) entityMap.get(equipmentDto.getName());
			} else {
				// create entity
				equipment = new Equipment(equipmentDto);

				connectParentChild(cell, equipment);

				modified = true;
			}

			if (logger.isInfoEnabled()) {
				logger.info("Imported equipment: " + equipmentDto.getName());
			}
		}
		return modified;
	}

	private void connectParentChild(PlantEntity parent, PlantEntity child) {
		// add child
		parent.getChildren().add(child);

		// set parent
		child.setParent(parent);
	}

	private void restoreUOMs(ExportImportContent content) throws Exception {
		// iterate through each exported UOM
		for (UnitOfMeasureDto dto : content.getUOMs()) {

			List<UnitOfMeasure> createdUOMs = new ArrayList<>();

			// check to see if already in the database, if so reference it
			UnitOfMeasure uom = PersistenceService.instance().fetchUomBySymbol(dto.getSymbol());

			if (uom == null) {
				uom = new UnitOfMeasure();
				List<UnitOfMeasure> restoredUOMs = uom.restoreAttributes(dto);
				createdUOMs.addAll(restoredUOMs);
				createdUOMs.add(uom);

				if (logger.isInfoEnabled()) {
					for (UnitOfMeasure restored : restoredUOMs) {
						logger.info("Imported UOM: " + restored.getName());
					}
				}
			}

			// find all other referenced UOMs
			List<UnitOfMeasure> referencedUOMs = new ArrayList<>();

			for (UnitOfMeasure createdUOM : createdUOMs) {
				UnitOfMeasure ref = createdUOM.getAbscissaUnit();
				if (ref != null && !createdUOM.equals(ref) && !referencedUOMs.contains(ref)) {
					referencedUOMs.add(ref);
				}

				ref = createdUOM.getUOM1();
				if (ref != null && !referencedUOMs.contains(ref)) {
					referencedUOMs.add(ref);
				}

				ref = createdUOM.getUOM2();
				if (ref != null && !referencedUOMs.contains(ref)) {
					referencedUOMs.add(ref);
				}

				ref = createdUOM.getBridgeAbscissaUnit();
				if (ref != null && !referencedUOMs.contains(ref)) {
					referencedUOMs.add(ref);
				}
			}

			// removed referenced UOMS from the created list since they will be saved by
			// reference
			for (UnitOfMeasure referencedUOM : referencedUOMs) {
				if (createdUOMs.contains(referencedUOM)) {
					createdUOMs.remove(referencedUOM);
				}
			}

			// build list of UOMs to save
			if (!createdUOMs.isEmpty()) {
				List<KeyedObject> toSaveUOMs = new ArrayList<>();

				for (UnitOfMeasure createdUOM : createdUOMs) {
					toSaveUOMs.add(createdUOM);
				}

				// save them all
				PersistenceService.instance().save(toSaveUOMs);
			}
		}
	}

	private void restoreWorkSchedules(ExportImportContent content) throws Exception {
		// cache all work schedules
		List<WorkSchedule> dbSchedules = PersistenceService.instance().fetchAllWorkSchedules();

		scheduleMap.clear();
		for (WorkSchedule dbSchedule : dbSchedules) {
			scheduleMap.put(dbSchedule.getName(), dbSchedule);
		}

		List<KeyedObject> toSaveSchedules = new ArrayList<>();

		// iterate over each exported schedule, skip database one if is there
		for (WorkScheduleDto dto : content.getWorkSchedules()) {
			WorkSchedule schedule = scheduleMap.get(dto.getName());

			if (schedule == null) {
				schedule = new WorkSchedule(dto);

				if (logger.isInfoEnabled()) {
					logger.info("Imported schedule: " + dto.getName());
				}

				// set referenced objects
				for (Team team : schedule.getTeams()) {
					String rotationName = team.getRotation().getName();

					for (Rotation rotation : schedule.getRotations()) {
						if (rotation.getName().equals(rotationName)) {
							team.setRotation(rotation);
							break;
						}
					}
				}

				Map<String, Shift> shiftMap = new HashMap<>();

				for (Shift shift : schedule.getShifts()) {
					shiftMap.put(shift.getName(), shift);
				}

				for (Rotation rotation : schedule.getRotations()) {
					for (RotationSegment segment : rotation.getRotationSegments()) {
						String name = segment.getStartingShift().getName();
						segment.setStartingShift(shiftMap.get(name));
					}
				}
			} // end work schedule DTO
			toSaveSchedules.add(schedule);
		}

		if (!toSaveSchedules.isEmpty()) {
			PersistenceService.instance().save(toSaveSchedules);
		}
	}

	private void restoreDataCollectors(ExportImportContent content) throws Exception {
		// cache all data collectors
		List<DataCollector> dbDataCollectors = PersistenceService.instance().fetchAllDataCollectors();

		collectorMap.clear();
		for (DataCollector dbDataCollector : dbDataCollectors) {
			collectorMap.put(dbDataCollector.getName(), dbDataCollector);
		}

		List<KeyedObject> toSaveDataCollectors = new ArrayList<>();

		// iterate over each exported data collector, skip database one if there
		for (DataCollectorDto dto : content.getDataCollectors()) {
			DataCollector collector = collectorMap.get(dto.getName());

			if (collector == null) {
				collector = new DataCollector(dto);
				toSaveDataCollectors.add(collector);

				if (logger.isInfoEnabled()) {
					logger.info("Imported collector: " + dto.getName());
				}
			}
		}

		if (!toSaveDataCollectors.isEmpty()) {
			PersistenceService.instance().save(toSaveDataCollectors);
		}
	}

	private void restoreDataSources(ExportImportContent content) throws Exception {
		List<KeyedObject> toSaveSources = new ArrayList<>();

		for (DataSourceType type : DataSourceType.values()) {
			switch (type) {
			case CRON: {
				for (CronSourceDto dto : content.getCronSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new CronEventSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported cron source: " + dto.getName());
						}
					}
				}
				break;
			}
			case DATABASE: {
				for (DatabaseSourceDto dto : content.getDatabaseSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new DatabaseEventSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported database source: " + dto.getName());
						}
					}
				}
				break;
			}
			case EMAIL: {
				for (EmailSourceDto dto : content.getEmailSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new EmailSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported email source: " + dto.getName());
						}
					}
				}
				break;
			}
			case FILE: {
				for (FileSourceDto dto : content.getFileSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new FileEventSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported file source: " + dto.getName());
						}
					}
				}
				break;
			}
			case HTTP: {
				for (HttpSourceDto dto : content.getHttpSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new HttpSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported HTTP source: " + dto.getName());
						}
					}
				}
				break;
			}
			case JMS: {
				for (JmsSourceDto dto : content.getJmsSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new JmsSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported JMS source: " + dto.getName());
						}
					}
				}
				break;
			}
			case KAFKA: {
				for (KafkaSourceDto dto : content.getKafkaSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new KafkaSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported Kafka source: " + dto.getName());
						}
					}
				}
				break;
			}
			case MODBUS: {
				for (ModbusSourceDto dto : content.getModbusSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new ModbusSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported Modbus source: " + dto.getName());
						}
					}
				}
				break;
			}
			case MQTT: {
				for (MqttSourceDto dto : content.getMqttSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new MqttSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported MQTT source: " + dto.getName());
						}
					}
				}
				break;
			}
			case OPC_DA: {
				for (OpcDaSourceDto dto : content.getOpcDaSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new OpcDaSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported OPC DA source: " + dto.getName());
						}
					}
				}
				break;
			}
			case OPC_UA: {
				for (OpcUaSourceDto dto : content.getOpcUaSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new OpcUaSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported OPC UA source: " + dto.getName());
						}
					}
				}
				break;
			}
			case PROFICY: {
				for (ProficySourceDto dto : content.getProficySources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new ProficySource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported Proficy source: " + dto.getName());
						}
					}
				}
				break;
			}
			case RMQ: {
				for (RmqSourceDto dto : content.getRmqSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new RmqSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported RMQ source: " + dto.getName());
						}
					}
				}
				break;
			}
			case WEB_SOCKET: {
				for (WebSocketSourceDto dto : content.getWebSocketSources()) {
					CollectorDataSource source = PersistenceService.instance().fetchDataSourceByName(dto.getName());

					if (source == null) {
						source = new WebSocketSource(dto);
						toSaveSources.add(source);

						if (logger.isInfoEnabled()) {
							logger.info("Imported web socket source: " + dto.getName());
						}
					}
				}
				break;
			}
			default:
				if (logger.isWarnEnabled()) {
					logger.warn("Unknown import type: " + type.name());
				}
				break;
			}
		}

		if (!toSaveSources.isEmpty()) {
			PersistenceService.instance().save(toSaveSources);
		}
	}
}
