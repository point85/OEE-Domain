package org.point85.domain.plant;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.dto.EnterpriseDto;

@Entity
@DiscriminatorValue(Enterprise.ENTER_VALUE)
public class Enterprise extends PlantEntity {
	public static final String ENTER_VALUE = "ENTER";
	
	public static final String DEFAULT_NAME = "Enterprise";
	public static final String DEFAULT_DESC = "Default enterprise";
	
	public Enterprise() {
		this(ENTER_VALUE, null);
	}

	public Enterprise(String name, String description) {
		super(name, description, EntityLevel.ENTERPRISE);
	}
	
	public Enterprise(EnterpriseDto dto) throws Exception {
		super(dto);
		setLevel(EntityLevel.ENTERPRISE);
		
	}

	public List<Site> getSites() {
		List<Site> sites = new ArrayList<>(getChildren().size());

		for (PlantEntity object : getChildren()) {
			sites.add((Site) object);
		}
		return sites;
	}

	public void addSite(Site site) {
		addChild(site);
	}

	public void removeSite(Site site) {
		removeChild(site);
	}
}
