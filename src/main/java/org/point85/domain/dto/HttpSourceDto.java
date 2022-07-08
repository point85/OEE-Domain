package org.point85.domain.dto;

import org.point85.domain.http.HttpSource;

public class HttpSourceDto extends CollectorDataSourceDto {
	
	private Integer httpsPort;

	public HttpSourceDto(HttpSource source) {
		super(source);
		
		this.setHttpsPort(source.getHttpsPort());
	}

	public Integer getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(Integer httpsPort) {
		this.httpsPort = httpsPort;
	}

}
