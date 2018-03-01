package org.point85.domain.messaging;

public enum RoutingKey {
	ALL("#"), EQUIPMENT_SOURCE_EVENT("equipment.event"), NOTIFICATION_ALL("notification.#"), NOTIFICATION_MESSAGE(
			"notification.msg"), NOTIFICATION_STATUS("notification.status"), RESOLVED_EVENT("resolved.event");

	private String routingKey;

	private RoutingKey(String key) {
		this.setRoutingKey(key);
	}

	public String getKey() {
		return routingKey;
	}

	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}
}
