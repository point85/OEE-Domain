package org.point85.domain.script;

import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.persistence.EventTypeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;

@Entity
@Table(name = "EVENT_RESOLVER")
@AttributeOverride(name = "primaryKey", column = @Column(name = "ER_KEY"))

public class EventResolver extends KeyedObject {
	// period between value updates
	public static final int DEFAULT_UPDATE_PERIOD = 5000;

	// owning plant entity
	@ManyToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
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

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "COLLECT_KEY")
	private DataCollector collector;

	// last value received
	private transient Object lastValue;

	// time last received
	private transient OffsetDateTime lastTimestamp;
	
	// mode
	private transient boolean watchMode = false;

	public EventResolver() {
		super();
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

	// get owning entity
	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment owner) {
		this.equipment = owner;
	}

	public static String getPassthroughScript() {
		String body = "return value;";
		return ResolverFunction.functionFromBody(body);
	}

	public static String getDefaultProductionScript() {
		StringBuilder sb = new StringBuilder();
		sb.append("var ROLLOVER = 0;");
		sb.append('\n').append("var delta = value - lastValue;");
		sb.append('\n').append("if (value < lastValue) {");
		sb.append('\n').append("    delta += ROLLOVER;");
		sb.append('\n').append('}');
		sb.append('\n').append("return delta;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public static String getDefaultMaterialScript() {
		StringBuilder sb = new StringBuilder();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public static String getDefaultJobScript() {
		StringBuilder sb = new StringBuilder();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public Integer getUpdatePeriod() {
		return this.updatePeriod;
	}

	public void setUpdatePeriod(Integer period) throws Exception {
		if (period < 0) {
			throw new Exception(
					"The specified update period of " + period + " msec() must be greater than or equal to zero");
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

		if (other == null || !(other instanceof EventResolver)) {
			return false;
		}
		EventResolver otherResolver = (EventResolver) other;

		if (getSourceId() == null) {
			return true;
		}

		if (!getSourceId().equals(otherResolver.getSourceId())) {
			return false;
		}

		return true;
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
		return "Data source: " + dataSource + ", source id: " + sourceId;
	}

	public OffsetDateTime getLastTimestamp() {
		return lastTimestamp;
	}

	public void setLastTimestamp(OffsetDateTime lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}

	public boolean isWatchMode() {
		return watchMode;
	}

	public void setWatchMode(boolean watchMode) {
		this.watchMode = watchMode;
	}
}
