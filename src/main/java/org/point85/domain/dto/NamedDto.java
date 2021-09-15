package org.point85.domain.dto;

/**
 * Base class for named DTOs
 */
abstract class NamedDto {
	private String name;
	
	private String description;
	
	protected NamedDto() {
		// nothing to do
	}
	
	protected NamedDto(String name, String description) {
		this.name = name;
		this.description = description;	
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
