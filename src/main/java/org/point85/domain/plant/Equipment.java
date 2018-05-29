package org.point85.domain.plant;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@DiscriminatorValue(Equipment.EQUIP_VALUE)

public class Equipment extends PlantEntity {
	public static final String EQUIP_VALUE = "EQ";
	public static final String DEFAULT_NAME = "Equipment";
	public static final String DEFAULT_DESC = "Default equipment";
	public static final Duration MIN_DURATION = Duration.ofSeconds(5);
	
	// default and minimum periods for retaining OEE records in the database
	public static final Duration DEFAULT_RETENTION_PERIOD = Duration.ofDays(30);

	// map by Material
	transient private Map<Material, EquipmentMaterial> equipmentMaterialsMap = new HashMap<>();

	@OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EquipmentMaterial> equipmentMaterials = new HashSet<>();

	// reason resolvers
	@OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EventResolver> eventResolvers = new HashSet<>();

	public Equipment() {
		super();
		setLevel(EntityLevel.EQUIPMENT);
	}

	public Equipment(String name, String description) {
		super(name, description, EntityLevel.EQUIPMENT);
	}

	public WorkCell getWorkCell() {
		return (WorkCell) getParent();
	}

	public void setWorkCell(WorkCell workCell) {
		setParent(workCell);
	}

	private void populateMap() {
		this.equipmentMaterialsMap.clear();

		for (EquipmentMaterial equipmentMaterial : equipmentMaterials) {
			this.equipmentMaterialsMap.put(equipmentMaterial.getMaterial(), equipmentMaterial);
		}
	}

	public EquipmentMaterial getEquipmentMaterial(Material material) {
		if (equipmentMaterialsMap.size() == 0) {
			populateMap();
		}

		EquipmentMaterial equipmentMaterial = null;
		if (material != null) {
			equipmentMaterial = equipmentMaterialsMap.get(material);
		}
		return equipmentMaterial;
	}

	public EquipmentMaterial getDefaultEquipmentMaterial() {

		EquipmentMaterial equipmentMaterial = null;

		Set<EquipmentMaterial> eqms = getEquipmentMaterials();

		if (eqms.size() == 1) {
			equipmentMaterial = eqms.iterator().next();
		} else {
			for (EquipmentMaterial eqm : eqms) {
				if (eqm.isDefault()) {
					equipmentMaterial = eqm;
					break;
				}
			}
		}
		return equipmentMaterial;
	}

	public void addEquipmentMaterial(EquipmentMaterial equipmentMaterial) {
		if (!equipmentMaterials.contains(equipmentMaterial)) {
			this.equipmentMaterials.add(equipmentMaterial);
			this.equipmentMaterialsMap.put(equipmentMaterial.getMaterial(), equipmentMaterial);
		}
	}

	public void removeEquipmentMaterial(EquipmentMaterial equipmentMaterial) {
		if (equipmentMaterials.contains(equipmentMaterial)) {
			this.equipmentMaterials.remove(equipmentMaterial);
			this.equipmentMaterialsMap.remove(equipmentMaterial.getMaterial());
		}
	}

	public Set<EquipmentMaterial> getEquipmentMaterials() {
		return this.equipmentMaterials;
	}

	public void setEquipmentMaterials(Set<EquipmentMaterial> materials) {
		this.equipmentMaterials = materials;
		this.populateMap();

	}

	public Set<EventResolver> getScriptResolvers() {
		return eventResolvers;
	}

	public void setScriptResolvers(Set<EventResolver> resolvers) {
		this.eventResolvers = resolvers;
	}

	public void addScriptResolver(EventResolver resolver) {
		if (!eventResolvers.contains(resolver)) {
			eventResolvers.add(resolver);
			resolver.setEquipment(this);
		}
	}

	public void removeScriptResolver(EventResolver resolver) {
		if (eventResolvers.contains(resolver)) {
			eventResolvers.remove(resolver);
			resolver.setEquipment(null);
		}
	}

	public boolean hasResolver(EventResolver resolver) {
		return eventResolvers.contains(resolver);
	}

	public boolean hasEquipmentMaterial(EquipmentMaterial equipmentMaterial) {
		return equipmentMaterials.contains(equipmentMaterial);
	}

	public UnitOfMeasure getUOM(Material material, OeeEventType resolverType) throws Exception {
		UnitOfMeasure uom = null;
		if (material == null) {
			return uom;
		}
		EquipmentMaterial equipmentMaterial = getEquipmentMaterial(material);

		if (equipmentMaterial != null) {
			switch (resolverType) {
			case PROD_GOOD:
			case PROD_STARTUP:
				// per unit of time
				uom = equipmentMaterial.getRunRateUOM().getDividend();
				break;
			case PROD_REJECT:
				uom = equipmentMaterial.getRejectUOM();
				break;
			default:
				throw new Exception("Invalid resolver type " + resolverType);
			}
		}
		return uom;
	}
}
