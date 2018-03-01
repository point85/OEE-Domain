package org.point85.domain.collector;

import org.point85.domain.persistence.DataSourceConverter;

public abstract class CollectorProperties {
	// configuration file
	static final String CONFIG_FILE = "./conf/collector.properties";
	
	// configuration attributes
	static final String ATTRIB_COLLECTOR = "collector";
	static final String ATTRIB_TYPE = "type";
	static final String ATTRIB_ID = "id";
	static final String ATTRIB_BROKER = "brokerHost";
	
	// type of collector
	private DataSourceType type;
	
	// my identity
	private String identity;

	// RMQ broker host
	private String brokerHostName;

	public CollectorProperties(String type, String identity, String brokerHostName) {
		DataSourceConverter converter = new DataSourceConverter();
		this.setType(converter.convertToEntityAttribute(type));
		this.setIdentity(identity);
		this.setBrokerHostName(brokerHostName);
	}

	public String getBrokerHostName() {
		return brokerHostName;
	}

	public void setBrokerHostName(String hostName) {
		this.brokerHostName = hostName;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public DataSourceType getType() {
		return type;
	}

	public void setType(DataSourceType type) {
		this.type = type;
	}
}
