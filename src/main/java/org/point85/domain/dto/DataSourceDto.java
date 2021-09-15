package org.point85.domain.dto;

import org.point85.domain.collector.CollectorDataSource;

/**
 * Data Transfer Object (DTO) for data source
 */
public class DataSourceDto extends NamedDto {
	private String host;
	private Integer port;

	public DataSourceDto(CollectorDataSource source) {
		super(source.getName(), source.getDescription());
		this.host = source.getHost();
		this.port = source.getPort();
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
}
