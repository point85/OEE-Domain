package org.point85.domain.plant;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.persistence.EntityLevelConverter;
import org.point85.domain.schedule.WorkSchedule;

/**
 * The PlantEntity class is an object in the S95 hierarchy (Enterprise, Site,
 * Area, ProductionLine, WorkCell or Equipment). It is also a class for each
 * type.
 * 
 * @author Kent Randall
 *
 */

@Entity
@Table(name = "PLANT_ENTITY")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "LEVEL", discriminatorType = DiscriminatorType.STRING)
@AttributeOverride(name = "primaryKey", column = @Column(name = "ENT_KEY"))

@NamedQueries({
		@NamedQuery(name = PlantEntity.ENTITY_BY_NAME, query = "SELECT ent FROM PlantEntity ent WHERE ent.name = :name"),
		@NamedQuery(name = PlantEntity.ENTITY_NAMES, query = "SELECT ent.name FROM PlantEntity ent"),
		@NamedQuery(name = PlantEntity.ENTITY_KEY_BY_NAME, query = "SELECT ent.primaryKey, ent.version FROM PlantEntity ent WHERE ent.name = :name"),
		@NamedQuery(name = PlantEntity.ENTITY_BY_NAME_LIST, query = "SELECT ent FROM PlantEntity ent WHERE ent.name IN :names"),
		@NamedQuery(name = PlantEntity.ENTITY_CHILDREN, query = "SELECT ent FROM PlantEntity ent WHERE ent.parent = :parent"),
		@NamedQuery(name = PlantEntity.ENTITY_ROOTS, query = "SELECT ent FROM PlantEntity ent WHERE ent.parent IS NULL"),
		@NamedQuery(name = PlantEntity.ENTITY_ROOT_NAMES, query = "SELECT ent.name FROM PlantEntity ent WHERE ent.parent IS NULL"),
		@NamedQuery(name = PlantEntity.ENTITY_ALL, query = "SELECT ent FROM PlantEntity ent"), })
public class PlantEntity extends NamedObject {
	public static final String ROOT_ENTITY_NAME = "All Entities";

	// named queries
	public static final String ENTITY_BY_NAME = "ENTITY.ByName";
	public static final String ENTITY_NAMES = "ENTITY.Names";
	public static final String ENTITY_KEY_BY_NAME = "ENTITY.KeyByName";
	public static final String ENTITY_BY_NAME_LIST = "ENTITY.ByNameList";
	public static final String ENTITY_CHILDREN = "ENTITY.Children";
	public static final String ENTITY_ROOTS = "ENTITY.Roots";
	public static final String ENTITY_ROOT_NAMES = "ENTITY.RootNames";
	public static final String ENTITY_ALL = "ENTITY.All";

	// parent object in the S95 hierarchy
	@ManyToOne
	@JoinColumn(name = "PARENT_KEY")
	private PlantEntity parent;

	// children
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<PlantEntity> children = new HashSet<>();

	// level in the hierarchy
	@Column(name = "LEVEL", insertable = false, updatable = false)
	@Convert(converter = EntityLevelConverter.class)
	private EntityLevel level;

	// work schedule
	@OneToOne
	@JoinColumn(name = "WS_KEY")
	private WorkSchedule workSchedule;

	// flag for root entity in the hierarchy
	@Column(name = "IS_ROOT")
	private Boolean isRoot = false;

	public PlantEntity() {
		super();
	}

	public PlantEntity(String name, String description, EntityLevel nodeLevel) {
		super(name, description);
		setLevel(nodeLevel);
	}

	public PlantEntity getParent() {
		return this.parent;
	}

	public void setParent(PlantEntity parent) {
		this.parent = parent;
	}

	public Set<PlantEntity> getChildren() {
		return this.children;
	}

	public void addChild(PlantEntity child) {
		if (!children.contains(child)) {
			children.add(child);
			child.setParent(this);
		}
	}

	public void removeChild(PlantEntity child) {
		if (children.contains(child)) {
			children.remove(child);
			child.setParent(null);
		}
	}

	public EntityLevel getLevel() {
		return level;
	}

	public void setLevel(EntityLevel level) {
		this.level = level;
	}

	public WorkSchedule getWorkSchedule() {
		return this.workSchedule;
	}

	public void setWorkSchedule(WorkSchedule schedule) {
		this.workSchedule = schedule;
	}

	/*
	 * @Override public String getFetchQueryName() { return ENTITY_BY_NAME; }
	 */
	public Boolean isRoot() {
		return this.isRoot;
	}

	public void setIsRoot(Boolean isRoot) {
		this.isRoot = isRoot;
	}

	@Override
	public String toString() {
		String parentName = parent != null ? parent.getName() : "none";
		return super.toString() + ", Level: " + getLevel() + ", Parent: " + parentName;
	}
}
