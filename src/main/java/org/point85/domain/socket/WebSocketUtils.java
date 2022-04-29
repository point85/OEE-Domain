package org.point85.domain.socket;

import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.CollectorCommandMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageType;

import com.google.gson.Gson;

/**
 * Class with methods to support web sockets
 *
 */
public class WebSocketUtils {
	// serializer
	private static final Gson gson = new Gson();

	private WebSocketUtils() {
		// hide public constructor
	}

	/**
	 * Serialize the application message
	 * 
	 * @param message {@link ApplicationMessage}
	 * @return JSON string
	 */
	public static String serialize(ApplicationMessage message) {
		// payload is JSON string
		return gson.toJson(message);
	}

	private static ApplicationMessage deserialize(MessageType type, String payload) {
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

	/**
	 * Deserialize the JSON string into an application message
	 * 
	 * @param json JSON string
	 * @return {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public static ApplicationMessage deserialize(String json) throws Exception {
		ApplicationMessage appMessage = null;

		if (json.contains(MessageType.EQUIPMENT_EVENT.name())) {
			// equipment event
			appMessage = deserialize(MessageType.EQUIPMENT_EVENT, json);
		} else if (json.contains(MessageType.COMMAND.name())) {
			// command
			appMessage = deserialize(MessageType.COMMAND, json);
		} else {
			throw new Exception(DomainLocalizer.instance().getErrorString("invalid.ws.message", json));
		}

		return appMessage;
	}

}
