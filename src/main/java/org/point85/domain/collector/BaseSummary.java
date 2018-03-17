package org.point85.domain.collector;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

import org.point85.domain.persistence.OffsetDateTimeConverter;

@MappedSuperclass
public class BaseSummary extends BaseRecord {

	@Column(name = "START_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime startTime;

	@Column(name = "END_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime endTime;

	// length of event
	@Column(name = "DURATION")
	private Duration duration;

	protected BaseSummary() {
		super();
	}

	protected BaseSummary(LossSummary summary) {
		super(summary.getEquipment());
		this.startTime = summary.getStartTime();
		this.endTime = summary.getEndTime();
		this.duration = summary.getDuration();
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}
}
