package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object (DTO) for a plant entity (e.g. Equipment}
 */
public class PlantEntityDto extends NamedDto {
	private String parent;

	private List<PlantEntityDto> children = new ArrayList<>();

	private String level;

	public PlantEntityDto() {
		super();
	}

	public PlantEntityDto(String name, String description, String level) {
		super(name, description);
		this.level = level;
	}

	public List<PlantEntityDto> getChildren() {
		return children;
	}

	public void setChildren(List<PlantEntityDto> children) {
		this.children = children;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", Level: " + level + ", Parent: " + parent);
		sb.append('\n');

		for (PlantEntityDto child : getChildren()) {
			sb.append('\t').append(child.toString());
		}
		return sb.toString();
	}
}
