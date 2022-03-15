package org.point85.domain.collector;

import org.point85.domain.i18n.DomainLocalizer;

public enum DataSourceType {
	CRON, DATABASE, EMAIL, FILE, HTTP, JMS, KAFKA, MODBUS, MQTT, OPC_DA, OPC_UA, PROFICY, RMQ;

	public static final String OPC_DA_VALUE = "OPC_DA";
	public static final String OPC_UA_VALUE = "OPC_UA";
	public static final String HTTP_VALUE = "HTTP";
	public static final String RMQ_VALUE = "MESSAGING";
	public static final String JMS_VALUE = "JMS";
	public static final String DATABASE_VALUE = "DB";
	public static final String FILE_VALUE = "FILE";
	public static final String MQTT_VALUE = "MQTT";
	public static final String MODBUS_VALUE = "MODBUS";
	public static final String CRON_VALUE = "CRON";
	public static final String KAFKA_VALUE = "KAFKA";
	public static final String EMAIL_VALUE = "EMAIL";
	public static final String PROFICY_VALUE = "PROFICY";

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case DATABASE:
			key = "db.type";
			break;
		case FILE:
			key = "file.type";
			break;
		case HTTP:
			key = "http.type";
			break;
		case JMS:
			key = "jms.type";
			break;
		case RMQ:
			key = "rmq.type";
			break;
		case MQTT:
			key = "mqtt.type";
			break;
		case OPC_DA:
			key = "opc.da.type";
			break;
		case OPC_UA:
			key = "opc.ua.type";
			break;
		case MODBUS:
			key = "modbus.type";
			break;
		case CRON:
			key = "cron.type";
			break;
		case KAFKA:
			key = "kafka.type";
			break;
		case EMAIL:
			key = "email.type";
			break;
		case PROFICY:
			key = "proficy.type";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
