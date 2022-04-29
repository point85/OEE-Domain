package org.point85.domain.socket;

import org.point85.domain.messaging.ApplicationMessage;

/*
 * Listener for application messages
 */
public interface WebSocketMessageListener {
	/***
	 * Callback when an application message is received
	 * 
	 * @param message {@link ApplicationMessage}
	 */
	void onWebSocketMessage(ApplicationMessage message);
}
