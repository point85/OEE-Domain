package org.point85.domain.http;

public class MaterialDto extends NamedDto {
	private String category;
	
	public MaterialDto() {
		super();
	}

	public MaterialDto(String name, String description, String category) {
		super(name, description);
		this.setCategory(category);
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
