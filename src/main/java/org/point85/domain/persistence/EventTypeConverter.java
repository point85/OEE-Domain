package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.script.OeeEventType;

@Converter
public class EventTypeConverter implements AttributeConverter<OeeEventType, String> {

	@Override
	public String convertToDatabaseColumn(OeeEventType attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case AVAILABILITY:
			value = OeeEventType.AVAILABILITY_VALUE;
			break;
		case PROD_GOOD:
			value = OeeEventType.GOOD_PROD_VALUE;
			break;
		case PROD_REJECT:
			value = OeeEventType.REJECT_PROD_VALUE;
			break;
		case PROD_STARTUP:
			value = OeeEventType.STARTUP_PROD_VALUE;
			break;
		case JOB_CHANGE:
			value = OeeEventType.JOB_VALUE;
			break;
		case MATL_CHANGE:
			value = OeeEventType.MATERIAL_VALUE;
			break;
		case CUSTOM:
			value = OeeEventType.CUSTOM_VALUE;
			break;
		default:			
			break;
		}
		return value;
	}

	@Override
	public OeeEventType convertToEntityAttribute(String value) {
		OeeEventType type = null;

		if (value == null) {
			return type;
		}

		switch (value) {
		case OeeEventType.AVAILABILITY_VALUE:
			type = OeeEventType.AVAILABILITY;
			break;

		case OeeEventType.GOOD_PROD_VALUE:
			type = OeeEventType.PROD_GOOD;
			break;

		case OeeEventType.REJECT_PROD_VALUE:
			type = OeeEventType.PROD_REJECT;
			break;

		case OeeEventType.STARTUP_PROD_VALUE:
			type = OeeEventType.PROD_STARTUP;
			break;

		case OeeEventType.JOB_VALUE:
			type = OeeEventType.JOB_CHANGE;
			break;

		case OeeEventType.MATERIAL_VALUE:
			type = OeeEventType.MATL_CHANGE;
			break;
			
		case OeeEventType.CUSTOM_VALUE:
			type = OeeEventType.CUSTOM;
			break;

		default:
			break;

		}
		return type;
	}
}
