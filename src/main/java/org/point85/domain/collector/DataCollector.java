package org.point85.domain.collector;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.CollectorStateConverter;
import org.point85.domain.persistence.DataSourceConverter;
import org.point85.domain.plant.NamedObject;

@Entity
@Table(name = "COLLECTOR")
@AttributeOverride(name = "primaryKey", column = @Column(name = "COLLECT_KEY"))

public class DataCollector extends NamedObject {
	// machine running on
	@Column(name = "HOST")
	private String host;

	// development state
	@Column(name = "STATE")
	@Convert(converter = CollectorStateConverter.class)
	private CollectorState state = CollectorState.DEV;

	// notification server type
	@Column(name = "BROKER_TYPE")
	@Convert(converter = DataSourceConverter.class)
	private DataSourceType brokerType;

	// notification broker machine
	@Column(name = "BROKER_HOST")
	private String brokerHost;

	@Column(name = "BROKER_PORT")
	private Integer brokerPort;

	// broker credentials
	@Column(name = "BROKER_USER")
	private String brokerUserName;

	@Column(name = "BROKER_PWD")
	private String brokerUserPassword;

	public DataCollector() {
		super();
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

	public String getBrokerUserName() {
		return brokerUserName;
	}

	public void setBrokerUserName(String userName) {
		this.brokerUserName = userName;
	}

	public String getBrokerUserPassword() {
		return DomainUtils.decode(brokerUserPassword);
	}

	public void setBrokerUserPassword(String password) {
		this.brokerUserPassword = DomainUtils.encode(password);
	}

	public DataSourceType getBrokerType() {
		return brokerType;
	}

	public void setBrokerType(DataSourceType sourceType) {
		this.brokerType = sourceType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DataCollector) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost());
	}
}
