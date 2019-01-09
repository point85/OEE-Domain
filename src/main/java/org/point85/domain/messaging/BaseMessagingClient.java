package org.point85.domain.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public abstract class BaseMessagingClient {
	// queue TTL (sec)
	protected static final int QUEUE_TTL_SEC = 3600;

	// json serializer
	private final Gson gson = new Gson();

	protected String serialize(ApplicationMessage message) {
		// payload is JSON string
		return gson.toJson(message);
	}

	protected ApplicationMessage deserialize(MessageType type, String payload) throws JsonSyntaxException {
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

}
