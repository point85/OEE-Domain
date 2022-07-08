package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.plant.Area;
import org.point85.domain.plant.EntityLevel;

public class AreaDto extends PlantEntityDto {
	private List<ProductionLineDto> productionLines = new ArrayList<>();

	public AreaDto(Area area) {
		super(area);
	}

	public AreaDto(String name, String description) {
		super(name, description, EntityLevel.AREA.name());
	}

	public List<ProductionLineDto> getProductionLines() {
		return productionLines;
	}

	public void setProductionLines(List<ProductionLineDto> productionLines) {
		this.productionLines = productionLines;
	}
}
