package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for a reason HTTP response
 */
public class ReasonResponseDto {
	private List<ReasonDto> reasonList;
	
	public ReasonResponseDto(List<ReasonDto> reasonList) {
		this.reasonList = reasonList;
	}

	public List<ReasonDto> getReasonList() {
		return reasonList;
	}

	public void setReasonList(List<ReasonDto> reasonList) {
		this.reasonList = reasonList;
	}
}
