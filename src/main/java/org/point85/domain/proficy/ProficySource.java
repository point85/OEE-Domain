package org.point85.domain.proficy;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.ProficySourceDto;

/**
 * A Proficy historian data source
 * 
 */

@Entity
@DiscriminatorValue(DataSourceType.PROFICY_VALUE)
public class ProficySource extends CollectorDataSource {
	// overloaded for UAA HTTP port
	@Column(name = "MSG_MODE")
	private String uaaHttpPort;

	// overloaded for SSL certificate validation
	@Column(name = "KEYSTORE")
	private String validateCertificate;

	// overloaded for historian SSL port
	@Column(name = "KEYSTORE_PWD")
	private String httpsPort;

	public ProficySource() {
		super();
		setDataSourceType(DataSourceType.PROFICY);
	}

	public ProficySource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.PROFICY);
	}

	public ProficySource(ProficySourceDto dto) {
		super(dto);
		setDataSourceType(DataSourceType.PROFICY);

		this.uaaHttpPort = dto.getUaaHttpPort() != null ? String.valueOf(dto.getUaaHttpPort()) : null;
		this.validateCertificate = dto.getValidateCertificate() != null ? String.valueOf(dto.getValidateCertificate())
				: null;
		this.httpsPort = dto.getHttpsPort() != null ? String.valueOf(dto.getHttpsPort()) : null;
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	public Integer getHttpPort() {
		return getPort();
	}

	public void setHttpPort(Integer port) {
		setPort(port);
	}

	public Integer getHttpsPort() {
		return (httpsPort != null) ? Integer.parseInt(httpsPort) : null;
	}

	public void setHttpsPort(Integer port) {
		this.httpsPort = port != null ? String.valueOf(port) : null;
	}

	public Boolean getValidateCertificate() {
		return Boolean.valueOf(validateCertificate);
	}

	public void setValidateCertificate(Boolean flag) {
		this.validateCertificate = flag.toString();
	}

	public Integer getUaaHttpPort() {
		return Integer.valueOf(uaaHttpPort);
	}

	public void setUaaHttpPort(Integer port) {
		this.uaaHttpPort = String.valueOf(port);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProficySource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getHttpPort(), getHttpsPort());
	}
}
