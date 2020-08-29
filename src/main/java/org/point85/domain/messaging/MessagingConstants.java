package org.point85.domain.messaging;

class MessagingConstants {
	private MessagingConstants() {
		throw new IllegalStateException("Utility class");
	}

	static final String RESOLVED_EVENT_ATTRIB = "RESOLVED_EVENT";
	static final String EQUIP_EVENT_ATTRIB = "EQUIP_EVENT";
	static final String NOTIFICATION_ATTRIB = "NOTIFICATION";
	static final String STATUS_ATTRIB = "STATUS";
	static final String COMMAND_ATTRIB = "COMMAND";
}
