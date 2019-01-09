package org.point85.domain.collector;

public enum DataSourceType {
	OPC_DA, OPC_UA, HTTP, MESSAGING, JMS, DATABASE, FILE;

	public static final String OPC_DA_VALUE = "OPC_DA";
	public static final String OPC_UA_VALUE = "OPC_UA";
	public static final String HTTP_VALUE = "HTTP";
	public static final String MESSAGING_VALUE = "MESSAGING";
	public static final String JMS_VALUE = "JMS";
	public static final String DATABASE_VALUE = "DB";
	public static final String FILE_VALUE = "FILE";
}
