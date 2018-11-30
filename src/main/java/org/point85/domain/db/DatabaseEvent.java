package org.point85.domain.db;

import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.point85.domain.persistence.DatabaseEventStatusConverter;
import org.point85.domain.persistence.OffsetDateTimeConverter;
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

	@Column(name = "TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime time;

	@Column(name = "STATUS")
	@Convert(converter = DatabaseEventStatusConverter.class)
	private DatabaseEventStatus status = DatabaseEventStatus.READY;

	@Column(name = "ERROR")
	private String error;

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

	public OffsetDateTime getTime() {
		return time;
	}

	public void setTime(OffsetDateTime time) {
		this.time = time;
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

	@Override
	public String toString() {
		return "Source: " + sourceId + ", value: " + inputValue + ", status: " + status + ", time: " + time;
	}
}
