package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Site;

public class SiteDto extends PlantEntityDto {
	private List<AreaDto> areas = new ArrayList<>();

	public SiteDto(String name, String description) {
		super(name, description, EntityLevel.SITE.name());
	}

	public SiteDto(Site site) {
		super(site);
	}

	public List<AreaDto> getAreas() {
		return areas;
	}

	public void setAreas(List<AreaDto> areas) {
		this.areas = areas;
	}
}
