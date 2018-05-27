package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.script.EventType;

@Converter
public class EventTypeConverter implements AttributeConverter<EventType, String> {

	@Override
	public String convertToDatabaseColumn(EventType attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case AVAILABILITY:
			value = EventType.AVAILABILITY_VALUE;
			break;
		case PROD_GOOD:
			value = EventType.GOOD_PROD_VALUE;
			break;
		case PROD_REJECT:
			value = EventType.REJECT_PROD_VALUE;
			break;
		case PROD_STARTUP:
			value = EventType.STARTUP_PROD_VALUE;
			break;
		case JOB_CHANGE:
			value = EventType.JOB_VALUE;
			break;
		case MATL_CHANGE:
			value = EventType.MATERIAL_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	@Override
	public EventType convertToEntityAttribute(String value) {
		EventType type = null;

		if (value == null) {
			return type;
		}

		switch (value) {
		case EventType.AVAILABILITY_VALUE:
			type = EventType.AVAILABILITY;
			break;

		case EventType.GOOD_PROD_VALUE:
			type = EventType.PROD_GOOD;
			break;

		case EventType.REJECT_PROD_VALUE:
			type = EventType.PROD_REJECT;
			break;

		case EventType.STARTUP_PROD_VALUE:
			type = EventType.PROD_STARTUP;
			break;

		case EventType.JOB_VALUE:
			type = EventType.JOB_CHANGE;
			break;

		case EventType.MATERIAL_VALUE:
			type = EventType.MATL_CHANGE;
			break;

		default:
			break;

		}
		return type;
	}
}
