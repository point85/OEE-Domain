package org.point85.domain.dto;

import org.point85.domain.email.EmailSource;

public class EmailSourceDto extends CollectorDataSourceDto {
	private String receiveSecurityPolicy;
	private String emailProtocol;
	private String sendHost;
	private Integer sendPort;
	private String sendSecurityPolicy;

	public EmailSourceDto(EmailSource source) {
		super(source);

		this.setReceiveSecurityPolicy(
				source.getReceiveSecurityPolicy() != null ? source.getReceiveSecurityPolicy().name() : null);
		this.setSendSecurityPolicy(
				source.getSendSecurityPolicy() != null ? source.getSendSecurityPolicy().name() : null);
		this.setEmailProtocol(source.getProtocol() != null ? source.getProtocol().name() : null);
		this.setSendHost(source.getSendHost());
		this.setSendPort(source.getSendPort());
	}

	public String getReceiveSecurityPolicy() {
		return receiveSecurityPolicy;
	}

	public void setReceiveSecurityPolicy(String receiveSecurityPolicy) {
		this.receiveSecurityPolicy = receiveSecurityPolicy;
	}

	public String getEmailProtocol() {
		return emailProtocol;
	}

	public void setEmailProtocol(String emailProtocol) {
		this.emailProtocol = emailProtocol;
	}

	public String getSendHost() {
		return sendHost;
	}

	public void setSendHost(String sendHost) {
		this.sendHost = sendHost;
	}

	public Integer getSendPort() {
		return sendPort;
	}

	public void setSendPort(Integer sendPort) {
		this.sendPort = sendPort;
	}

	public String getSendSecurityPolicy() {
		return sendSecurityPolicy;
	}

	public void setSendSecurityPolicy(String sendSecurityPolicy) {
		this.sendSecurityPolicy = sendSecurityPolicy;
	}
}
