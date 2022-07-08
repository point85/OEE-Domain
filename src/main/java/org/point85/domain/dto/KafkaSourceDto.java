package org.point85.domain.dto;

import org.point85.domain.kafka.KafkaSource;

public class KafkaSourceDto extends CollectorDataSourceDto {
	private String keystore;
	private String keystorePassword;
	private String truststore;
	private String truststorePassword;
	private String keyPassword;

	public KafkaSourceDto(KafkaSource source) {
		super(source);

		this.setKeystore(source.getKeystore());
		this.setKeystorePassword(source.getEncodedKeystorePassword());
		this.setTruststore(source.getTruststore());
		this.setTruststorePassword(source.getEncodedTruststorePassword());
		this.setKeyPassword(source.getEncodedKeyPassword());
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

	public String getTruststore() {
		return truststore;
	}

	public void setTruststore(String truststore) {
		this.truststore = truststore;
	}

	public String getTruststorePassword() {
		return truststorePassword;
	}

	public void setTruststorePassword(String password) {
		this.truststorePassword = password;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String password) {
		this.keyPassword = password;
	}

}
