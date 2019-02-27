package org.point85.domain.db;

import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.point85.domain.persistence.DatabaseEventStatusConverter;
import org.point85.domain.persistence.OffsetTimestamp;
import org.point85.domain.plant.KeyedObject;

@Entity
@Table(name = "DB_EVENT")
@AttributeOverride(name = "primaryKey", column = @Column(name = "EVENT_KEY"))

public class DatabaseEvent extends KeyedObject {
	private static final int ERROR_LENGTH = 256;

	@Column(name = "SOURCE_ID")
	private String sourceId;

	@Column(name = "IN_VALUE")
	private String inputValue;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "localDateTime", column = @Column(name = "EVENT_TIME")),
			@AttributeOverride(name = "utcOffset", column = @Column(name = "EVENT_TIME_OFFSET")) })
	private OffsetTimestamp eventTime;

	@Column(name = "STATUS")
	@Convert(converter = DatabaseEventStatusConverter.class)
	private DatabaseEventStatus status = DatabaseEventStatus.READY;

	@Column(name = "ERROR")
	private String error;

	@Column(name = "REASON")
	private String reason;

	public DatabaseEvent() {
		super();
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public DatabaseEventStatus getStatus() {
		return status;
	}

	public void setStatus(DatabaseEventStatus status) {
		this.status = status;
	}

	public String getInputValue() {
		return inputValue;
	}

	public void setInputValue(String inputValue) {
		this.inputValue = inputValue;
	}

	public OffsetTimestamp getOffsetStartTime() {
		return eventTime;
	}

	public void setOffsetStartTime(OffsetTimestamp offsetTime) {
		eventTime = offsetTime;
	}

	public OffsetDateTime getEventTime() {
		return eventTime != null ? eventTime.toOffsetDateTime() : null;
	}

	public void setEventTime(OffsetDateTime dateTime) {
		if (dateTime != null) {
			this.eventTime = new OffsetTimestamp(dateTime);
		}
	}

	public String getError() {
		return error;
	}

	public void setError(String msg) {
		// check on length
		this.error = msg;

		if (msg != null && msg.length() >= ERROR_LENGTH) {
			this.error = msg.substring(0, ERROR_LENGTH - 1);
		}
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "Source: " + sourceId + ", value: " + inputValue + ", status: " + status + ", time: " + eventTime;
	}
}
