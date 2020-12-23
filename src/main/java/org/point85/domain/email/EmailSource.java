package org.point85.domain.email;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

/**
 * The EmailSource class represents an email server both as a source of events
 * and one that can receive notifications.
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.EMAIL_VALUE)
public class EmailSource extends CollectorDataSource {
	// receive security policy
	@Transient
	private EmailSecurityPolicy receivePolicy = EmailSecurityPolicy.NONE;

	// SSL or TLS as String
	@Column(name = "SEC_POLICY")
	private String receiveSecurityPolicy;

	// send security policy
	@Transient
	private EmailSecurityPolicy sendPolicy = EmailSecurityPolicy.NONE;

	// protocol
	@Transient
	private EmailProtocol protocol = EmailProtocol.NONE;

	// overloaded for email protocol
	@Column(name = "MSG_MODE")
	private String emailProtocol;

	// overloaded for send host
	@Column(name = "KEYSTORE")
	private String sendHost;

	// send port
	@Column(name = "KEYSTORE_PWD")
	private String sendPort;

	public EmailSource() {
		super();
		setDataSourceType(DataSourceType.EMAIL);
	}

	public EmailSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.EMAIL);
	}

	public String getReceiveHost() {
		return getHost();
	}

	public void setReceiveHost(String host) {
		setHost(host);
	}

	public Integer getReceivePort() {
		return getPort();
	}

	public void setReceivePort(Integer port) {
		setPort(port);
	}

	// host for sending messages
	public String getSendHost() {
		return sendHost;
	}

	public void setSendHost(String host) {
		this.sendHost = host;
	}

	public Integer getSendPort() {
		return Integer.valueOf(sendPort);
	}

	public void setSendPort(Integer port) {
		this.sendPort = String.valueOf(port);
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	// overload message mode for protocol
	public EmailProtocol getProtocol() {
		if (protocol != null) {
			protocol = EmailProtocol.valueOf(emailProtocol);
		}
		return protocol;
	}

	public void setProtocol(EmailProtocol protocol) {
		if (protocol != null) {
			this.emailProtocol = protocol.name();
			this.protocol = protocol;
		}
	}

	public EmailSecurityPolicy getReceiveSecurityPolicy() {
		if (receiveSecurityPolicy != null) {
			receivePolicy = EmailSecurityPolicy.valueOf(receiveSecurityPolicy);
		}
		return receivePolicy;
	}

	public void setReceiveSecurityPolicy(EmailSecurityPolicy policy) {
		if (policy != null) {
			this.receiveSecurityPolicy = policy.name();
			this.receivePolicy = policy;
		}
	}

	public EmailSecurityPolicy getSendSecurityPolicy() {
		if (getEndpointPath() != null) {
			sendPolicy = EmailSecurityPolicy.valueOf(getEndpointPath());
		}
		return sendPolicy;
	}

	public void setSendSecurityPolicy(EmailSecurityPolicy policy) {
		if (policy != null) {
			setEndpointPath(policy.name());
			this.sendPolicy = policy;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EmailSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getProtocol(), getReceiveSecurityPolicy());
	}
}
