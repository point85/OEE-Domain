package org.point85.domain.http;

import org.point85.domain.collector.DataSource;

public class DataSourceDto {
	private String name;
	private String host;
	private Integer port;
	private String description;

	public DataSourceDto() {

	}

	public DataSourceDto(DataSource source) {
		this.name = source.getName();
		this.host = source.getHost();
		this.port = source.getPort();
		this.description = source.getDescription();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostName() {
		return host;
	}

	public void setHostName(String hostName) {
		this.host = hostName;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
