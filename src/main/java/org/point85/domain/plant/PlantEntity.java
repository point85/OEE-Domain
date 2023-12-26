package org.point85.domain.plant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
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
import javax.persistence.Table;

import org.point85.domain.dto.EntityScheduleDto;
import org.point85.domain.dto.EventResolverDto;
import org.point85.domain.dto.PlantEntityDto;
import org.point85.domain.persistence.EntityLevelConverter;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;

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
	private final Set<PlantEntity> children = new HashSet<>();

	// level in the hierarchy
	@Column(name = "HIER_LEVEL", insertable = false, updatable = false)
	@Convert(converter = EntityLevelConverter.class)
	private EntityLevel level;

	// work schedules
	@OneToMany(mappedBy = "plantEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EntitySchedule> entitySchedules = new HashSet<>();

	// retention period for database records
	@Column(name = "RETENTION")
	private Duration retentionDuration;

	// reason resolvers
	@OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<EventResolver> eventResolvers = new HashSet<>();

	public PlantEntity() {
		super();
	}

	public PlantEntity(String name, String description, EntityLevel nodeLevel) {
		super(name, description);
		this.level = nodeLevel;
	}

	public PlantEntity(PlantEntityDto dto) throws Exception {
		super(dto.getName(), dto.getDescription());

		this.retentionDuration = dto.getRetentionDuration() != null ? Duration.ofSeconds(dto.getRetentionDuration())
				: null;

		for (EntityScheduleDto scheduleDto : dto.getEntitySchedules()) {
			EntitySchedule schedule = new EntitySchedule(scheduleDto);
			schedule.setPlantEntity(this);
			entitySchedules.add(schedule);
		}

		if (dto.getEventResolvers() != null) {
			for (EventResolverDto resolverDto : dto.getEventResolvers()) {
				EventResolver resolver = new EventResolver(resolverDto);
				resolver.setPlantEntity(this);

				eventResolvers.add(resolver);
			}
		}
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

	public Set<EntitySchedule> getSchedules() {
		return this.entitySchedules;
	}

	public void setSchedules(Set<EntitySchedule> schedules) {
		this.entitySchedules = schedules;
	}

	public void addEntitySchedule(EntitySchedule entitySchedule) {
		if (!entitySchedules.contains(entitySchedule)) {
			this.entitySchedules.add(entitySchedule);
		}
	}

	public void removeEntitySchedule(EntitySchedule entitySchedule) {
		if (entitySchedules.contains(entitySchedule)) {
			this.entitySchedules.remove(entitySchedule);
		}
	}

	public WorkSchedule findWorkSchedule() {
		WorkSchedule schedule = null;
		LocalDateTime now = LocalDateTime.now();

		for (EntitySchedule entitySchedule : entitySchedules) {
			if (now.isAfter(entitySchedule.getStartDateTime()) && now.isBefore(entitySchedule.getEndDateTime())) {
				schedule = entitySchedule.getWorkSchedule();
				break;
			}
		}

		if (schedule == null && parent != null) {
			schedule = parent.findWorkSchedule();
		}
		return schedule;
	}

	public Duration getRetentionDuration() {
		return retentionDuration;
	}

	public void setRetentionDuration(Duration retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	public Duration findRetentionPeriod() {
		Duration duration = retentionDuration;

		if (duration == null && parent != null) {
			duration = parent.findRetentionPeriod();
		}
		return duration;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PlantEntity) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getDescription());
	}

	@Override
	public String toString() {
		String parentName = parent != null ? parent.getName() : "none";
		return super.toString() + ", Level: " + getLevel() + ", Parent: " + parentName;
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
			resolver.setPlantEntity(this);
		}
	}

	public void removeScriptResolver(EventResolver resolver) {
		if (eventResolvers.contains(resolver)) {
			eventResolvers.remove(resolver);
			resolver.setPlantEntity(null);
		}
	}

	public boolean hasResolver(EventResolver resolver) {
		return eventResolvers.contains(resolver);
	}
}
