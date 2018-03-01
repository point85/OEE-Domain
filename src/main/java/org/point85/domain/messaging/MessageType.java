package org.point85.domain.messaging;

public enum MessageType implements MessagingConstants {
	EQUIPMENT_EVENT(EQUIP_EVENT_ATTRIB), NOTIFICATION(NOTIFICATION_ATTRIB), STATUS(STATUS_ATTRIB), RESOLVED_EVENT(
			RESOLVED_EVENT_ATTRIB);

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

		if (type.equals(EQUIP_EVENT_ATTRIB)) {
			messageType = MessageType.EQUIPMENT_EVENT;
		} else if (type.equals(NOTIFICATION_ATTRIB)) {
			messageType = MessageType.NOTIFICATION;
		} else if (type.equals(STATUS_ATTRIB)) {
			messageType = MessageType.STATUS;
		} else if (type.equals(RESOLVED_EVENT_ATTRIB)) {
			messageType = MessageType.RESOLVED_EVENT;
		}

		return messageType;
	}
}
