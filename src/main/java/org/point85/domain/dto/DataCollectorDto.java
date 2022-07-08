package org.point85.domain.dto;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.DataCollector;

public class DataCollectorDto extends NamedObjectDto {
	private String host;
	private String state;
	private String brokerType;
	private String brokerHost;
	private Integer brokerPort;
	private String brokerUserName;
	private String brokerUserPassword;
	private String notificationServer;

	public DataCollectorDto(DataCollector collector) {
		super(collector);

		this.host = collector.getHost();
		this.state = collector.getCollectorState() != null ? collector.getCollectorState().name() : null;
		this.brokerType = collector.getBrokerType() != null ? collector.getBrokerType().name() : null;
		this.brokerHost = collector.getBrokerHost();
		this.brokerUserName = collector.getBrokerUserName();
		this.brokerUserPassword = collector.getBrokerUserPassword() != null
				? DomainUtils.encode(collector.getBrokerUserPassword())
				: null;
		this.notificationServer = collector.getNotificationServer() != null
				? collector.getNotificationServer().getName()
				: null;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getBrokerType() {
		return brokerType;
	}

	public void setBrokerType(String brokerType) {
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

	public String getNotificationServer() {
		return notificationServer;
	}

	public void setNotificationServer(String notificationServer) {
		this.notificationServer = notificationServer;
	}

}
