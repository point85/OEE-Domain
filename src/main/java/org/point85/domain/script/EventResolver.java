package org.point85.domain.script;

import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.dto.EventResolverDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.EventTypeConverter;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.PlantEntity;

@Entity
@Table(name = "EVENT_RESOLVER")
@AttributeOverride(name = "primaryKey", column = @Column(name = "ER_KEY"))

/**
 * This class executes a JavaScript script to resolve an event
 *
 */
public class EventResolver extends KeyedObject {
	// owning plant entity
	@ManyToOne
	@JoinColumn(name = "ENT_KEY")
	private PlantEntity entity;

	@OneToOne
	@JoinColumn(name = "SOURCE_KEY")
	private CollectorDataSource dataSource;

	@Column(name = "SOURCE_ID")
	private String sourceId;

	@Column(name = "SCRIPT")
	private String functionScript;

	@Column(name = "PERIOD")
	private Integer updatePeriod;

	@Column(name = "ER_TYPE")
	@Convert(converter = EventTypeConverter.class)
	private OeeEventType type;

	@Column(name = "DATA_TYPE")
	private String dataType;

	@OneToOne
	@JoinColumn(name = "COLLECT_KEY")
	private DataCollector collector;

	// last value received
	@Transient
	private Object lastValue;

	// time last received
	@Transient
	private OffsetDateTime timestamp;

	// Reason name can be set in script
	@Transient
	private String reason;

	// mode
	@Transient
	private boolean watchMode = false;

	public EventResolver() {
		super();
	}

	public EventResolver(EventResolverDto dto) throws Exception {

		if (dto.getCollector() != null) {
			DataCollector dbCollector = PersistenceService.instance().fetchCollectorByName(dto.getCollector());

			if (dbCollector == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.data.collector", dto.getCollector()));
			}
			this.collector = dbCollector;
		}

		if (dto.getDataSource() != null) {
			CollectorDataSource dbSource = PersistenceService.instance().fetchDataSourceByName(dto.getDataSource());

			if (dbSource == null) {
				throw new Exception(
						DomainLocalizer.instance().getErrorString("no.collector.source", dto.getDataSource()));
			}
			this.dataSource = dbSource;
		}

		this.dataType = dto.getDataType();
		this.functionScript = dto.getScript();
		this.sourceId = dto.getSourceId();
		this.updatePeriod = dto.getUpdatePeriod();
		this.type = dto.getType() != null ? OeeEventType.valueOf(dto.getType()) : null;
	}

	public CollectorDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(CollectorDataSource source) {
		this.dataSource = source;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getScript() {
		return functionScript;
	}

	public void setScript(String script) {
		this.functionScript = script;
	}

	public Equipment getEquipment() {
		return (entity instanceof Equipment) ? (Equipment)entity : null;
	}
	
	public PlantEntity getPlantEntity() {
		return entity;
	}

	public void setPlantEntity(PlantEntity owner) {
		this.entity = owner;
	}

	/**
	 * Create the default pass-through function for an availability event
	 * 
	 * @return JavaScript function
	 */
	public static String createDefaultAvailabilityFunction() {
		String body = "return value;";
		return ResolverFunction.functionFromBody(body);
	}

	/**
	 * Create the default function for a production count event
	 * 
	 * @return JavaScript function
	 */
	public static String createDefaultProductionFunction() {
		String body = "return value;";
		return ResolverFunction.functionFromBody(body);
	}

	/**
	 * Create the default pass-through function for a material change event
	 * 
	 * @return JavaScript function
	 */
	public static String createDefaultMaterialFunction() {
		StringBuilder sb = new StringBuilder();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	/**
	 * Create the default pass-through function for a job change event
	 * 
	 * @return JavaScript function
	 */
	public static String createDefaultJobFunction() {
		StringBuilder sb = new StringBuilder();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	/**
	 * Create the default function for an event
	 * 
	 * @return JavaScript function
	 */
	public static String createDefaultFunction() {
		StringBuilder sb = new StringBuilder();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public Integer getUpdatePeriod() {
		return this.updatePeriod;
	}

	public void setUpdatePeriod(Integer period) throws Exception {
		if (period < 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("invalid.period", period));
		}
		this.updatePeriod = period;
	}

	public OeeEventType getType() {
		return type;
	}

	public void setType(OeeEventType type) {
		this.type = type;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getEquipment(), getSourceId());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof EventResolver)) {
			return false;
		}
		EventResolver otherResolver = (EventResolver) other;

		if (getSourceId() == null) {
			return true;
		}

		return getSourceId().equals(otherResolver.getSourceId());
	}

	public DataCollector getCollector() {
		return collector;
	}

	public void setCollector(DataCollector collector) {
		this.collector = collector;
	}

	public Object getLastValue() {
		return lastValue;
	}

	public void setLastValue(Object lastValue) {
		this.lastValue = lastValue;
	}

	@Override
	public String toString() {
		return "Data source: " + dataSource + ", source id: " + sourceId + ", type: " + type;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public void setIsoTimestamp(String timestamp) {
		this.timestamp = DomainUtils.offsetDateTimeFromString(timestamp, DomainUtils.OFFSET_DATE_TIME_8601);
	}

	public boolean isWatchMode() {
		return watchMode;
	}

	public void setWatchMode(boolean watchMode) {
		this.watchMode = watchMode;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
