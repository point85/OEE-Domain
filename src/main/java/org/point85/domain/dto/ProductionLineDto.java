package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.ProductionLine;

public class ProductionLineDto extends PlantEntityDto {
	private List<WorkCellDto> workCells = new ArrayList<>();

	public ProductionLineDto(String name, String description) {
		super(name, description, EntityLevel.PRODUCTION_LINE.name());
	}

	public ProductionLineDto(ProductionLine line) {
		super(line);
	}

	public List<WorkCellDto> getWorkCells() {
		return workCells;
	}

	public void setWorkCells(List<WorkCellDto> workCells) {
		this.workCells = workCells;
	}
}
