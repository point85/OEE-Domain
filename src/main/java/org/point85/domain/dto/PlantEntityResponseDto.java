package org.point85.domain.dto;

import org.point85.domain.exim.ExportImportContent;

/**
 * Data Transfer Object (DTO) for an HTTP plant entity response
 */
public class PlantEntityResponseDto {
	private ExportImportContent content;

	public PlantEntityResponseDto(ExportImportContent content) {
		this.setContent(content);
	}

	public ExportImportContent getContent() {
		return content;
	}

	public void setContent(ExportImportContent content) {
		this.content = content;
	}
}
