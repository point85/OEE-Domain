package org.point85.domain.dto;

import org.point85.domain.schedule.TimePeriod;

public class TimePeriodDto extends NamedObjectDto {
	// local start time seconds
	private int startTime = 0;

	// duration in seconds
	private long duration = 0;

	public TimePeriodDto(TimePeriod period) {
		super(period);
		this.startTime = period.getStart().toSecondOfDay();
		this.duration = period.getDuration().getSeconds();
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

}
