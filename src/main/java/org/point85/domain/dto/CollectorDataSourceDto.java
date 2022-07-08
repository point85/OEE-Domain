package org.point85.domain.dto;

import org.point85.domain.collector.CollectorDataSource;

public abstract class CollectorDataSourceDto extends NamedObjectDto {
	private String host;

	private String userName;

	private String userPassword;

	private String sourceType;

	private Integer port;

	protected CollectorDataSourceDto(CollectorDataSource source) {
		super(source);

		this.host = source.getHost();
		this.userName = source.getUserName();
		this.userPassword = source.getEncodedPassword();
		this.sourceType = source.getDataSourceType() != null ? source.getDataSourceType().name() : null;
		this.port = source.getPort();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
}
