package org.point85.domain.rmq;

import org.point85.domain.messaging.ApplicationMessage;

public interface RmqMessageListener {
	public void onRmqMessage(ApplicationMessage message) throws Exception;
}
