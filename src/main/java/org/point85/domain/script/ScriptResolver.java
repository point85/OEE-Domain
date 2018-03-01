package org.point85.domain.script;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSource;
import org.point85.domain.persistence.ScriptResolverTypeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "SCRIPT_RESOLVER")
@AttributeOverride(name = "primaryKey", column = @Column(name = "SR_KEY"))

@NamedQueries({ @NamedQuery(name = ScriptResolver.RESOLVER_ALL, query = "SELECT sr FROM ScriptResolver sr"),
		@NamedQuery(name = ScriptResolver.RESOLVER_BY_COLLECTOR, query = "SELECT sr FROM ScriptResolver sr WHERE sr.collector.name IN :names"),
		@NamedQuery(name = ScriptResolver.RESOLVER_BY_HOST, query = "SELECT sr FROM ScriptResolver sr WHERE sr.collector.host IN :names AND sr.collector.state IN :states"), })

public class ScriptResolver extends KeyedObject {
	// named queries
	public static final String RESOLVER_BY_COLLECTOR = "RESOLVER.ByCollector";
	public static final String RESOLVER_BY_HOST = "RESOLVER.ByHost";
	public static final String RESOLVER_ALL = "RESOLVER.All";

	// period between value updates
	public static final int DEFAULT_UPDATE_PERIOD = 1000;

	// logger
	private transient final Logger logger = LoggerFactory.getLogger(getClass());

	// owning plant entity
	@ManyToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "SOURCE_KEY")
	private DataSource dataSource;

	@Column(name = "SOURCE_ID")
	private String sourceId;

	@Column(name = "SCRIPT")
	private String functionScript;

	@Column(name = "PERIOD")
	private Integer updatePeriod = new Integer(DEFAULT_UPDATE_PERIOD);

	@Column(name = "SR_TYPE")
	@Convert(converter = ScriptResolverTypeConverter.class)
	private ScriptResolverType type;

	@Column(name = "DATA_TYPE")
	private String dataType;

	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "COLLECT_KEY")
	private DataCollector collector;

	// last value received
	private transient Object lastValue;

	public ScriptResolver() {
		super();
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource source) {
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
		StringBuffer sb = new StringBuffer();
		sb.append("var ROLLOVER = 0;");
		sb.append('\n').append("var delta = value - lastValue;");
		sb.append('\n').append("if (value < lastValue) {");
		sb.append('\n').append("    delta += ROLLOVER;");
		sb.append('\n').append('}');
		sb.append('\n').append("return delta;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public static String getDefaultMaterialScript() {
		StringBuffer sb = new StringBuffer();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public static String getDefaultJobScript() {
		StringBuffer sb = new StringBuffer();
		sb.append("return value;");

		return ResolverFunction.functionFromBody(sb.toString());
	}

	public Integer getUpdatePeriod() {
		return this.updatePeriod;
	}

	public void setUpdatePeriod(Integer period) {
		if (period < DEFAULT_UPDATE_PERIOD) {
			logger.warn("Specified update period of " + period + " msec() is less than the default of "
					+ DEFAULT_UPDATE_PERIOD);
		}
		this.updatePeriod = period;
	}

	public ScriptResolverType getType() {
		return type;
	}

	public void setType(ScriptResolverType type) {
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

		if (other == null || !(other instanceof ScriptResolver)) {
			return false;
		}
		ScriptResolver otherResolver = (ScriptResolver) other;

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
}
