package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.collector.DataSourceType;

@Converter
public class DataSourceConverter implements AttributeConverter<DataSourceType, String> {

	@Override
	public String convertToDatabaseColumn(DataSourceType attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case OPC_DA:
			value = DataSourceType.OPC_DA_VALUE;
			break;

		case OPC_UA:
			value = DataSourceType.OPC_UA_VALUE;
			break;

		case HTTP:
			value = DataSourceType.HTTP_VALUE;
			break;

		case MESSAGING:
			value = DataSourceType.MESSAGING_VALUE;
			break;

		default:
			break;
		}

		return value;
	}

	@Override
	public DataSourceType convertToEntityAttribute(String value) {
		DataSourceType state = null;

		if (value == null) {
			return state;
		}

		switch (value) {
		case DataSourceType.OPC_DA_VALUE:
			state = DataSourceType.OPC_DA;
			break;

		case DataSourceType.OPC_UA_VALUE:
			state = DataSourceType.OPC_UA;
			break;

		case DataSourceType.HTTP_VALUE:
			state = DataSourceType.HTTP;
			break;

		case DataSourceType.MESSAGING_VALUE:
			state = DataSourceType.MESSAGING;
			break;

		default:
			break;
		}

		return state;
	}
}
