package org.point85.domain.dto;

import org.point85.domain.socket.WebSocketSource;

public class WebSocketSourceDto extends CollectorDataSourceDto {
	private String keystore;
	private String keystorePassword;
	private String keyPassword;
	private Boolean clientAuthentication;

	public WebSocketSourceDto(WebSocketSource source) {
		super(source);

		this.setKeystore(source.getKeystore());
		this.setKeystorePassword(source.getEncodedKeystorePassword());
		this.setKeyPassword(source.getEncodedKeyPassword());
		this.setClientAuthentication(source.isClientAuthorization());
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

	public void setKeystorePassword(String password) {
		this.keystorePassword = password;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String password) {
		this.keyPassword = password;
	}

	public Boolean getClientAuthentication() {
		return clientAuthentication;
	}

	public void setClientAuthentication(Boolean clientAuthentication) {
		this.clientAuthentication = clientAuthentication;
	}
}
