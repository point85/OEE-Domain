package org.point85.domain.kafka;

import org.point85.domain.messaging.ApplicationMessage;

/**
 * A listener to receive Kafka messages
 *
 */
public interface KafkaMessageListener {
	void onKafkaMessage(ApplicationMessage message);
}
