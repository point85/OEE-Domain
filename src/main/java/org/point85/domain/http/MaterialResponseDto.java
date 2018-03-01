package org.point85.domain.http;

import java.util.List;

public class MaterialResponseDto {
	private List<MaterialDto> materialList;
	
	public MaterialResponseDto() {
		
	}

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
