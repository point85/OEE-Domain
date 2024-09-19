package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.point85.domain.plant.EntitySchedule;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.script.EventResolver;

/**
 * Data Transfer Object (DTO) for a plant entity (e.g. Equipment}
 */
public class PlantEntityDto extends NamedObjectDto {
	private String parent;

	private List<String> children = new ArrayList<>();

	private String level;

	private Set<EntityScheduleDto> entitySchedules = new HashSet<>();

	private Set<EventResolverDto> eventResolvers = new HashSet<>();

	private Long retentionDuration;

	protected PlantEntityDto(String name, String description, String level) {
		super(name, description);
		this.level = level;
	}

	public PlantEntityDto(PlantEntity entity) {
		super(entity.getName(), entity.getDescription());
		this.level = entity.getLevel().name();
		this.parent = entity.getParent() != null ? entity.getParent().getName() : null;

		// children
		for (PlantEntity child : entity.getChildren()) {
			children.add(child.getName());
		}

		// work schedules
		for (EntitySchedule entitySchedule : entity.getSchedules()) {
			this.entitySchedules.add(new EntityScheduleDto(entitySchedule));
		}

		// event resolvers
		for (EventResolver resolver : entity.getScriptResolvers()) {
			eventResolvers.add(new EventResolverDto(resolver));
		}

		this.setRetentionDuration(
				entity.getRetentionDuration() != null ? entity.getRetentionDuration().getSeconds() : null);
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public Set<EventResolverDto> getEventResolvers() {
		return eventResolvers;
	}

	public void setEventResolvers(Set<EventResolverDto> eventResolvers) {
		this.eventResolvers = eventResolvers;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public Set<EntityScheduleDto> getEntitySchedules() {
		return this.entitySchedules;
	}

	public Long getRetentionDuration() {
		return retentionDuration;
	}

	public void setRetentionDuration(Long retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", Level: " + level + ", Parent: " + parent);
		sb.append('\n');

		for (String child : getChildren()) {
			sb.append('\t').append(child);
		}
		return sb.toString();
	}
}
