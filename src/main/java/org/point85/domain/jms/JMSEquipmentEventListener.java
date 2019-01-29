package org.point85.domain.jms;

import org.point85.domain.messaging.EquipmentEventMessage;

public interface JMSEquipmentEventListener {
	void onJMSEquipmentEvent(EquipmentEventMessage message);
}
