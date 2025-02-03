package org.point85.domain.oee;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.i18n.DomainLocalizer;

public enum TimeLoss {
	NOT_SCHEDULED, UNSCHEDULED, REJECT_REWORK, STARTUP_YIELD, PLANNED_DOWNTIME, UNPLANNED_DOWNTIME, MINOR_STOPPAGES,
	REDUCED_SPEED, SETUP, NO_LOSS;

	// database values
	public static final String NO_LOSS_VALUE = "NLOSS";
	public static final String NOT_SCHEDULED_VALUE = "NSCHED";
	public static final String UNSCHEDULED_VALUE = "USCHED";
	public static final String PLANNED_DOWNTIME_VALUE = "PDOWN";
	public static final String SETUP_VALUE = "SETUP";
	public static final String UNPLANNED_DOWNTIME_VALUE = "UDOWN";
	public static final String MINOR_STOPPAGES_VALUE = "STOP";
	public static final String REDUCED_SPEED_VALUE = "SPEED";
	public static final String REJECT_REWORK_VALUE = "REJECT";
	public static final String STARTUP_YIELD_VALUE = "START";

	public static List<TimeLoss> getAvailabilityLosses() {
		List<TimeLoss> losses = new ArrayList<>(3);
		losses.add(PLANNED_DOWNTIME);
		losses.add(SETUP);
		losses.add(UNPLANNED_DOWNTIME);
		return losses;
	}

	public static List<TimeLoss> getPerformanceLosses() {
		List<TimeLoss> losses = new ArrayList<>(2);
		losses.add(MINOR_STOPPAGES);
		losses.add(REDUCED_SPEED);
		return losses;
	}

	public static List<TimeLoss> getQualityLosses() {
		List<TimeLoss> losses = new ArrayList<>(2);
		losses.add(REJECT_REWORK);
		losses.add(STARTUP_YIELD);
		return losses;
	}

	public static List<TimeLoss> getNonWorkingLosses() {
		List<TimeLoss> losses = new ArrayList<>(2);
		losses.add(NOT_SCHEDULED);
		losses.add(UNSCHEDULED);
		return losses;
	}

	public static List<TimeLoss> getBreakLosses() {
		List<TimeLoss> losses = new ArrayList<>(3);
		losses.add(NOT_SCHEDULED);
		losses.add(UNSCHEDULED);
		losses.add(PLANNED_DOWNTIME);
		return losses;
	}

	public static TimeLoss getNoLoss() {
		return NO_LOSS;
	}

	public boolean isLoss() {
		boolean isLoss = true;

		if (this.equals(NO_LOSS) || this.equals(NOT_SCHEDULED) || this.equals(UNSCHEDULED)) {
			isLoss = false;
		}
		return isLoss;
	}

	public String getColor() {
		// WHITE
		String color = "#FFFFFF";

		switch (this) {
		case MINOR_STOPPAGES:
			// AQUA
			color = "#00FFFF";
			break;
		case NOT_SCHEDULED:
			// BISQUE
			color = "#FFE4C4";
			break;
		case PLANNED_DOWNTIME:
			// CORAL
			color = "#FF7F50";
			break;
		case REDUCED_SPEED:
			// SALMON
			color = "#F9A825"; //"#FFFF00";
			break;
		case REJECT_REWORK:
			// PURPLE
			color = "#800080";
			break;
		case STARTUP_YIELD:
			// IVORY
			color = "#FFFFF0";
			break;
		case UNPLANNED_DOWNTIME:
			// RED
			color = "#FF0000";
			break;
		case UNSCHEDULED:
			// LIGHTGREY
			color = "#D3D3D3";
			break;
		case SETUP:
			// MAGENTA
			color = "#FF00FF";
			break;
		default:
			break;
		}
		return color;
	}

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case MINOR_STOPPAGES:
			key = "minor.loss";
			break;
		case NOT_SCHEDULED:
			key = "not.scheduled.loss";
			break;
		case NO_LOSS:
			key = "no.loss";
			break;
		case PLANNED_DOWNTIME:
			key = "planned.loss";
			break;
		case REDUCED_SPEED:
			key = "speed.loss";
			break;
		case REJECT_REWORK:
			key = "reject.loss";
			break;
		case SETUP:
			key = "setup.loss";
			break;
		case STARTUP_YIELD:
			key = "startup.loss";
			break;
		case UNPLANNED_DOWNTIME:
			key = "unplanned.loss";
			break;
		case UNSCHEDULED:
			key = "unscheduled.loss";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}

	public String serialize() {
		String value = null;

		switch (this) {
		case MINOR_STOPPAGES:
			value = MINOR_STOPPAGES_VALUE;
			break;
		case NOT_SCHEDULED:
			value = NOT_SCHEDULED_VALUE;
			break;
		case NO_LOSS:
			value = NO_LOSS_VALUE;
			break;
		case PLANNED_DOWNTIME:
			value = PLANNED_DOWNTIME_VALUE;
			break;
		case REDUCED_SPEED:
			value = REDUCED_SPEED_VALUE;
			break;
		case REJECT_REWORK:
			value = REJECT_REWORK_VALUE;
			break;
		case SETUP:
			value = SETUP_VALUE;
			break;
		case STARTUP_YIELD:
			value = STARTUP_YIELD_VALUE;
			break;
		case UNPLANNED_DOWNTIME:
			value = UNPLANNED_DOWNTIME_VALUE;
			break;
		case UNSCHEDULED:
			value = UNSCHEDULED_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	public OeeComponent getComponent() {
		OeeComponent value = null;

		switch (this) {
		case MINOR_STOPPAGES:
			value = OeeComponent.PERFORMANCE;
			break;
		case NOT_SCHEDULED:
			value = OeeComponent.NON_WORKING;
			break;
		case NO_LOSS:
			value = OeeComponent.NORMAL;
			break;
		case PLANNED_DOWNTIME:
			value = OeeComponent.AVAILABILITY;
			break;
		case REDUCED_SPEED:
			value = OeeComponent.PERFORMANCE;
			break;
		case REJECT_REWORK:
			value = OeeComponent.QUALITY;
			break;
		case SETUP:
			value = OeeComponent.AVAILABILITY;
			;
			break;
		case STARTUP_YIELD:
			value = OeeComponent.QUALITY;
			break;
		case UNPLANNED_DOWNTIME:
			value = OeeComponent.AVAILABILITY;
			;
			break;
		case UNSCHEDULED:
			value = OeeComponent.NON_WORKING;
			break;
		default:
			break;
		}
		return value;
	}
}
