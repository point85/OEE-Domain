package org.point85.domain.http;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.HttpSourceDto;

/**
 * The HttpSource class represents an HTTP/HTTPS server as a source of
 * application events.
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.HTTP_VALUE)
public class HttpSource extends CollectorDataSource {
	// overloaded for HTTPS port
	@Column(name = "END_PATH")
	private String httpsPort;

	// overloaded for client id
	@Column(name = "MSG_MODE")
	private String clientId;

	// overloaded for client secret
	@Column(name = "KEYSTORE")
	private String clientSecret;

	public HttpSource() {
		super();
		setDataSourceType(DataSourceType.HTTP);
	}

	public HttpSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.HTTP);
	}

	public HttpSource(HttpSourceDto dto) {
		super(dto);
		setHttpsPort(dto.getHttpsPort());

		setClientId(dto.getClientId());
		setClientSecret(dto.getClientSecret());
	}
	
	public HttpSource(String host, Integer port) {
		super(host, port);
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

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
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
