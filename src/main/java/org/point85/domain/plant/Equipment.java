package org.point85.domain.plant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.point85.domain.collector.SetupHistory;
import org.point85.domain.persistence.PersistencyService;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.script.ScriptResolverType;

@Entity
@DiscriminatorValue(Equipment.EQUIP_VALUE)
@NamedQueries({
		@NamedQuery(name = Equipment.EQUIPMENT_RESOLVERS, query = "SELECT sr FROM ScriptResolver sr JOIN sr.equipment e JOIN sr.dataSource ds"),
		@NamedQuery(name = Equipment.EQUIPMENT_SOURCE_IDS, query = "SELECT sr.sourceId FROM ScriptResolver sr JOIN sr.equipment e JOIN sr.dataSource ds WHERE e.name = :name AND ds.sourceType = :type"), })
public class Equipment extends PlantEntity {
	public static final String EQUIP_VALUE = "EQ";
	public static final String DEFAULT_NAME = "Equipment";
	public static final String DEFAULT_DESC = "Default equipment";

	// named queries
	public static final String EQUIPMENT_RESOLVERS = "EQUIP.Resolvers";
	public static final String EQUIPMENT_SOURCE_IDS = "EQUIP.SourceIds";

	// map by Material
	transient private Map<Material, EquipmentMaterial> equipmentMaterialsMap = new HashMap<>();

	@OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EquipmentMaterial> equipmentMaterials = new HashSet<>();

	// reason resolvers
	@OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<ScriptResolver> scriptResolvers = new HashSet<>();

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

	public Set<ScriptResolver> getScriptResolvers() {
		return scriptResolvers;
	}

	public void setScriptResolvers(Set<ScriptResolver> resolvers) {
		this.scriptResolvers = resolvers;
	}

	public void addScriptResolver(ScriptResolver resolver) {
		if (!scriptResolvers.contains(resolver)) {
			scriptResolvers.add(resolver);
			resolver.setEquipment(this);
		}
	}

	public void removeScriptResolver(ScriptResolver resolver) {
		if (scriptResolvers.contains(resolver)) {
			scriptResolvers.remove(resolver);
			resolver.setEquipment(null);
		}
	}

	public boolean hasResolver(ScriptResolver resolver) {
		return scriptResolvers.contains(resolver);
	}
	
	public boolean hasEquipmentMaterial(EquipmentMaterial equipmentMaterial) {
		return equipmentMaterials.contains(equipmentMaterial);
	}

	public Material getCurrentMaterial() {
		Material material = null;

		SetupHistory history = PersistencyService.instance().fetchLastHistory(this, ScriptResolverType.MATERIAL);

		if (history != null) {
			material = history.getMaterial();
		}

		return material;
	}

	public String getCurrentJob() {
		String job = null;

		SetupHistory history = PersistencyService.instance().fetchLastHistory(this, ScriptResolverType.JOB);

		if (history != null) {
			job = history.getJob();
		}

		return job;
	}

}