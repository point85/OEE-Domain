package org.point85.domain.dto;

import org.point85.domain.opc.ua.OpcUaSource;

public class OpcUaSourceDto extends CollectorDataSourceDto {
	private String securityPolicy;
	private String messageMode;
	private String keystore;
	private String keystorePassword;
	private String endpointPath;

	public OpcUaSourceDto(OpcUaSource source) {
		super(source);

		this.securityPolicy = source.getSecurityPolicy() != null ? source.getSecurityPolicy().name() : null;
		this.messageMode = source.getMessageSecurityMode() != null ? source.getMessageSecurityMode().name() : null;
		this.keystore = source.getKeystore();
		this.keystorePassword = source.getEncodedKeystorePassword();
		this.endpointPath = source.getEndpointPath();
	}

	public String getSecurityPolicy() {
		return securityPolicy;
	}

	public void setSecurityPolicy(String securityPolicy) {
		this.securityPolicy = securityPolicy;
	}

	public String getMessageMode() {
		return messageMode;
	}

	public void setMessageMode(String messageMode) {
		this.messageMode = messageMode;
	}

	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public String getEndpointPath() {
		return endpointPath;
	}

	public void setEndpointPath(String endpointPath) {
		this.endpointPath = endpointPath;
	}

}
