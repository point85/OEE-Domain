package org.point85.domain.dto;

import org.point85.domain.proficy.ProficySource;

public class ProficySourceDto extends CollectorDataSourceDto {
	private Integer uaaHttpPort;
	private Boolean validateCertificate;
	private Integer httpsPort;
	
	public ProficySourceDto(ProficySource source) {
		super(source);
		
		this.uaaHttpPort = source.getUaaHttpPort();
		this.validateCertificate = source.getValidateCertificate();
		this.httpsPort = source.getHttpsPort();
	}

	public Integer getUaaHttpPort() {
		return uaaHttpPort;
	}

	public void setUaaHttpPort(Integer uaaHttpPort) {
		this.uaaHttpPort = uaaHttpPort;
	}

	public Boolean getValidateCertificate() {
		return validateCertificate;
	}

	public void setValidateCertificate(Boolean validateCertificate) {
		this.validateCertificate = validateCertificate;
	}

	public Integer getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(Integer httpsPort) {
		this.httpsPort = httpsPort;
	}
}
