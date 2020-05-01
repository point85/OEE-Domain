package org.point85.domain.http;

/**
 * Data Transfer Object (DTO) for an equipment event (availability, production
 * of material, setup or job change)
 * 
 *
 */
public class EquipmentEventRequestDto {
	private String eventType;
	private String equipmentName;
	private String sourceId;
	private String value;
	private String reason;
	private String timestamp;
	private String endTimestamp;
	private String duration;

	/**
	 * Construct the event
	 * 
	 * @param sourceId Data source identifier from the configured event resolver
	 * @param value    Data value
	 */
	public EquipmentEventRequestDto(String sourceId, String value) {
		this.sourceId = sourceId;
		this.value = value;
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getEndTimestamp() {
		return endTimestamp;
	}

	public void setEndTimestamp(String endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	public String getEquipmentName() {
		return equipmentName;
	}

	public void setEquipmentName(String equipmentName) {
		this.equipmentName = equipmentName;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("source id: " + sourceId);
		sb.append(", equipment: " + equipmentName);
		sb.append(", event: " + eventType);
		sb.append(", value: " + value);
		sb.append(", start timestamp: " + timestamp);
		sb.append(", end timestamp: " + endTimestamp);
		sb.append(", duration: " + duration);
		sb.append(", reason: " + reason);

		return sb.toString();
	}
}
