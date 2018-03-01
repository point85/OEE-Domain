package org.point85.domain.http;

public class NamedDto {
	private String name;
	
	private String description;
	
	protected NamedDto() {
		
	}
	
	protected NamedDto(String name, String description) {
		this.setName(name);
		this.setDescription(description);	
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
		StringBuffer sb = new StringBuffer();
		sb.append(name).append(" (").append(description).append(") ");
		return sb.toString();
	}
}
