package org.point85.domain.dto;

import org.point85.domain.schedule.RotationSegment;

public class RotationSegmentDto {
	// ordering
	private int sequence = 0;

	// days on
	private int daysOn = 0;

	// days off
	private int daysOff = 0;

	// starting shift
	private String shift;

	public RotationSegmentDto(RotationSegment segment) {
		this.sequence = segment.getSequence();
		this.daysOn = segment.getDaysOn();
		this.daysOff = segment.getDaysOff();
		this.shift = segment.getStartingShift().getName();
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public int getDaysOn() {
		return daysOn;
	}

	public void setDaysOn(int daysOn) {
		this.daysOn = daysOn;
	}

	public int getDaysOff() {
		return daysOff;
	}

	public void setDaysOff(int daysOff) {
		this.daysOff = daysOff;
	}

	public String getShift() {
		return shift;
	}

	public void setShift(String shift) {
		this.shift = shift;
	}
}
