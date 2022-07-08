package org.point85.domain.dto;

import org.point85.domain.plant.NamedObject;
import org.point85.domain.schedule.Named;

/**
 * Base class for named DTOs
 */
public abstract class NamedObjectDto {
	private String name;

	private String description;

	protected NamedObjectDto() {
		// nothing to do
	}

	protected NamedObjectDto(String name, String description) {
		this.name = name;
		this.description = description;
	}

	protected NamedObjectDto(NamedObject namedObject) {
		this.name = namedObject.getName();
		this.description = namedObject.getDescription();
	}
	
	protected NamedObjectDto(Named named) {
		this.name = named.getName();
		this.description = named.getDescription();
	}	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" (").append(description).append(") ");
		return sb.toString();
	}
}
