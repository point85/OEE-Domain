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

		case RMQ:
			value = DataSourceType.RMQ_VALUE;
			break;

		case JMS:
			value = DataSourceType.JMS_VALUE;
			break;

		case KAFKA:
			value = DataSourceType.KAFKA_VALUE;
			break;

		case MQTT:
			value = DataSourceType.MQTT_VALUE;
			break;

		case DATABASE:
			value = DataSourceType.DATABASE_VALUE;
			break;

		case FILE:
			value = DataSourceType.FILE_VALUE;
			break;

		case CRON:
			value = DataSourceType.CRON_VALUE;
			break;

		case MODBUS:
			value = DataSourceType.MODBUS_VALUE;
			break;

		case EMAIL:
			value = DataSourceType.EMAIL_VALUE;
			break;

		case PROFICY:
			value = DataSourceType.PROFICY_VALUE;
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

		case DataSourceType.RMQ_VALUE:
			state = DataSourceType.RMQ;
			break;

		case DataSourceType.JMS_VALUE:
			state = DataSourceType.JMS;
			break;

		case DataSourceType.KAFKA_VALUE:
			state = DataSourceType.KAFKA;
			break;

		case DataSourceType.MQTT_VALUE:
			state = DataSourceType.MQTT;
			break;

		case DataSourceType.DATABASE_VALUE:
			state = DataSourceType.DATABASE;
			break;

		case DataSourceType.FILE_VALUE:
			state = DataSourceType.FILE;
			break;

		case DataSourceType.CRON_VALUE:
			state = DataSourceType.CRON;
			break;

		case DataSourceType.MODBUS_VALUE:
			state = DataSourceType.MODBUS;
			break;

		case DataSourceType.EMAIL_VALUE:
			state = DataSourceType.EMAIL;
			break;

		case DataSourceType.PROFICY_VALUE:
			state = DataSourceType.PROFICY;
			break;

		default:
			break;
		}

		return state;
	}
}
