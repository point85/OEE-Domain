package org.point85.domain.plant;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(Area.AREA_VALUE)
public class Area extends PlantEntity {
	public static final String AREA_VALUE = "AREA";
	
	public static final String DEFAULT_NAME = "Area";
	public static final String DEFAULT_DESC = "Default area";
	
	public Area() {
		super();
		setLevel(EntityLevel.AREA);
	}

	public Area(String name, String description) {
		super(name, description, EntityLevel.AREA);
	}

	public List<ProductionLine> getProductionLines() {
		List<ProductionLine> lines = new ArrayList<>(getChildren().size());

		for (PlantEntity object : getChildren()) {
			lines.add((ProductionLine) object);
		}
		return lines;
	}

	public void addProductionLine(ProductionLine productionLine) {
		addChild(productionLine);
	}

	public void removeProductionLine(ProductionLine productionLine) {
		removeChild(productionLine);
	}

	public Site getSite() {
		return (Site) getParent();
	}

	public void setSite(Site site) {
		setParent(site);
	}
}
