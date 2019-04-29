package org.point85.domain.messaging;

import org.point85.domain.i18n.DomainLocalizer;

import javafx.scene.paint.Color;

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

	public Color getColor() {
		Color color = null;

		switch (this) {
		case ERROR:
			color = Color.RED;
			break;
		case INFO:
			color = Color.BLACK;
			break;
		case WARNING:
			color = Color.AQUA;
			break;
		default:
			break;
		}
		return color;
	}
}
