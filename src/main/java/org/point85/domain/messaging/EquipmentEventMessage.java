package org.point85.domain.messaging;

public class EquipmentEventMessage extends ApplicationMessage {
	private String sourceId;
	private String value;
	private String reason;

	public EquipmentEventMessage() {
		super(MessageType.EQUIPMENT_EVENT);
	}

	public EquipmentEventMessage(String sourceId, String value, String timestamp) {
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
	
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public void validate() throws Exception {
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
