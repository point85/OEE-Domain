package org.point85.domain.collector;

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

	protected BaseSummary() {
		super();
	}

	protected BaseSummary(LossSummary summary) {
		super(summary.getEquipment());
		this.setStartTime(summary.getStartTime());
		this.setEndTime(summary.getEndTime());
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public OffsetDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(OffsetDateTime endTime) {
		this.endTime = endTime;
	}

}
