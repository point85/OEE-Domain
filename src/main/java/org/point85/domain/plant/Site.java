package org.point85.domain.plant;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.dto.SiteDto;

@Entity
@DiscriminatorValue(Site.SITE_VALUE)
public class Site extends PlantEntity {

	public static final String SITE_VALUE = "SITE";

	public static final String DEFAULT_NAME = "Site";
	public static final String DEFAULT_DESC = "Default site";

	public Site() {
		super();
		setLevel(EntityLevel.SITE);
	}

	public Site(String name, String description) {
		super(name, description, EntityLevel.SITE);
	}

	public Site(SiteDto dto) throws Exception {
		super(dto);
		setLevel(EntityLevel.SITE);
	}

	public List<Area> getAreas() {
		List<Area> areas = new ArrayList<>(getChildren().size());

		for (PlantEntity object : getChildren()) {
			areas.add((Area) object);
		}
		return areas;
	}

	public void addArea(Area area) {
		addChild(area);
	}

	public void removeArea(Area area) {
		removeChild(area);
	}

	public Enterprise getEnterprise() {
		return (Enterprise) getParent();
	}

	public void setEnterprise(Enterprise parent) {
		setParent(parent);
	}
}
