package org.point85.domain.collector;

import org.point85.domain.persistence.DataSourceConverter;

public class CollectorProperties {
	// configuration file
	static final String CONFIG_FILE = "./conf/collector.properties";
	
	// configuration attributes
	static final String ATTRIB_COLLECTOR = "collector";
	static final String ATTRIB_TYPE = "type";
	static final String ATTRIB_ID = "id";
	static final String ATTRIB_BROKER = "brokerHost";
	
	// type of collector
	private final DataSourceType type;
	
	// my identity
	private final String identity;

	// RMQ broker host
	private final String brokerHostName;

	public CollectorProperties(String type, String identity, String brokerHostName) {
		DataSourceConverter converter = new DataSourceConverter();
		this.type = converter.convertToEntityAttribute(type);
		this.identity = identity;
		this.brokerHostName = brokerHostName;
	}

	public String getBrokerHostName() {
		return brokerHostName;
	}

	public String getIdentity() {
		return identity;
	}

	public DataSourceType getType() {
		return type;
	}
}
