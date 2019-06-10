package org.point85.domain.collector;

import org.point85.domain.i18n.DomainLocalizer;

public enum DataSourceType {
	DATABASE, FILE, HTTP, JMS, MESSAGING, MODBUS, MQTT, OPC_DA, OPC_UA;

	public static final String OPC_DA_VALUE = "OPC_DA";
	public static final String OPC_UA_VALUE = "OPC_UA";
	public static final String HTTP_VALUE = "HTTP";
	public static final String MESSAGING_VALUE = "MESSAGING";
	public static final String JMS_VALUE = "JMS";
	public static final String DATABASE_VALUE = "DB";
	public static final String FILE_VALUE = "FILE";
	public static final String MQTT_VALUE = "MQTT";
	public static final String MODBUS_VALUE = "MODBUS";

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
		case MESSAGING:
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
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
