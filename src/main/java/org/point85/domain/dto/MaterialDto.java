package org.point85.domain.dto;

import org.point85.domain.plant.Material;

/**
 * Data Transfer Object (DTO) for a material
 */
public class MaterialDto extends NamedObjectDto {
	private String category;

	public MaterialDto() {
		super();
	}

	public MaterialDto(String name, String description, String category) {
		super(name, description);
		this.category = category;
	}

	public MaterialDto(Material material) {
		super(material.getName(), material.getDescription());
		this.category = material.getCategory();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", Category: " + category);
		sb.append('\n');
		return sb.toString();
	}
}
