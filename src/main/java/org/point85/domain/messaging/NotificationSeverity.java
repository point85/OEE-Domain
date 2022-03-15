package org.point85.domain.messaging;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * The severity level of a notification message
 *
 */
public enum NotificationSeverity {
	ERROR, WARNING, INFO;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case ERROR:
			key = "error.type";
			break;
		case INFO:
			key = "info.type";
			break;
		case WARNING:
			key = "warning.type";
			break;
		default:
			break;

		}
		return DomainLocalizer.instance().getLangString(key);
	}

	public String getColor() {
		String color = null;

		switch (this) {
		case ERROR:
			// RED
			color = "#FF0000";
			break;
		case INFO:
			// BLACK
			color = "#000000";
			break;
		case WARNING:
			// AQUA
			color = "#00FFFF";
			break;
		default:
			break;
		}
		return color;
	}
}
