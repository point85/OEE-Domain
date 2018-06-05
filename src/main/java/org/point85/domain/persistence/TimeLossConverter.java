package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.oee.TimeLoss;

@Converter
public class TimeLossConverter implements AttributeConverter<TimeLoss, String> {

	@Override
	public String convertToDatabaseColumn(TimeLoss attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case MINOR_STOPPAGES:
			value = TimeLoss.MINOR_STOPPAGES_VALUE;
			break;
		case NOT_SCHEDULED:
			value = TimeLoss.NOT_SCHEDULED_VALUE;
			break;
		case PLANNED_DOWNTIME:
			value = TimeLoss.PLANNED_DOWNTIME_VALUE;
			break;
		case REDUCED_SPEED:
			value = TimeLoss.REDUCED_SPEED_VALUE;
			break;
		case REJECT_REWORK:
			value = TimeLoss.REJECT_REWORK_VALUE;
			break;
		case STARTUP_YIELD:
			value = TimeLoss.STARTUP_YIELD_VALUE;
			break;
		case UNPLANNED_DOWNTIME:
			value = TimeLoss.UNPLANNED_DOWNTIME_VALUE;
			break;
		case UNSCHEDULED:
			value = TimeLoss.UNSCHEDULED_VALUE;
			break;
		case SETUP:
			value = TimeLoss.SETUP_VALUE;
			break;
		case NO_LOSS:
			value = TimeLoss.NO_LOSS_VALUE;
			break;
		default:
			break;
		}
		return value;
	}

	@Override
	public TimeLoss convertToEntityAttribute(String value) {
		TimeLoss loss = null;

		if (value == null) {
			return loss;
		}

		switch (value) {
		case TimeLoss.MINOR_STOPPAGES_VALUE:
			loss = TimeLoss.MINOR_STOPPAGES;
			break;
		case TimeLoss.NOT_SCHEDULED_VALUE:
			loss = TimeLoss.NOT_SCHEDULED;
			break;
		case TimeLoss.PLANNED_DOWNTIME_VALUE:
			loss = TimeLoss.PLANNED_DOWNTIME;
			break;
		case TimeLoss.REDUCED_SPEED_VALUE:
			loss = TimeLoss.REDUCED_SPEED;
			break;
		case TimeLoss.REJECT_REWORK_VALUE:
			loss = TimeLoss.REJECT_REWORK;
			break;
		case TimeLoss.STARTUP_YIELD_VALUE:
			loss = TimeLoss.STARTUP_YIELD;
			break;
		case TimeLoss.UNPLANNED_DOWNTIME_VALUE:
			loss = TimeLoss.UNPLANNED_DOWNTIME;
			break;
		case TimeLoss.UNSCHEDULED_VALUE:
			loss = TimeLoss.UNSCHEDULED;
			break;
		case TimeLoss.SETUP_VALUE:
			loss = TimeLoss.SETUP;
			break;
		case TimeLoss.NO_LOSS_VALUE:
			loss = TimeLoss.NO_LOSS;
			break;
		default:
			break;
		}
		return loss;
	}

}
