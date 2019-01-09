package org.point85.domain.jms;

import org.point85.domain.messaging.EquipmentEventMessage;

public interface JMSListener {
	void onEquipmentEvent(EquipmentEventMessage message);
}
