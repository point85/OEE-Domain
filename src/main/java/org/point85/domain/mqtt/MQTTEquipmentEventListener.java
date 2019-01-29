package org.point85.domain.mqtt;

import org.point85.domain.messaging.EquipmentEventMessage;

public interface MQTTEquipmentEventListener {
	void onMQTTEquipmentEvent(EquipmentEventMessage message);
}
