package org.point85.domain.mqtt;

import org.point85.domain.messaging.ApplicationMessage;

public interface MqttMessageListener {
	void onMqttMessage(ApplicationMessage message);
}
