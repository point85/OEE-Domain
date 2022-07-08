package org.point85.domain.collector;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.dto.DataCollectorDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.CollectorStateConverter;
import org.point85.domain.persistence.DataSourceConverter;
import org.point85.domain.persistence.PersistenceService;
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

	@OneToOne
	@JoinColumn(name = "SOURCE_KEY")
	private CollectorDataSource notificationServer;

	public DataCollector() {
		super();
	}

	public DataCollector(String name, String description) {
		super(name, description);
	}

	public DataCollector(DataCollectorDto dto) throws Exception {
		super(dto.getName(), dto.getDescription());

		this.brokerHost = dto.getBrokerHost();
		this.brokerPort = dto.getBrokerPort();
		this.brokerType = dto.getBrokerType() != null ? DataSourceType.valueOf(dto.getBrokerType()) : null;
		this.brokerUserName = dto.getBrokerUserName();
		this.brokerUserPassword = dto.getBrokerUserPassword();
		this.host = dto.getHost();

		if (dto.getNotificationServer() != null) {
			notificationServer = PersistenceService.instance().fetchDataSourceByName(dto.getNotificationServer());

			if (notificationServer == null) {
				throw new Exception(
						DomainLocalizer.instance().getErrorString("no.data.source", dto.getNotificationServer()));
			}
		}

		this.state = dto.getState() != null ? CollectorState.valueOf(dto.getState()) : null;
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

	public CollectorDataSource getNotificationServer() {
		return this.notificationServer;
	}

	public void setNotificationServer(CollectorDataSource server) {
		this.notificationServer = server;
	}

	public DataSourceType getBrokerType() {
		return brokerType;
	}

	public void setBrokerType(DataSourceType brokerType) {
		this.brokerType = brokerType;
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

	public void setBrokerUserName(String brokerUserName) {
		this.brokerUserName = brokerUserName;
	}

	public String getBrokerUserPassword() {
		return brokerUserPassword;
	}

	public void setBrokerUserPassword(String brokerUserPassword) {
		this.brokerUserPassword = brokerUserPassword;
	}
}
