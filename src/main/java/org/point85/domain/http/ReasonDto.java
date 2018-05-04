package org.point85.domain.http;

import java.util.ArrayList;
import java.util.List;

public class ReasonDto extends NamedDto {
	private String parent;
	private String lossCategory;

	private List<ReasonDto> children = new ArrayList<>();

	public ReasonDto() {
		super();
	}

	public ReasonDto(String name, String description, String lossCategory) {
		super(name, description);
		this.lossCategory = lossCategory;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public List<ReasonDto> getChildren() {
		return children;
	}

	public void setChildren(List<ReasonDto> children) {
		this.children = children;
	}
	
	public String getLossCategory() {
		return lossCategory;
	}

	public void setLossCategory(String lossCategory) {
		this.lossCategory = lossCategory;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", Parent: " + parent);
		sb.append('\n');

		for (ReasonDto child : getChildren()) {
			sb.append('\t').append(child.toString());
		}
		return sb.toString();
	}
}
