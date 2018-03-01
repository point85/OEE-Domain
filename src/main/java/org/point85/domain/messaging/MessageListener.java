package org.point85.domain.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public interface MessageListener {
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) throws Exception;
}
