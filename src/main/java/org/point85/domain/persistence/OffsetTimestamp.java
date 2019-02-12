package org.point85.domain.persistence;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.persistence.Embeddable;

import org.point85.domain.DomainUtils;

@Embeddable
public class OffsetTimestamp {

	// local date and time of day
	private LocalDateTime localDateTime;

	// offset from UTC in seconds
	private int utcOffset = 0;

	public OffsetTimestamp() {

	}

	public OffsetTimestamp(OffsetDateTime timestamp) {
		this.localDateTime = timestamp.toLocalDateTime();
		this.utcOffset = timestamp.getOffset().getTotalSeconds();
	}

	public OffsetDateTime toOffsetDateTime() {
		OffsetDateTime timestamp = null;

		if (localDateTime != null) {
			ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(utcOffset);
			timestamp = OffsetDateTime.of(localDateTime, zoneOffset);
		}
		return timestamp;
	}

	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public void setLocalDateTime(LocalDateTime dateTime) {
		this.localDateTime = dateTime;
	}

	public int getOffset() {
		return utcOffset;
	}

	public void setOffset(int offset) {
		this.utcOffset = offset;
	}

	@Override
	public String toString() {
		return DomainUtils.offsetDateTimeToString(toOffsetDateTime(), DomainUtils.OFFSET_DATE_TIME_PATTERN);
	}
}
