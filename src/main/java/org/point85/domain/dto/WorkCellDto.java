package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.WorkCell;

public class WorkCellDto extends PlantEntityDto {
	private List<EquipmentDto> equipment = new ArrayList<>();

	public WorkCellDto(String name, String description) {
		super(name, description, EntityLevel.WORK_CELL.name());
	}

	public WorkCellDto(WorkCell cell) {
		super(cell);
	}

	public List<EquipmentDto> getEquipment() {
		return equipment;
	}

	public void setEquipment(List<EquipmentDto> equipment) {
		this.equipment = equipment;
	}

	@Override
	public String toString() {
		String base = super.toString();

		StringBuilder sb = new StringBuilder();
		sb.append('\n');

		for (EquipmentDto child : getEquipment()) {
			sb.append('\t').append(child.toString());
		}
		sb.append(base);

		return sb.toString();
	}
}
