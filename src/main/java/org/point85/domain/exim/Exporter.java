package org.point85.domain.exim;

import java.io.File;
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
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.socket.WebSocketSource;
import org.point85.domain.uom.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton service to backup design-time objects to a file that can be
 * restored to another database.
 *
 */
public class Exporter extends BaseExportImport {
	private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

	// singleton
	private static Exporter exporter;

	private Exporter() {
		// Singleton
	}

	/**
	 * Obtain an Exporter instance
	 * 
	 * @return {@link Exporter}
	 */
	public static synchronized Exporter instance() {
		if (exporter == null) {
			exporter = new Exporter();
		}
		return exporter;
	}

	/**
	 * Backup the exported content to a file
	 * 
	 * @param file File to write to
	 * @throws Exception Exception
	 */
	public synchronized void backup(File file) throws Exception {
		// backup content
		String json = gson.toJson(content);

		if (logger.isTraceEnabled()) {
			logger.trace(json);
		}

		DomainUtils.gzip(json, file.getCanonicalPath());

		content.clear();
	}

	/**
	 * Backup the exported design-time class objects to a file
	 * 
	 * @param clazz Class to backup
	 * @param file  File to write to
	 * @throws Exception Exception
	 * @return {@link ExportImportContent}
	 */
	public synchronized ExportImportContent backup(Class<?> clazz, File file) throws Exception {
		if (clazz.equals(Material.class)) {
			prepareMaterials(PersistenceService.instance().fetchAllMaterials());
		} else if (clazz.equals(Reason.class)) {
			prepareReasons(PersistenceService.instance().fetchAllReasons());
		} else if (clazz.equals(UnitOfMeasure.class)) {
			prepareUnitsOfMeasure(PersistenceService.instance().fetchAllUnitsOfMeasures());
		} else if (clazz.equals(WorkSchedule.class)) {
			prepareWorkSchedules(PersistenceService.instance().fetchAllWorkSchedules());
		} else if (clazz.equals(PlantEntity.class)) {
			preparePlantEntities(PersistenceService.instance().fetchAllPlantEntities());
		} else if (clazz.equals(DataCollector.class)) {
			prepareDataCollectors(PersistenceService.instance().fetchAllDataCollectors());
		} else if (clazz.equals(CronEventSource.class)) {
			prepareCronDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.CRON));
		} else if (clazz.equals(DatabaseEventSource.class)) {
			prepareDatabaseDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.DATABASE));
		} else if (clazz.equals(EmailSource.class)) {
			prepareEmailDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.EMAIL));
		} else if (clazz.equals(FileEventSource.class)) {
			prepareFileDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.FILE));
		} else if (clazz.equals(HttpSource.class)) {
			prepareHttpDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.HTTP));
		} else if (clazz.equals(JmsSource.class)) {
			prepareJmsDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.JMS));
		} else if (clazz.equals(KafkaSource.class)) {
			prepareKafkaDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.KAFKA));
		} else if (clazz.equals(ModbusSource.class)) {
			prepareModbusDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.MODBUS));
		} else if (clazz.equals(MqttSource.class)) {
			prepareMqttDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.MQTT));
		} else if (clazz.equals(OpcDaSource.class)) {
			prepareOpcDaDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.OPC_DA));
		} else if (clazz.equals(OpcUaSource.class)) {
			prepareOpcUaDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.OPC_UA));
		} else if (clazz.equals(ProficySource.class)) {
			prepareProficyDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.PROFICY));
		} else if (clazz.equals(RmqSource.class)) {
			prepareRmqDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.RMQ));
		} else if (clazz.equals(WebSocketSource.class)) {
			prepareWebSocketDataSources(PersistenceService.instance().fetchDataSources(DataSourceType.WEB_SOCKET));
		} else {
			logger.error("Unknown backup class: " + clazz.getSimpleName());
		}

		if (file != null) {
			backup(file);
		}

		return content;
	}

	public synchronized ExportImportContent backupReasons(List<Reason> reasons, File file) throws Exception {
		prepareReasons(reasons);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupUOMs(List<UnitOfMeasure> uoms, File file) throws Exception {
		prepareUnitsOfMeasure(uoms);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupMaterials(List<Material> materials, File file) throws Exception {
		prepareMaterials(materials);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupSchedules(List<WorkSchedule> schedules, File file) throws Exception {
		prepareWorkSchedules(schedules);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupEntities(List<PlantEntity> entities, File file) throws Exception {
		preparePlantEntities(entities);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupCollectors(List<DataCollector> collectors, File file)
			throws Exception {
		prepareDataCollectors(collectors);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupOpcUaSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareOpcUaDataSources(sources);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupHttpSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareHttpDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupCronSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareCronDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupDatabaseSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareDatabaseDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupEmailSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareEmailDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupFileSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareFileDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupJmsSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareJmsDataSources(sources);
		backup(file);
		return content;
	}

	public synchronized ExportImportContent backupKafkaSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareKafkaDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupModbusSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareModbusDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupMqttSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareMqttDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupOpcDaSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareOpcDaDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupProficySources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareProficyDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupRmqSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareRmqDataSources(sources);
		backup(file);
		return content;
	}
	
	public synchronized ExportImportContent backupWebSocketSources(List<CollectorDataSource> sources, File file)
			throws Exception {
		prepareWebSocketDataSources(sources);
		backup(file);
		return content;
	}
	
	/**
	 * Serialize the objects of this class prior to writing them to a file
	 * 
	 * @param clazz Class to backup
	 * @throws Exception Exception
	 * @return {@link ExportImportContent}
	 */
	public synchronized ExportImportContent prepare(Class<?> clazz) throws Exception {
		return backup(clazz, null);
	}

	private void prepareWorkSchedules(List<WorkSchedule> schedules) {
		content.getWorkSchedules().clear();

		for (WorkSchedule schedule : schedules) {
			content.getWorkSchedules().add(new WorkScheduleDto(schedule));
		}
	}

	private void prepareMaterials(List<Material> materials) {
		content.getMaterials().clear();

		for (Material material : materials) {
			content.getMaterials().add(new MaterialDto(material));
		}
	}

	private void prepareReasons(List<Reason> reasons) {
		content.getReasons().clear();

		Map<String, ReasonDto> reasonMap = new HashMap<>();

		List<ReasonDto> topDtos = new ArrayList<>();

		// put in map for export
		for (Reason reason : reasons) {
			String lossName = reason.getLossCategory() != null ? reason.getLossCategory().name() : null;
			ReasonDto reasonDto = new ReasonDto(reason.getName(), reason.getDescription(), lossName);

			// parent entity
			if (reason.getParent() == null) {
				topDtos.add(reasonDto);
			}
			reasonMap.put(reasonDto.getName(), reasonDto);
		}

		// set children
		for (Reason reason : reasons) {
			ReasonDto parentDto = reasonMap.get(reason.getName());

			for (Reason childReason : reason.getChildren()) {
				ReasonDto childDto = reasonMap.get(childReason.getName());
				childDto.setParent(parentDto.getName());
				parentDto.getChildren().add(childDto);
			}
		}

		// JSON payload is top-level DTOs
		content.setReasons(topDtos);
	}

	private void prepareUnitsOfMeasure(List<UnitOfMeasure> uoms) {
		for (UnitOfMeasure uom : uoms) {
			content.getUOMs().add(new UnitOfMeasureDto(uom));
		}
	}

	private void prepareDataCollectors(List<DataCollector> collectors) {
		for (DataCollector collector : collectors) {
			content.getDataCollectors().add(new DataCollectorDto(collector));
		}
	}

	private void addSites(Enterprise enterprise, EnterpriseDto dto) {
		for (PlantEntity childEntity : enterprise.getChildren()) {
			SiteDto siteDto = new SiteDto((Site) childEntity);

			siteDto.setParent(enterprise.getName());
			dto.getSites().add(siteDto);

			addAreas((Site) childEntity, siteDto);
		}
	}

	private void addAreas(Site site, SiteDto dto) {
		for (PlantEntity childEntity : site.getChildren()) {
			AreaDto areaDto = new AreaDto((Area) childEntity);

			areaDto.setParent(site.getName());
			dto.getAreas().add(areaDto);

			addProductionLines((Area) childEntity, areaDto);
		}
	}

	private void addProductionLines(Area area, AreaDto dto) {
		for (PlantEntity childEntity : area.getChildren()) {
			ProductionLineDto lineDto = new ProductionLineDto((ProductionLine) childEntity);

			lineDto.setParent(dto.getName());
			dto.getProductionLines().add(lineDto);

			addWorkCells((ProductionLine) childEntity, lineDto);
		}
	}

	private void addWorkCells(ProductionLine line, ProductionLineDto dto) {
		for (PlantEntity childEntity : line.getChildren()) {
			WorkCellDto cellDto = new WorkCellDto((WorkCell) childEntity);

			cellDto.setParent(dto.getName());
			dto.getWorkCells().add(cellDto);

			addEquipment((WorkCell) childEntity, cellDto);
		}
	}

	private void addEquipment(WorkCell cell, WorkCellDto dto) {
		for (PlantEntity childEntity : cell.getChildren()) {
			EquipmentDto childDto = new EquipmentDto((Equipment) childEntity);

			childDto.setParent(dto.getName());
			dto.getEquipment().add(childDto);
		}
	}

	private void preparePlantEntities(List<PlantEntity> entities) {
		content.getEnterprises().clear();
		content.getSites().clear();
		content.getAreas().clear();
		content.getProductionLines().clear();
		content.getWorkCells().clear();
		content.getEquipment().clear();

		Map<String, EquipmentDto> equipmentMap = new HashMap<>();
		Map<String, WorkCellDto> cellMap = new HashMap<>();
		Map<String, ProductionLineDto> lineMap = new HashMap<>();
		Map<String, AreaDto> areaMap = new HashMap<>();
		Map<String, SiteDto> siteMap = new HashMap<>();

		List<EnterpriseDto> topEnterprises = new ArrayList<>();
		List<SiteDto> topSites = new ArrayList<>();
		List<AreaDto> topAreas = new ArrayList<>();
		List<ProductionLineDto> topLines = new ArrayList<>();
		List<WorkCellDto> topCells = new ArrayList<>();
		List<EquipmentDto> topEquipment = new ArrayList<>();

		// create top-level DTOs
		for (PlantEntity entity : entities) {
			if (entity instanceof Enterprise) {
				EnterpriseDto dto = new EnterpriseDto((Enterprise) entity);

				if (entity.getParent() == null) {
					topEnterprises.add(dto);
				}

				// traverse hierarchy
				addSites((Enterprise) entity, dto);
			} else if (entity instanceof Site) {
				SiteDto dto = siteMap.get(entity.getName());

				if (dto == null) {
					dto = new SiteDto((Site) entity);
					siteMap.put(dto.getName(), dto);
				}

				if (entity.getParent() == null) {
					topSites.add(dto);
				}

				// traverse hierarchy
				addAreas((Site) entity, dto);
			} else if (entity instanceof Area) {
				AreaDto dto = areaMap.get(entity.getName());

				if (dto == null) {
					dto = new AreaDto((Area) entity);
					areaMap.put(dto.getName(), dto);
				}

				if (entity.getParent() == null) {
					topAreas.add(dto);
				}

				// traverse hierarchy
				addProductionLines((Area) entity, dto);
			} else if (entity instanceof ProductionLine) {
				ProductionLineDto dto = lineMap.get(entity.getName());

				if (dto == null) {
					dto = new ProductionLineDto((ProductionLine) entity);
					lineMap.put(dto.getName(), dto);
				}

				if (entity.getParent() == null) {
					topLines.add(dto);
				}

				// traverse hierarchy
				addWorkCells((ProductionLine) entity, dto);

				for (PlantEntity childEntity : entity.getChildren()) {
					WorkCellDto childDto = new WorkCellDto((WorkCell) childEntity);
					cellMap.put(childDto.getName(), childDto);

					childDto.setParent(dto.getName());
					dto.getWorkCells().add(childDto);
				}
			} else if (entity instanceof WorkCell) {
				WorkCellDto dto = cellMap.get(entity.getName());

				if (dto == null) {
					dto = new WorkCellDto((WorkCell) entity);
					cellMap.put(dto.getName(), dto);
				}

				if (entity.getParent() == null) {
					topCells.add(dto);
				}

				// traverse hierarchy
				addEquipment((WorkCell) entity, dto);
			} else if (entity instanceof Equipment) {
				EquipmentDto dto = equipmentMap.get(entity.getName());

				if (dto == null) {
					dto = new EquipmentDto((Equipment) entity);
					equipmentMap.put(dto.getName(), dto);
				}

				if (entity.getParent() == null) {
					topEquipment.add(dto);
				}
			}
		}

		// JSON payload is top-level DTOs
		content.setEnterprises(topEnterprises);
		content.setSites(topSites);
		content.setAreas(topAreas);
		content.setProductionLines(topLines);
		content.setWorkCells(topCells);
		content.setEquipment(topEquipment);
	}

	private void prepareCronDataSources(List<CollectorDataSource> sources) {
		content.getCronSources().clear();

		for (CollectorDataSource source : sources) {
			CronSourceDto dto = new CronSourceDto((CronEventSource) source);
			content.getCronSources().add(dto);
		}
	}

	private void prepareDatabaseDataSources(List<CollectorDataSource> sources) {
		content.getDatabaseSources().clear();

		for (CollectorDataSource source : sources) {
			DatabaseSourceDto dto = new DatabaseSourceDto((DatabaseEventSource) source);
			content.getDatabaseSources().add(dto);
		}
	}

	private void prepareEmailDataSources(List<CollectorDataSource> sources) {
		content.getEmailSources().clear();

		for (CollectorDataSource source : sources) {
			EmailSourceDto dto = new EmailSourceDto((EmailSource) source);
			content.getEmailSources().add(dto);
		}
	}

	private void prepareFileDataSources(List<CollectorDataSource> sources) {
		content.getFileSources().clear();

		for (CollectorDataSource source : sources) {
			FileSourceDto dto = new FileSourceDto((FileEventSource) source);
			content.getFileSources().add(dto);
		}
	}

	private void prepareHttpDataSources(List<CollectorDataSource> sources) {
		content.getHttpSources().clear();

		for (CollectorDataSource source : sources) {
			HttpSourceDto dto = new HttpSourceDto((HttpSource) source);
			content.getHttpSources().add(dto);
		}
	}

	private void prepareJmsDataSources(List<CollectorDataSource> sources) {
		content.getJmsSources().clear();

		for (CollectorDataSource source : sources) {
			JmsSourceDto dto = new JmsSourceDto((JmsSource) source);
			content.getJmsSources().add(dto);
		}
	}

	private void prepareKafkaDataSources(List<CollectorDataSource> sources) {
		content.getKafkaSources().clear();

		for (CollectorDataSource source : sources) {
			KafkaSourceDto dto = new KafkaSourceDto((KafkaSource) source);
			content.getKafkaSources().add(dto);
		}
	}

	private void prepareModbusDataSources(List<CollectorDataSource> sources) {
		content.getModbusSources().clear();

		for (CollectorDataSource source : sources) {
			ModbusSourceDto dto = new ModbusSourceDto((ModbusSource) source);
			content.getModbusSources().add(dto);
		}
	}

	private void prepareMqttDataSources(List<CollectorDataSource> sources) {
		content.getMqttSources().clear();

		for (CollectorDataSource source : sources) {
			MqttSourceDto dto = new MqttSourceDto((MqttSource) source);
			content.getMqttSources().add(dto);
		}
	}

	private void prepareOpcDaDataSources(List<CollectorDataSource> sources) {
		content.getOpcDaSources().clear();

		for (CollectorDataSource source : sources) {
			OpcDaSourceDto dto = new OpcDaSourceDto((OpcDaSource) source);
			content.getOpcDaSources().add(dto);
		}
	}

	private void prepareOpcUaDataSources(List<CollectorDataSource> sources) {
		content.getOpcUaSources().clear();

		for (CollectorDataSource source : sources) {
			OpcUaSourceDto dto = new OpcUaSourceDto((OpcUaSource) source);
			content.getOpcUaSources().add(dto);
		}
	}

	private void prepareProficyDataSources(List<CollectorDataSource> sources) {
		content.getProficySources().clear();

		for (CollectorDataSource source : sources) {
			ProficySourceDto dto = new ProficySourceDto((ProficySource) source);
			content.getProficySources().add(dto);
		}
	}

	private void prepareRmqDataSources(List<CollectorDataSource> sources) {
		content.getRmqSources().clear();

		for (CollectorDataSource source : sources) {
			RmqSourceDto dto = new RmqSourceDto((RmqSource) source);
			content.getRmqSources().add(dto);
		}
	}

	private void prepareWebSocketDataSources(List<CollectorDataSource> sources) {
		content.getWebSocketSources().clear();

		for (CollectorDataSource source : sources) {
			WebSocketSourceDto dto = new WebSocketSourceDto((WebSocketSource) source);
			content.getWebSocketSources().add(dto);
		}
	}
}
