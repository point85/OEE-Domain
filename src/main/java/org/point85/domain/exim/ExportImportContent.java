package org.point85.domain.exim;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.dto.AreaDto;
import org.point85.domain.dto.CronSourceDto;
import org.point85.domain.dto.DataCollectorDto;
import org.point85.domain.dto.DatabaseSourceDto;
import org.point85.domain.dto.EmailSourceDto;
import org.point85.domain.dto.EnterpriseDto;
import org.point85.domain.dto.EquipmentDto;
import org.point85.domain.dto.FileSourceDto;
import org.point85.domain.dto.GenericSourceDto;
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

public class ExportImportContent {
	private List<MaterialDto> materials = new ArrayList<>();
	private List<ReasonDto> reasons = new ArrayList<>();
	private List<UnitOfMeasureDto> uoms = new ArrayList<>();
	private List<WorkScheduleDto> schedules = new ArrayList<>();
	private List<DataCollectorDto> dataCollectors = new ArrayList<>();

	// plant entities
	private List<EnterpriseDto> enterprises = new ArrayList<>();
	private List<SiteDto> sites = new ArrayList<>();
	private List<AreaDto> areas = new ArrayList<>();
	private List<ProductionLineDto> lines = new ArrayList<>();
	private List<WorkCellDto> cells = new ArrayList<>();
	private List<EquipmentDto> equipment = new ArrayList<>();

	// data sources
	private List<CronSourceDto> cronSources = new ArrayList<>();
	private List<HttpSourceDto> httpSources = new ArrayList<>();
	private List<DatabaseSourceDto> databaseSources = new ArrayList<>();
	private List<EmailSourceDto> emailSources = new ArrayList<>();
	private List<FileSourceDto> fileSources = new ArrayList<>();
	private List<JmsSourceDto> jmsSources = new ArrayList<>();
	private List<KafkaSourceDto> kafkaSources = new ArrayList<>();
	private List<ModbusSourceDto> modbusSources = new ArrayList<>();
	private List<MqttSourceDto> mqttSources = new ArrayList<>();
	private List<OpcDaSourceDto> opcDaSources = new ArrayList<>();
	private List<OpcUaSourceDto> opcUaSources = new ArrayList<>();
	private List<ProficySourceDto> proficySources = new ArrayList<>();
	private List<RmqSourceDto> rmqSources = new ArrayList<>();
	private List<WebSocketSourceDto> webSocketSources = new ArrayList<>();
	private List<GenericSourceDto> genericSources = new ArrayList<>();

	// create or update flag
	private Boolean forCreate;

	public ExportImportContent() {

	}

	public ExportImportContent(Boolean forCreate) {
		this.forCreate = forCreate;
	}

	public Boolean isForCreate() {
		return this.forCreate;
	}

	public void clear() {
		this.materials.clear();
		this.reasons.clear();
		this.uoms.clear();
		this.schedules.clear();
		this.enterprises.clear();
		this.sites.clear();
		this.areas.clear();
		this.lines.clear();
		this.cells.clear();
		this.equipment.clear();

		this.dataCollectors.clear();

		this.cronSources.clear();
		this.httpSources.clear();
		this.databaseSources.clear();
		this.emailSources.clear();
		this.fileSources.clear();
		this.jmsSources.clear();
		this.kafkaSources.clear();
		this.modbusSources.clear();
		this.mqttSources.clear();
		this.opcDaSources.clear();
		this.opcUaSources.clear();
		this.proficySources.clear();
		this.rmqSources.clear();
		this.webSocketSources.clear();
		this.genericSources.clear();
	}

	public List<MaterialDto> getMaterials() {
		return this.materials;
	}

	public List<ReasonDto> getReasons() {
		return this.reasons;
	}

	void setReasons(List<ReasonDto> dtos) {
		this.reasons = dtos;
	}

	public List<UnitOfMeasureDto> getUOMs() {
		return this.uoms;
	}

	public List<WorkScheduleDto> getWorkSchedules() {
		return this.schedules;
	}

	public List<EnterpriseDto> getEnterprises() {
		return this.enterprises;
	}

	void setEnterprises(List<EnterpriseDto> dtos) {
		this.enterprises = dtos;
	}

	public List<SiteDto> getSites() {
		return this.sites;
	}

	void setSites(List<SiteDto> dtos) {
		this.sites = dtos;
	}

	public List<AreaDto> getAreas() {
		return this.areas;
	}

	void setAreas(List<AreaDto> dtos) {
		this.areas = dtos;
	}

	public List<ProductionLineDto> getProductionLines() {
		return this.lines;
	}

	void setProductionLines(List<ProductionLineDto> dtos) {
		this.lines = dtos;
	}

	public List<WorkCellDto> getWorkCells() {
		return this.cells;
	}

	void setWorkCells(List<WorkCellDto> dtos) {
		this.cells = dtos;
	}

	public List<EquipmentDto> getEquipment() {
		return this.equipment;
	}

	void setEquipment(List<EquipmentDto> dtos) {
		this.equipment = dtos;
	}

	List<HttpSourceDto> getHttpSources() {
		return this.httpSources;
	}

	List<CronSourceDto> getCronSources() {
		return this.cronSources;
	}

	List<DatabaseSourceDto> getDatabaseSources() {
		return databaseSources;
	}

	List<EmailSourceDto> getEmailSources() {
		return emailSources;
	}

	List<FileSourceDto> getFileSources() {
		return fileSources;
	}

	List<JmsSourceDto> getJmsSources() {
		return jmsSources;
	}

	List<KafkaSourceDto> getKafkaSources() {
		return kafkaSources;
	}

	List<ModbusSourceDto> getModbusSources() {
		return modbusSources;
	}

	List<MqttSourceDto> getMqttSources() {
		return mqttSources;
	}

	List<OpcDaSourceDto> getOpcDaSources() {
		return opcDaSources;
	}

	List<OpcUaSourceDto> getOpcUaSources() {
		return opcUaSources;
	}

	List<ProficySourceDto> getProficySources() {
		return proficySources;
	}

	List<RmqSourceDto> getRmqSources() {
		return rmqSources;
	}

	List<WebSocketSourceDto> getWebSocketSources() {
		return webSocketSources;
	}

	List<GenericSourceDto> getGenericSources() {
		return genericSources;
	}

	List<DataCollectorDto> getDataCollectors() {
		return dataCollectors;
	}
}
