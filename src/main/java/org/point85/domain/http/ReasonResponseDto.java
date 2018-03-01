package org.point85.domain.http;

import java.util.List;

public class ReasonResponseDto {
	private List<ReasonDto> reasonList;
	
	public ReasonResponseDto() {
		
	}

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
