package org.point85.domain.messaging;

public enum MessageType {
	EQUIPMENT_EVENT(MessagingConstants.EQUIP_EVENT_ATTRIB), NOTIFICATION(
			MessagingConstants.NOTIFICATION_ATTRIB), STATUS(MessagingConstants.STATUS_ATTRIB), RESOLVED_EVENT(
					MessagingConstants.RESOLVED_EVENT_ATTRIB), COMMAND(MessagingConstants.COMMAND_ATTRIB);

	private String type;

	private MessageType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return this.type;
	}

	public static MessageType fromString(String type) {

		MessageType messageType = null;

		if (type == null) {
			return messageType;
		}

		if (type.equals(MessagingConstants.EQUIP_EVENT_ATTRIB)) {
			messageType = MessageType.EQUIPMENT_EVENT;
		} else if (type.equals(MessagingConstants.NOTIFICATION_ATTRIB)) {
			messageType = MessageType.NOTIFICATION;
		} else if (type.equals(MessagingConstants.STATUS_ATTRIB)) {
			messageType = MessageType.STATUS;
		} else if (type.equals(MessagingConstants.RESOLVED_EVENT_ATTRIB)) {
			messageType = MessageType.RESOLVED_EVENT;
		} else if (type.equals(MessagingConstants.COMMAND_ATTRIB)) {
			messageType = MessageType.COMMAND;
		}

		return messageType;
	}
}
