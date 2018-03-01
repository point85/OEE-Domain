package org.point85.domain.http;

import java.util.ArrayList;
import java.util.List;

public class ReasonDto extends NamedDto {
	private String parent;

	private List<ReasonDto> children = new ArrayList<>();

	public ReasonDto() {
		super();
	}

	public ReasonDto(String name, String description) {
		super(name, description);
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

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString()).append(", Parent: " + parent);
		sb.append('\n');

		for (ReasonDto child : getChildren()) {
			sb.append('\t').append(child.toString());
		}
		return sb.toString();
	}

}
