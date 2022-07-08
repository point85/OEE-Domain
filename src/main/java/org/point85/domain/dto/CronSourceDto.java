package org.point85.domain.dto;

import org.point85.domain.cron.CronEventSource;

public class CronSourceDto extends CollectorDataSourceDto {
	private String cronExpression;

	public CronSourceDto(CronEventSource source) {
		super(source);
		this.cronExpression = source.getCronExpression();
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
}
