package org.point85.domain.rmq;

public enum RoutingKey {
	ALL("#"), EQUIPMENT_SOURCE_EVENT("equipment.event"), NOTIFICATION_ALL("notification.#"),
	NOTIFICATION_MESSAGE("notification.msg"), NOTIFICATION_STATUS("notification.status"),
	RESOLVED_EVENT("resolved.event"), COMMAND_MESSAGE("command.#");

	private String routingId;

	private RoutingKey(String key) {
		this.routingId = key;
	}

	public String getKey() {
		return routingId;
	}
}
