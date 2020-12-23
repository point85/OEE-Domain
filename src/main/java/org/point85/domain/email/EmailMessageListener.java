package org.point85.domain.email;

import org.point85.domain.messaging.ApplicationMessage;

/**
 * Listener for events received via email
 *
 */
public interface EmailMessageListener {
	/**
	 * Perform processing of a received {@link ApplicationMessage}
	 * 
	 * @param message ApplicationMessage
	 */
	void onEmailMessage(ApplicationMessage message);
}
