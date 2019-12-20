package org.point85.domain.mqtt;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.MQTT_VALUE)
public class MqttSource extends CollectorDataSource {
	public MqttSource() {
		super();
		setDataSourceType(DataSourceType.MQTT);
	}

	public MqttSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.MQTT);
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}
}
