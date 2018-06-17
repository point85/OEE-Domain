package org.point85.domain.http;

/**
 * Data Transfer Object (DTO) for an equipment event (availability, production
 * of material, setup or job change)
 * 
 *
 */
public class EquipmentEventRequestDto {
	private String sourceId;
	private String value;
	private String timestamp;

	/**
	 * Construct the event
	 * 
	 * @param sourceId
	 *            Data source identifier from the configured event resolver
	 * @param value
	 *            Data value
	 * @param timestamp
	 *            Date and time when event occurred
	 */
	public EquipmentEventRequestDto(String sourceId, String value, String timestamp) {
		this.sourceId = sourceId;
		this.value = value;
		this.timestamp = timestamp;
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

}
