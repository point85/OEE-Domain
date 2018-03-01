package org.point85.domain.collector;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.point85.domain.persistence.CollectorStateConverter;
import org.point85.domain.plant.NamedObject;

@Entity
@Table(name = "COLLECTOR")
@AttributeOverride(name = "primaryKey", column = @Column(name = "COLLECT_KEY"))

@NamedQueries({
		@NamedQuery(name = DataCollector.COLLECT_BY_NAME, query = "SELECT collector FROM DataCollector collector WHERE collector.name = :name"),
		@NamedQuery(name = DataCollector.COLLECT_ALL, query = "SELECT collector FROM DataCollector collector"),
		@NamedQuery(name = DataCollector.COLLECTOR_BY_HOST_BY_STATE, query = "SELECT collector FROM DataCollector collector WHERE collector.host IN :names AND collector.state IN :states"),
		@NamedQuery(name = DataCollector.COLLECTOR_BY_STATE, query = "SELECT collector FROM DataCollector collector WHERE collector.state IN :states"), })

public class DataCollector extends NamedObject {

	// named queries
	public static final String COLLECT_ALL = "COLLECT.All";
	public static final String COLLECT_BY_NAME = "COLLECT.ByName";
	public static final String COLLECTOR_BY_STATE = "COLLECT.ByState";
	public static final String COLLECTOR_BY_HOST_BY_STATE = "COLLECT.ByStateByHost";

	@Column(name = "HOST")
	private String host;

	@Column(name = "STATE")
	@Convert(converter = CollectorStateConverter.class)
	private CollectorState state = CollectorState.DEV;

	@Column(name = "BROKER_HOST")
	private String brokerHost;

	@Column(name = "BROKER_PORT")
	private Integer brokerPort;

	public DataCollector() {

	}

	public DataCollector(String name, String description) {
		super(name, description);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return getName();
	}

	public CollectorState getCollectorState() {
		return state;
	}

	public void setCollectorState(CollectorState collectorState) {
		this.state = collectorState;
	}

	public String getBrokerHost() {
		return brokerHost;
	}

	public void setBrokerHost(String brokerHost) {
		this.brokerHost = brokerHost;
	}

	public Integer getBrokerPort() {
		return brokerPort;
	}

	public void setBrokerPort(Integer brokerPort) {
		this.brokerPort = brokerPort;
	}
}
