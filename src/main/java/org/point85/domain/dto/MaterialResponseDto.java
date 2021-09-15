package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for a material HTTP response
 */
public class MaterialResponseDto {
	private List<MaterialDto> materialList;

	public MaterialResponseDto(List<MaterialDto> materialList) {
		this.materialList = materialList;
	}

	public List<MaterialDto> getMaterialList() {
		return materialList;
	}

	public void setMaterialList(List<MaterialDto> materialList) {
		this.materialList = materialList;
	}
}
