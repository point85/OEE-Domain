package org.point85.domain.script;

public enum EventType {
	AVAILABILITY, PROD_GOOD, PROD_REJECT, PROD_STARTUP, MATL_CHANGE, JOB_CHANGE, OTHER;

	// database values
	public static final String AVAILABILITY_VALUE = "AVAIL";
	public static final String GOOD_PROD_VALUE = "GOOD";
	public static final String REJECT_PROD_VALUE = "REJECT";
	public static final String STARTUP_PROD_VALUE = "STARTUP";
	public static final String MATERIAL_VALUE = "MATL";
	public static final String JOB_VALUE = "JOB";
	public static final String OTHER_VALUE = "OTHER";

	public boolean isAvailability() {
		return this.equals(AVAILABILITY) ? true : false;
	}

	public boolean isProduction() {
		return (this.equals(PROD_GOOD) || this.equals(PROD_REJECT) || this.equals(PROD_STARTUP)) ? true : false;
	}

	public boolean isMaterial() {
		return this.equals(MATL_CHANGE) ? true : false;
	}

	public boolean isJob() {
		return this.equals(JOB_CHANGE) ? true : false;
	}
}
