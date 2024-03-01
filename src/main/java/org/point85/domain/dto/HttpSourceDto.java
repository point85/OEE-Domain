package org.point85.domain.dto;

import org.point85.domain.http.HttpSource;

public class HttpSourceDto extends CollectorDataSourceDto {
	// HTTPS
	private Integer httpsPort;

	// OAuth 2.0
	private String clientId;
	private String clientSecret;

	// external application flag
	private Boolean standalone;

	public HttpSourceDto(HttpSource source) {
		super(source);

		this.setHttpsPort(source.getHttpsPort());
		this.setClientId(source.getClientId());
		this.setClientSecret(source.getClientSecret());

	}

	public Integer getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(Integer httpsPort) {
		this.httpsPort = httpsPort;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public Boolean isStandalone() {
		return standalone;
	}

	public void setStandalone(Boolean externalApp) {
		this.standalone = externalApp;
	}
}
