package org.point85.domain.messaging;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * Base class for Point85 application messages
 *
 */
public abstract class ApplicationMessage {
	// type of message for deserialization
	private MessageType messageType;

	// sender host name
	private String senderHostName;

	// sender host IP address
	private String senderHostAddress;

	// time message sent
	private String timestamp;

	// id of sender
	private String senderId;

	protected ApplicationMessage(MessageType messageType) {
		this.messageType = messageType;
	}

	protected ApplicationMessage(String senderHostName, String senderHostAddress, MessageType messageType) {
		this.messageType = messageType;
		this.senderHostName = senderHostName;
		this.senderHostAddress = senderHostAddress;
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

	public void validate() throws Exception {
		if (messageType == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("null.type"));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
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

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
}
