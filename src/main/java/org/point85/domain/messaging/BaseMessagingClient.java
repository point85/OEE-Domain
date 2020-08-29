package org.point85.domain.messaging;

import com.google.gson.Gson;

public abstract class BaseMessagingClient {
	// queue TTL (sec)
	protected static final int QUEUE_TTL_SEC = 3600;

	// json serializer
	private final Gson gson = new Gson();

	// flag for sending a notification message to the server
	private boolean notify = false;

	// host and port of broker
	private String hostName;

	private int hostPort = 0;

	protected String serialize(ApplicationMessage message) {
		// payload is JSON string
		return gson.toJson(message);
	}

	protected ApplicationMessage deserialize(MessageType type, String payload) {
		ApplicationMessage message = null;

		switch (type) {
		case EQUIPMENT_EVENT:
			message = gson.fromJson(payload, EquipmentEventMessage.class);
			break;

		case NOTIFICATION:
			message = gson.fromJson(payload, CollectorNotificationMessage.class);
			break;

		case STATUS:
			message = gson.fromJson(payload, CollectorServerStatusMessage.class);
			break;

		case RESOLVED_EVENT:
			message = gson.fromJson(payload, CollectorResolvedEventMessage.class);
			break;

		case COMMAND:
			message = gson.fromJson(payload, CollectorCommandMessage.class);
			break;

		default:
			break;
		}

		return message;
	}

	public boolean shouldNotify() {
		return notify;
	}

	public void setShouldNotify(boolean flag) {
		notify = flag;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getHostPort() {
		return hostPort;
	}

	public void setHostPort(int hostPort) {
		this.hostPort = hostPort;
	}

}
