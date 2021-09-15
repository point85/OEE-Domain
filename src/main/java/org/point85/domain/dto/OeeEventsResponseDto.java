package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for OEE events HTTP response
 */
public class OeeEventsResponseDto {
	private List<OeeEventDto> eventList;

	public OeeEventsResponseDto(List<OeeEventDto> eventList) {
		this.eventList = eventList;
	}

	public List<OeeEventDto> getOeeEventList() {
		return eventList;
	}

	public void setOeeEventList(List<OeeEventDto> eventList) {
		this.eventList = eventList;
	}
}
