package org.point85.domain.mqtt;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.MqttSourceDto;

@Entity
@DiscriminatorValue(DataSourceType.MQTT_VALUE)
public class MqttSource extends CollectorDataSource {
	@Column(name = "KEYSTORE")
	private String keystore;

	@Column(name = "KEYSTORE_PWD")
	private String keystorePassword;

	// overloaded
	@Column(name = "MSG_MODE")
	private String messageMode;

	public MqttSource() {
		super();
		setDataSourceType(DataSourceType.MQTT);
	}

	public MqttSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.MQTT);
	}

	public MqttSource(MqttSourceDto dto) {
		super(dto);

		keystore = dto.getKeystore();
		keystorePassword = dto.getKeystorePassword();
		messageMode = dto.getKeyPassword();
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String fileName) {
		this.keystore = fileName;
	}

	public String getKeystorePassword() {
		return DomainUtils.decode(keystorePassword);
	}

	public void setKeystorePassword(String password) {
		this.keystorePassword = DomainUtils.encode(password);
	}

	public String getEncodedKeystorePassword() {
		return keystorePassword;
	}

	// overload message mode
	public String getKeyPassword() {
		return DomainUtils.decode(messageMode);
	}

	public void setKeyPassword(String password) {
		this.messageMode = DomainUtils.encode(password);
	}

	public String getEncodedKeyPassword() {
		return messageMode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MqttSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getKeystore());
	}
}
