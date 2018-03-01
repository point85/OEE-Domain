package org.point85.domain.messaging;

import java.time.OffsetDateTime;

import org.point85.domain.CollectorUtils;

public class ApplicationMessage {
	// type of message for deserialization
	private MessageType messageType;

	// sender host name
	private String senderHostName;

	// sender host IP address
	private String senderHostAddress;

	// time message sent
	private String timestamp;
	
	protected ApplicationMessage(MessageType messageType) {
		this.setMessageType(messageType);
		this.setTimestamp(OffsetDateTime.now());
	}

	protected ApplicationMessage(String senderHostName,  String senderHostAddress, MessageType messageType) {
		this.setMessageType(messageType);
		this.setSenderHostName(senderHostName);
		this.setSenderHostAddress(senderHostAddress);
		this.setTimestamp(OffsetDateTime.now());
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType documentType) {
		this.messageType = documentType;
	}

	public String getSenderHostName() {
		return senderHostName;
	}

	public void setSenderHostName(String senderHostName) {
		this.senderHostName = senderHostName;
	}

	protected void validate() throws Exception {
		if (messageType == null) {
			throw new Exception("The message type cannot be null");
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Type: ").append(messageType.toString()).append(", Sender Host: ").append(senderHostName);

		return sb.toString();
	}

	public String getSenderHostAddress() {
		return senderHostAddress;
	}

	public void setSenderHostAddress(String senderHostAddress) {
		this.senderHostAddress = senderHostAddress;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public void setTimestamp(OffsetDateTime odt) {
		this.timestamp = CollectorUtils.offsetDateTimeToString(odt);
	}
	
}
