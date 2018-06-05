package org.point85.domain.http;

import java.util.List;

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
