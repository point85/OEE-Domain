package org.point85.domain.dto;

import org.point85.domain.generic.GenericSource;

public class GenericSourceDto extends CollectorDataSourceDto {
	private String attribute1;
	private String attribute2;
	private String attribute3;
	private String attribute4;
	private String attribute5;

	public GenericSourceDto(GenericSource source) {
		super(source);

		this.setAttribute1(source.getAttribute1());
		this.setAttribute2(source.getAttribute2());
		this.setAttribute3(source.getAttribute3());
		this.setAttribute4(source.getAttribute4());
		this.setAttribute5(source.getAttribute5());
	}

	public String getAttribute1() {
		return attribute1;
	}

	public void setAttribute1(String attribute) {
		this.attribute1 = attribute;
	}

	public String getAttribute2() {
		return attribute2;
	}

	public void setAttribute2(String attribute) {
		this.attribute2 = attribute;
	}

	public String getAttribute3() {
		return attribute3;
	}

	public void setAttribute3(String attribute) {
		this.attribute3 = attribute;
	}

	public String getAttribute4() {
		return attribute4;
	}

	public void setAttribute4(String attribute) {
		this.attribute4 = attribute;
	}

	public String getAttribute5() {
		return attribute5;
	}

	public void setAttribute5(String attribute) {
		this.attribute5 = attribute;
	}
}
