package org.point85.domain.script;

import java.util.ArrayList;
import java.util.List;

public enum EventType {
	AVAILABILITY, PROD_GOOD, PROD_REJECT, PROD_STARTUP, MATL_CHANGE, JOB_CHANGE;

	// database values
	public static final String AVAILABILITY_VALUE = "AVAIL";
	public static final String GOOD_PROD_VALUE = "GOOD";
	public static final String REJECT_PROD_VALUE = "REJECT";
	public static final String STARTUP_PROD_VALUE = "STARTUP";
	public static final String MATERIAL_VALUE = "MATL";
	public static final String JOB_VALUE = "JOB";

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

	public static List<EventType> getProductionTypes() {
		List<EventType> types = new ArrayList<>();
		types.add(EventType.PROD_GOOD);
		types.add(EventType.PROD_REJECT);
		types.add(EventType.PROD_STARTUP);
		return types;
	}
}
