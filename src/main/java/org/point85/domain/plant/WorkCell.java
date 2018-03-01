package org.point85.domain.plant;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(WorkCell.CELL_VALUE)
public class WorkCell extends PlantEntity {
	public static final String CELL_VALUE = "WC";
	
	public static final String DEFAULT_NAME = "Work Cell";
	public static final String DEFAULT_DESC = "Default work cell";
	
	public WorkCell() {
		super();
		setLevel(EntityLevel.WORK_CELL);
	}

	public WorkCell(String name, String description) {
		super(name, description, EntityLevel.WORK_CELL);
	}

	public List<Equipment> getEquipment() {
		List<Equipment> equipment = new ArrayList<>(getChildren().size());

		for (PlantEntity object : getChildren()) {
			equipment.add((Equipment) object);
		}
		return equipment;
	}

	public void addEquipment(Equipment equipment) {
		addChild(equipment);
	}

	public void removeEquipment(Equipment equipment) {
		removeChild(equipment);
	}

	public ProductionLine getProductionLine() {
		return (ProductionLine) getParent();
	}

	public void setProductionLine(ProductionLine productionLine) {
		setParent(productionLine);
	}
}
