package org.point85.domain.dto;

import org.point85.domain.DomainUtils;
import org.point85.domain.schedule.ExceptionPeriod;

public class ExceptionPeriodDto extends NamedObjectDto {
	// LocalDateTime
	private String startDateTime;

	// duration
	private long duration;

	// TimeLoss category
	private String timeLoss;

	public ExceptionPeriodDto(ExceptionPeriod period) {
		super(period);
		this.startDateTime = DomainUtils.localDateTimeToString(period.getStartDateTime(),
				DomainUtils.LOCAL_DATE_TIME_8601);
		this.duration = period.getDuration().getSeconds();
		this.timeLoss = period.getLossCategory() != null ? period.getLossCategory().name() : null;
	}

	public String getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(String startDateTime) {
		this.startDateTime = startDateTime;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getTimeLoss() {
		return timeLoss;
	}

	public void setTimeLoss(String timeLoss) {
		this.timeLoss = timeLoss;
	}
}
