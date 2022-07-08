package org.point85.domain.email;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * Security policies supported by the email client
 *
 */
public enum EmailSecurityPolicy {
	SSL, TLS, NONE;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case SSL:
			key = "email.ssl.policy";
			break;
		case TLS:
			key = "email.tls.policy";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}

}
