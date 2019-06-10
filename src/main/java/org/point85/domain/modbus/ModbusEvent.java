package org.point85.domain.modbus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * An event containing data read from a Modbus slave
 *
 */
public class ModbusEvent {
	private ModbusSource source;
	private OffsetDateTime eventTime;
	private String reason;
	private List<ModbusVariant> values;
	private String sourceId;

	public ModbusEvent(ModbusSource source, String sourceId, OffsetDateTime eventTime, List<ModbusVariant> values) {
		this.setSource(source);
		this.sourceId = sourceId;
		this.setEventTime(eventTime);
		this.values = values;
	}

	public ModbusSource getSource() {
		return source;
	}

	public void setSource(ModbusSource source) {
		this.source = source;
	}

	public OffsetDateTime getEventTime() {
		return eventTime;
	}

	public void setEventTime(OffsetDateTime eventTime) {
		this.eventTime = eventTime;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public List<ModbusVariant> getValues() {
		return values;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
}
