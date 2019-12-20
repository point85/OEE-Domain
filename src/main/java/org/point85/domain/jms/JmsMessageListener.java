package org.point85.domain.jms;

import org.point85.domain.messaging.ApplicationMessage;

public interface JmsMessageListener {
	void onJmsMessage(ApplicationMessage message);
}
