package org.point85.domain.opc.ua;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.OPC_UA_VALUE)

public class OpcUaSource extends CollectorDataSource {

	@Transient
	private SecurityPolicy policy = SecurityPolicy.None;

	@Transient
	private String endpointUrl;

	@Transient
	private MessageSecurityMode messageSecurityMode = MessageSecurityMode.None;

	@Column(name = "SEC_POLICY")
	private String securityPolicy;

	@Column(name = "MSG_MODE")
	private String messageMode;

	@Column(name = "KEYSTORE")
	private String keystore;

	@Column(name = "KEYSTORE_PWD")
	private String keystorePassword;
	
	@Column(name = "END_PATH")
	private String endpointPath;

	public OpcUaSource() {
		super();
		setDataSourceType(DataSourceType.OPC_UA);
	}

	public OpcUaSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.OPC_UA);
	}

	@Override
	public String getId() {
		return getEndpointUrl();
	}

	@Override
	public void setId(String id) {
		String[] tokens = id.split(":");
		if (tokens.length == 2) {
			setHost(tokens[0]);
			setPort(Integer.valueOf(tokens[1]));
		}
	}
	
	public String getEndpointPath() {
		return endpointPath;
	}

	public void setEndpointPath(String path) {
		this.endpointPath = path;
	}

	public String getEndpointUrl() {
		if (endpointUrl == null) {
			// only TCP is supported
			endpointUrl = String.format("opc.tcp://%s:%s", getHost(), getPort());

			if (getEndpointPath() != null && getEndpointPath().length() > 0) {
				endpointUrl += "/" + getEndpointPath();
			}
		}
		return endpointUrl;
	}

	public SecurityPolicy getSecurityPolicy() {
		if (securityPolicy != null) {
			policy = SecurityPolicy.valueOf(securityPolicy);
		}
		return policy;
	}

	public void setSecurityPolicy(SecurityPolicy policy) {
		if (policy != null) {
			this.securityPolicy = policy.name();
			this.policy = policy;
		}
	}

	public MessageSecurityMode getMessageSecurityMode() {
		if (messageMode != null) {
			messageSecurityMode = MessageSecurityMode.valueOf(messageMode);
		}
		return messageSecurityMode;
	}

	public void setMessageSecurityMode(MessageSecurityMode messageSecurityMode) {
		if (messageSecurityMode != null) {
			this.messageMode = messageSecurityMode.name();
			this.messageSecurityMode = messageSecurityMode;
		}
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

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = DomainUtils.encode(keystorePassword);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpcUaSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getEndpointUrl());
	}

}
