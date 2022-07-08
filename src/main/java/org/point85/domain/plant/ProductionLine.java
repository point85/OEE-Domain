package org.point85.domain.plant;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.dto.ProductionLineDto;

@Entity
@DiscriminatorValue(ProductionLine.LINE_VALUE)
public class ProductionLine extends PlantEntity {
	public static final String LINE_VALUE = "PL";
	
	public static final String DEFAULT_NAME = "Production Line";
	public static final String DEFAULT_DESC = "Default production line";
	
	public ProductionLine() {
		super();
		setLevel(EntityLevel.PRODUCTION_LINE);
	}

	public ProductionLine(String name, String description) {
		super(name, description, EntityLevel.PRODUCTION_LINE);
	}
	
	public ProductionLine(ProductionLineDto dto) throws Exception  {
		super(dto);
		setLevel(EntityLevel.PRODUCTION_LINE);
	}

	public List<WorkCell> getWorkCells() {
		List<WorkCell> cells = new ArrayList<>(getChildren().size());

		for (PlantEntity object : getChildren()) {
			cells.add((WorkCell) object);
		}
		return cells;
	}

	public void addWorkCell(WorkCell workCell) {
		addChild(workCell);
	}

	public void removeWorkCell(WorkCell workCell) {
		removeChild(workCell);
	}

	public Area getArea() {
		return (Area) getParent();
	}

	public void setArea(Area area) {
		setParent(area);
	}
}
