package org.point85.domain.collector;

public enum DataSourceType {
	OPC_DA, OPC_UA, HTTP, MESSAGING, WEB;

	public static final String OPC_DA_VALUE = "OPC_DA";
	public static final String OPC_UA_VALUE = "OPC_UA";
	public static final String HTTP_VALUE = "HTTP";
	public static final String MESSAGING_VALUE = "MESSAGING";
	public static final String WEB_VALUE = "WEB";
}
