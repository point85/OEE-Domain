package org.point85.domain.http;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.HTTP_VALUE)

public class HttpSource extends CollectorDataSource {
	// overloaded for HTTPS port
	@Column(name = "END_PATH")
	private String httpsPort;

	public HttpSource() {
		super();
		setDataSourceType(DataSourceType.HTTP);
	}

	public HttpSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.HTTP);
	}

	@Override
	public String getId() {
		return getHost() + ":" + getPort();
	}

	@Override
	public void setId(String id) {
		String[] tokens = id.split(":");
		if (tokens.length == 2) {
			setHost(tokens[0]);
			setPort(Integer.valueOf(tokens[1]));
		}
	}

	// use endpoint path column
	public Integer getHttpsPort() {
		return httpsPort != null ? Integer.parseInt(httpsPort) : null;
	}

	// use endpoint path column
	public void setHttpsPort(Integer port) {
		httpsPort = (port != null) ? String.valueOf(port) : null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HttpSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getHttpsPort());
	}
}
