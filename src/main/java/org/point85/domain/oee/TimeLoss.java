package org.point85.domain.oee;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

public enum TimeLoss {
	NO_LOSS("Running"), NOT_SCHEDULED("Not Scheduled"), UNSCHEDULED("Unscheduled"), PLANNED_DOWNTIME(
			"Planned Downtime"), SETUP("Setup"), UNPLANNED_DOWNTIME("Unplanned Downtime"), MINOR_STOPPAGES(
					"Stoppages"), REDUCED_SPEED(
							"Reduced Speed"), REJECT_REWORK("Reject/Rework"), STARTUP_YIELD("Startup/Yield");

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
	
	private String displayString;

	private TimeLoss(String displayString) {
		this.displayString = displayString;
	}

	public String getDisplayString() {
		return displayString;
	}

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

	public boolean isLoss() {
		boolean isLoss = true;

		if (this.equals(NOT_SCHEDULED) || this.equals(UNSCHEDULED)) {
			isLoss = false;
		}
		return isLoss;
	}

	public Color getColor() {
		Color color = Color.WHITE;

		switch (this) {
		case MINOR_STOPPAGES:
			color = Color.AQUA;
			break;
		case NOT_SCHEDULED:
			color = Color.BISQUE;
			break;
		case PLANNED_DOWNTIME:
			color = Color.CORAL;
			break;
		case REDUCED_SPEED:
			color = Color.YELLOW;
			break;
		case REJECT_REWORK:
			color = Color.LAVENDER;
			break;
		case STARTUP_YIELD:
			color = Color.IVORY;
			break;
		case UNPLANNED_DOWNTIME:
			color = Color.RED;
			break;
		case UNSCHEDULED:
			color = Color.LIGHTGREY;
			break;
		case SETUP:
			color = Color.MAGENTA;
			break;
		default:
			break;

		}
		return color;
	}
}
