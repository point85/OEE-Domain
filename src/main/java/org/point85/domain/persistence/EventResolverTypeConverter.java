package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.script.EventResolverType;

@Converter
public class EventResolverTypeConverter implements AttributeConverter<EventResolverType, String> {

	@Override
	public String convertToDatabaseColumn(EventResolverType attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case AVAILABILITY:
			value = EventResolverType.AVAILABILITY_VALUE;
			break;
		case PROD_GOOD:
			value = EventResolverType.GOOD_PROD_VALUE;
			break;
		case PROD_REJECT:
			value = EventResolverType.REJECT_PROD_VALUE;
			break;
		case PROD_STARTUP:
			value = EventResolverType.STARTUP_PROD_VALUE;
			break;
		case JOB_CHANGE:
			value = EventResolverType.JOB_VALUE;
			break;
		case MATL_CHANGE:
			value = EventResolverType.MATERIAL_VALUE;
			break;
		case OTHER:
			value = EventResolverType.OTHER_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	@Override
	public EventResolverType convertToEntityAttribute(String value) {
		EventResolverType type = null;

		if (value == null) {
			return type;
		}

		switch (value) {
		case EventResolverType.AVAILABILITY_VALUE:
			type = EventResolverType.AVAILABILITY;
			break;

		case EventResolverType.GOOD_PROD_VALUE:
			type = EventResolverType.PROD_GOOD;
			break;

		case EventResolverType.REJECT_PROD_VALUE:
			type = EventResolverType.PROD_REJECT;
			break;

		case EventResolverType.STARTUP_PROD_VALUE:
			type = EventResolverType.PROD_STARTUP;
			break;

		case EventResolverType.JOB_VALUE:
			type = EventResolverType.JOB_CHANGE;
			break;

		case EventResolverType.MATERIAL_VALUE:
			type = EventResolverType.MATL_CHANGE;
			break;

		case EventResolverType.OTHER_VALUE:
			type = EventResolverType.OTHER;
			break;

		default:
			break;

		}
		return type;
	}

}
