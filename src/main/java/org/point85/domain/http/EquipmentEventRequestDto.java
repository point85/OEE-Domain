package org.point85.domain.http;

import java.time.OffsetDateTime;

import org.point85.domain.DomainUtils;

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

	public void setDateTime(OffsetDateTime odt) {
		timestamp = DomainUtils.offsetDateTimeToString(odt, DomainUtils.OFFSET_DATE_TIME_8601);
	}

	public OffsetDateTime getDateTime() {
		return DomainUtils.offsetDateTimeFromString(timestamp, DomainUtils.OFFSET_DATE_TIME_8601);
	}

}
