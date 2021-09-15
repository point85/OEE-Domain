package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for a data source id HTTP response
 */
public class SourceIdResponseDto {
	private List<String> sourceIds;

	public SourceIdResponseDto(List<String> sourceIds) {
		this.sourceIds = sourceIds;
	}

	public List<String> getSourceIds() {
		return sourceIds;
	}

	public void setSourceIds(List<String> sourceIds) {
		this.sourceIds = sourceIds;
	}
}
