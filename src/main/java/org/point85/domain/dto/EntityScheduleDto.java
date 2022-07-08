package org.point85.domain.dto;

import org.point85.domain.DomainUtils;
import org.point85.domain.plant.EntitySchedule;

public class EntityScheduleDto {
	private String startDateTime;
	private String endDateTime;
	private String workSchedule;

	EntityScheduleDto(EntitySchedule entitySchedule) {
		this.startDateTime = entitySchedule.getStartDateTime() != null
				? DomainUtils.localDateTimeToString(entitySchedule.getStartDateTime(), DomainUtils.LOCAL_DATE_TIME_8601)
				: null;

		this.endDateTime = entitySchedule.getEndDateTime() != null
				? DomainUtils.localDateTimeToString(entitySchedule.getEndDateTime(), DomainUtils.LOCAL_DATE_TIME_8601)
				: null;

		this.workSchedule = entitySchedule.getWorkSchedule() != null ? entitySchedule.getWorkSchedule().getName()
				: null;
	}

	public String getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(String startDateTime) {
		this.startDateTime = startDateTime;
	}

	public String getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(String endDateTime) {
		this.endDateTime = endDateTime;
	}

	public String getWorkSchedule() {
		return workSchedule;
	}

	public void setWorkSchedule(String workSchedule) {
		this.workSchedule = workSchedule;
	}
}
