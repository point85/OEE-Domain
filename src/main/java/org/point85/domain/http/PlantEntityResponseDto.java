package org.point85.domain.http;

import java.util.List;

public class PlantEntityResponseDto {
	private List<PlantEntityDto> entityList;
	
	public PlantEntityResponseDto() {
		
	}

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
