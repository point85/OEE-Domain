package org.point85.domain.db;

public enum DatabaseEventStatus {
	READY, PROCESSING, PASS, FAIL;
	
	// database values
	public static final String READY_VALUE = "READY";
	public static final String PROCESSING_VALUE = "PROCESSING";
	public static final String PASS_VALUE = "PASS";
	public static final String FAIL_VALUE = "FAIL";
}

