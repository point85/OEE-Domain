package org.point85.domain.messaging;

import java.time.OffsetDateTime;

public class EquipmentEventMessage extends ApplicationMessage {
	private String sourceId;
	private String value;

	public EquipmentEventMessage() {
		super(MessageType.EQUIPMENT_EVENT);
	}

	public EquipmentEventMessage(String sourceId, String value, OffsetDateTime timestamp) {
		super(MessageType.EQUIPMENT_EVENT);
		this.sourceId = sourceId;
		this.value = value;
		this.setTimestamp(timestamp);
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	protected void validate() throws Exception {
		super.validate();

		if (sourceId == null) {
			throw new Exception("The source id cannot be null");
		}

		if (value == null) {
			throw new Exception("The value cannot be null");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append('\n');
		sb.append("Source: ").append(sourceId).append(", Value: ").append(value).append(", Timestamp: ")
				.append(getTimestamp().toString());

		return sb.toString();
	}
}
