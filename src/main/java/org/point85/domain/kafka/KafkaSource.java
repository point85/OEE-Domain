package org.point85.domain.kafka;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

/**
 * The KafkaSource class represents a Kafka server both as a source of events
 * and one that can receive notifications.
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.KAFKA_VALUE)
public class KafkaSource extends CollectorDataSource {
	@Column(name = "KEYSTORE")
	private String keystore;

	@Column(name = "KEYSTORE_PWD")
	private String keystorePassword;

	// overloaded
	@Column(name = "SEC_POLICY")
	private String truststorePassword;

	// overloaded
	@Column(name = "MSG_MODE")
	private String messageMode;

	public KafkaSource() {
		super();
		setDataSourceType(DataSourceType.KAFKA);
	}

	public KafkaSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.KAFKA);
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

	// overload endpoint path
	public String getTruststore() {
		return getEndpointPath();
	}

	public void setTruststore(String fileName) {
		setEndpointPath(fileName);
	}

	// overload security policy
	public String getTruststorePassword() {
		return DomainUtils.decode(truststorePassword);
	}

	public void setTruststorePassword(String password) {
		this.truststorePassword = DomainUtils.encode(password);
	}

	// overload message mode
	public String getKeyPassword() {
		return DomainUtils.decode(messageMode);
	}

	public void setKeyPassword(String password) {
		this.messageMode = DomainUtils.encode(password);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KafkaSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getKeystore(), getTruststore());
	}
}
