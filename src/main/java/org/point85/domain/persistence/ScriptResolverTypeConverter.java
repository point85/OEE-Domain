package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.script.ScriptResolverType;

@Converter
public class ScriptResolverTypeConverter implements AttributeConverter<ScriptResolverType, String> {

	@Override
	public String convertToDatabaseColumn(ScriptResolverType attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case AVAILABILITY:
			value = ScriptResolverType.AVAILABILITY_VALUE;
			break;
		case PROD_GOOD:
			value = ScriptResolverType.GOOD_PROD_VALUE;
			break;
		case JOB:
			value = ScriptResolverType.JOB_VALUE;
			break;
		case MATERIAL:
			value = ScriptResolverType.MATERIAL_VALUE;
			break;
		case OTHER:
			value = ScriptResolverType.OTHER_VALUE;
			break;
		case PROD_REJECT:
			value = ScriptResolverType.REJECT_PROD_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	@Override
	public ScriptResolverType convertToEntityAttribute(String value) {
		ScriptResolverType type = null;

		if (value == null) {
			return type;
		}

		switch (value) {
		case ScriptResolverType.AVAILABILITY_VALUE:
			type = ScriptResolverType.AVAILABILITY;
			break;

		case ScriptResolverType.GOOD_PROD_VALUE:
			type = ScriptResolverType.PROD_GOOD;
			break;

		case ScriptResolverType.JOB_VALUE:
			type = ScriptResolverType.JOB;
			break;

		case ScriptResolverType.MATERIAL_VALUE:
			type = ScriptResolverType.MATERIAL;
			break;

		case ScriptResolverType.OTHER_VALUE:
			type = ScriptResolverType.OTHER;
			break;

		case ScriptResolverType.REJECT_PROD_VALUE:
			type = ScriptResolverType.PROD_REJECT;
			break;

		default:
			break;

		}
		return type;
	}

}
