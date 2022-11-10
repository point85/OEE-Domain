package org.point85.domain.dto;

import org.point85.domain.schedule.Break;

public class BreakDto extends TimePeriodDto {
	// TimeLoss category
	private String timeLoss;

	public BreakDto(Break period) {
		super(period);
		this.timeLoss = period.getLossCategory() != null ? period.getLossCategory().name() : null;
	}

	public String getTimeLoss() {
		return timeLoss;
	}

	public void setTimeLoss(String timeLoss) {
		this.timeLoss = timeLoss;
	}
}
