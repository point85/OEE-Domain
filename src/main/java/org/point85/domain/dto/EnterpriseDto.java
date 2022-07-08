package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.EntityLevel;

public class EnterpriseDto extends PlantEntityDto {
	private List<SiteDto> sites = new ArrayList<>();

	public EnterpriseDto(String name, String description) {
		super(name, description, EntityLevel.ENTERPRISE.name());
	}

	public EnterpriseDto(Enterprise enterprise) {
		super(enterprise);
	}

	public List<SiteDto> getSites() {
		return sites;
	}

	public void setSites(List<SiteDto> sites) {
		this.sites = sites;
	}
}
