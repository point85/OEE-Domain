package org.point85.domain.dto;

import java.util.HashSet;
import java.util.Set;

import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.script.EventResolver;

public class EquipmentDto extends PlantEntityDto {
	private Set<EquipmentMaterialDto> equipmentMaterials = new HashSet<>();

	private Set<EventResolverDto> eventResolvers = new HashSet<>();

	public EquipmentDto(String name, String description) {
		super(name, description, EntityLevel.EQUIPMENT.name());
	}

	public EquipmentDto(Equipment equipment) {
		super(equipment);

		// materials
		for (EquipmentMaterial equipmentMaterial : equipment.getEquipmentMaterials()) {
			equipmentMaterials.add(new EquipmentMaterialDto(equipmentMaterial));
		}

		// event resolvers
		for (EventResolver resolver : equipment.getScriptResolvers()) {
			eventResolvers.add(new EventResolverDto(resolver));
		}
	}

	public Set<EquipmentMaterialDto> getEquipmentMaterials() {
		return equipmentMaterials;
	}

	public void setEquipmentMaterials(Set<EquipmentMaterialDto> equipmentMaterials) {
		this.equipmentMaterials = equipmentMaterials;
	}

	public Set<EventResolverDto> getEventResolvers() {
		return eventResolvers;
	}

	public void setEventResolvers(Set<EventResolverDto> eventResolvers) {
		this.eventResolvers = eventResolvers;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
