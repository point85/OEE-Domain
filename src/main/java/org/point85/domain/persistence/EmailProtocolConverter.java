package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.email.EmailProtocol;

@Converter
public class EmailProtocolConverter implements AttributeConverter<EmailProtocol, String> {

	@Override
	public String convertToDatabaseColumn(EmailProtocol attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case SMTP:
			value = EmailProtocol.SMTP_VALUE;
			break;
		case IMAP:
			value = EmailProtocol.IMAP_VALUE;
			break;
		case POP3:
			value = EmailProtocol.POP3_VALUE;
			break;
		default:
			break;
		}

		return value;
	}

	@Override
	public EmailProtocol convertToEntityAttribute(String value) {
		EmailProtocol protocol = null;

		if (value == null) {
			return protocol;
		}

		switch (value) {
		case EmailProtocol.SMTP_VALUE:
			protocol = EmailProtocol.SMTP;
			break;
		case EmailProtocol.IMAP_VALUE:
			protocol = EmailProtocol.IMAP;
			break;
		case EmailProtocol.POP3_VALUE:
			protocol = EmailProtocol.POP3;
			break;
		default:
			break;
		}

		return protocol;
	}

}
