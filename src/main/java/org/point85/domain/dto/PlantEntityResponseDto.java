package org.point85.domain.dto;

import org.point85.domain.exim.ExportContent;

/**
 * Data Transfer Object (DTO) for an HTTP plant entity response
 */
public class PlantEntityResponseDto {
	private ExportContent content;

	public PlantEntityResponseDto(ExportContent content) {
		this.setContent(content);
	}

	public ExportContent getContent() {
		return content;
	}

	public void setContent(ExportContent content) {
		this.content = content;
	}
}
