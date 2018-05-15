package org.point85.domain.plant;

import java.time.Duration;
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
@DiscriminatorColumn(name = "HIER_LEVEL", discriminatorType = DiscriminatorType.STRING)
@AttributeOverride(name = "primaryKey", column = @Column(name = "ENT_KEY"))

public class PlantEntity extends NamedObject {
	public static final String ROOT_ENTITY_NAME = "All Entities";

	// parent object in the S95 hierarchy
	@ManyToOne
	@JoinColumn(name = "PARENT_KEY")
	private PlantEntity parent;

	// children
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<PlantEntity> children = new HashSet<>();

	// level in the hierarchy
	@Column(name = "HIER_LEVEL", insertable = false, updatable = false)
	@Convert(converter = EntityLevelConverter.class)
	private EntityLevel level;

	// work schedule
	@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "WS_KEY")
	private WorkSchedule workSchedule;

	// retention period for database records
	@Column(name = "RETENTION")
	private Duration retentionDuration;

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

	public WorkSchedule findWorkSchedule() {
		WorkSchedule schedule = workSchedule;

		if (schedule == null) {
			if (parent != null) {
				schedule = parent.findWorkSchedule();
			}
		}
		return schedule;
	}

	public Duration getRetentionDuration() {
		return retentionDuration;
	}

	public void setRetentionDuration(Duration retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	public Duration findDurationPeriod() {
		Duration duration = retentionDuration;

		if (duration == null) {
			if (parent != null) {
				duration = parent.findDurationPeriod();
			}
		}
		return duration;
	}

	@Override
	public String toString() {
		String parentName = parent != null ? parent.getName() : "none";
		return super.toString() + ", Level: " + getLevel() + ", Parent: " + parentName;
	}
}
