package org.point85.domain.email;

/**
 * Protocols supported by the email client.
 *
 */
public enum EmailProtocol {
	SMTP, IMAP, POP3, NONE;

	public static final String SMTP_VALUE = "SMTP";
	public static final String IMAP_VALUE = "IMAP";
	public static final String POP3_VALUE = "POP3";

	@Override
	public String toString() {
		return this.name();
	}
}
