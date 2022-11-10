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

		ExportContent content = gson.fromJson(json, ExportContent.class);

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

	private void restoreMaterials(ExportContent content) throws Exception {
		// cache all materials
		List<Material> dbMaterials = PersistenceService.instance().fetchAllMaterials();

		Map<String, Material> dbMap = new HashMap<>();
		for (Material dbMaterial : dbMaterials) {
			dbMap.put(dbMaterial.getName(), dbMaterial);
		}

		List<KeyedObject> toSaveMaterials = new ArrayList<>();

		// iterate over each exported material, skip database one if there
		for (MaterialDto dto : content.getMaterials()) {
			Material material = dbMap.get(dto.getName());

			if (material == null) {
				material = new Material(dto);
				toSaveMaterials.add(material);

				if (logger.isInfoEnabled()) {
					logger.info("Imported material: " + dto.getName());
				}
			}
		}

		if (!toSaveMaterials.isEmpty()) {
			PersistenceService.instance().save(toSaveMaterials);
		}
	}

	private void addChildReasons(Reason parent, List<ReasonDto> childDtos, Map<String, Reason> dbMap) throws Exception {
		// iterate over each child
		for (ReasonDto childDto : childDtos) {
			Reason childReason = dbMap.get(childDto.getName());

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
			addChildReasons(childReason, childDto.getChildren(), dbMap);
		}
	}

	private void restoreReasons(ExportContent content) throws Exception {
		// cache all reasons
		List<Reason> dbReasons = PersistenceService.instance().fetchAllReasons();

		Map<String, Reason> dbMap = new HashMap<>();
		for (Reason dbReason : dbReasons) {
			dbMap.put(dbReason.getName(), dbReason);
		}

		List<KeyedObject> toSaveReasons = new ArrayList<>();

		// top-level reasons
		for (ReasonDto dto : content.getReasons()) {

			Reason parentReason = dbMap.get(dto.getName());

			if (parentReason == null) {
				parentReason = new Reason(dto);
				toSaveReasons.add(parentReason);

				if (logger.isInfoEnabled()) {
					logger.info("Imported reason: " + dto.getName());
				}
			}

			// children
			if (!dto.getChildren().isEmpty()) {
				addChildReasons(parentReason, dto.getChildren(), dbMap);
			}
		}

		PersistenceService.instance().save(toSaveReasons);
	}

	private void restorePlantEntities(ExportContent content) throws Exception {
		// cache all entities
		List<PlantEntity> dbPlantEntities = PersistenceService.instance().fetchAllPlantEntities();

		Map<String, PlantEntity> dbMap = new HashMap<>();
		for (PlantEntity dbPlantEntity : dbPlantEntities) {
			dbMap.put(dbPlantEntity.getName(), dbPlantEntity);
		}

		List<KeyedObject> toSavePlantEntities = new ArrayList<>();

		// top-level entities
		for (EnterpriseDto dto : content.getEnterprises()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			Enterprise entity = new Enterprise(dto);
			addSites(entity, dto.getSites());

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported enterprise: " + dto.getName());
			}
		}

		for (SiteDto dto : content.getSites()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			Site entity = new Site(dto);
			addAreas(entity, dto.getAreas());

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported site: " + dto.getName());
			}
		}

		for (AreaDto dto : content.getAreas()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			Area entity = new Area(dto);
			addProductionLines(entity, dto.getProductionLines());

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported area: " + dto.getName());
			}
		}

		for (ProductionLineDto dto : content.getProductionLines()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			ProductionLine entity = new ProductionLine(dto);
			addWorkCells(entity, dto.getWorkCells());

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported line: " + dto.getName());
			}
		}

		for (WorkCellDto dto : content.getWorkCells()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			WorkCell entity = new WorkCell(dto);
			addEquipment(entity, dto.getEquipment());

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported cell: " + dto.getName());
			}
		}

		// equipment
		for (EquipmentDto dto : content.getEquipment()) {
			if (dbMap.get(dto.getName()) != null) {
				continue;
			}

			Equipment entity = new Equipment(dto);

			toSavePlantEntities.add(entity);

			if (logger.isInfoEnabled()) {
				logger.info("Imported equipment: " + dto.getName());
			}
		}

		PersistenceService.instance().save(toSavePlantEntities);
	}

	public void addSites(Enterprise parent, List<SiteDto> childDtos) throws Exception {

		for (SiteDto childDto : childDtos) {
			// create entity
			Site childPlantEntity = new Site(childDto);

			// add child
			parent.getChildren().add(childPlantEntity);

			// set parent
			childPlantEntity.setParent(parent);

			if (logger.isInfoEnabled()) {
				logger.info("Imported site: " + childDto.getName());
			}

			// recurse
			addAreas(childPlantEntity, childDto.getAreas());
		}
	}

	public void addAreas(Site parent, List<AreaDto> childDtos) throws Exception {

		for (AreaDto childDto : childDtos) {
			// create entity
			Area childPlantEntity = new Area(childDto);

			// add child
			parent.getChildren().add(childPlantEntity);

			// set parent
			childPlantEntity.setParent(parent);

			if (logger.isInfoEnabled()) {
				logger.info("Imported area: " + childDto.getName());
			}

			// recurse
			addProductionLines(childPlantEntity, childDto.getProductionLines());
		}
	}

	public void addProductionLines(Area parent, List<ProductionLineDto> childDtos) throws Exception {

		for (ProductionLineDto childDto : childDtos) {
			// create entity
			ProductionLine childPlantEntity = new ProductionLine(childDto);

			// add child
			parent.getChildren().add(childPlantEntity);

			// set parent
			childPlantEntity.setParent(parent);

			if (logger.isInfoEnabled()) {
				logger.info("Imported line: " + childDto.getName());
			}

			// recurse
			addWorkCells(childPlantEntity, childDto.getWorkCells());
		}
	}

	public void addWorkCells(ProductionLine parent, List<WorkCellDto> childDtos) throws Exception {
		// iterate over each child
		for (WorkCellDto childDto : childDtos) {
			// create entity
			WorkCell childPlantEntity = new WorkCell(childDto);

			// add child
			parent.getChildren().add(childPlantEntity);

			// set parent
			childPlantEntity.setParent(parent);

			if (logger.isInfoEnabled()) {
				logger.info("Imported work cell: " + childDto.getName());
			}

			// recurse
			addEquipment(childPlantEntity, childDto.getEquipment());
		}
	}

	public void addEquipment(WorkCell parent, List<EquipmentDto> childDtos) throws Exception {
		// iterate over each child
		for (EquipmentDto childDto : childDtos) {
			// create entity
			Equipment childPlantEntity = new Equipment(childDto);

			// add child
			parent.getChildren().add(childPlantEntity);

			// set parent
			childPlantEntity.setParent(parent);

			if (logger.isInfoEnabled()) {
				logger.info("Imported equipment: " + childDto.getName());
			}
		}
	}

	private void restoreUOMs(ExportContent content) throws Exception {
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

	private void restoreWorkSchedules(ExportContent content) throws Exception {
		// cache all work schedules
		List<WorkSchedule> dbSchedules = PersistenceService.instance().fetchAllWorkSchedules();

		Map<String, WorkSchedule> dbMap = new HashMap<>();
		for (WorkSchedule dbSchedule : dbSchedules) {
			dbMap.put(dbSchedule.getName(), dbSchedule);
		}

		List<KeyedObject> toSaveSchedules = new ArrayList<>();

		// iterate over each exported schedule, skip database one if is there
		for (WorkScheduleDto dto : content.getWorkSchedules()) {
			WorkSchedule schedule = dbMap.get(dto.getName());

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

	private void restoreDataCollectors(ExportContent content) throws Exception {
		// cache all data collectors
		List<DataCollector> dbDataCollectors = PersistenceService.instance().fetchAllDataCollectors();

		Map<String, DataCollector> dbMap = new HashMap<>();
		for (DataCollector dbDataCollector : dbDataCollectors) {
			dbMap.put(dbDataCollector.getName(), dbDataCollector);
		}

		List<KeyedObject> toSaveDataCollectors = new ArrayList<>();

		// iterate over each exported data collector, skip database one if there
		for (DataCollectorDto dto : content.getDataCollectors()) {
			DataCollector collector = dbMap.get(dto.getName());

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

	private void restoreDataSources(ExportContent content) throws Exception {
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
