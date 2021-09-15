package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for an HTTP plant entity response
 */
public class PlantEntityResponseDto {
	private List<PlantEntityDto> entityList;

	public PlantEntityResponseDto(List<PlantEntityDto> entityList) {
		this.entityList = entityList;
	}

	public List<PlantEntityDto> getEntityList() {
		return entityList;
	}

	public void setEntityList(List<PlantEntityDto> entityList) {
		this.entityList = entityList;
	}
}
