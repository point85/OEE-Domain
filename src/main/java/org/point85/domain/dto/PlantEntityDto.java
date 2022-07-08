package org.point85.domain.dto;

import java.util.HashSet;
import java.util.Set;

import org.point85.domain.plant.EntitySchedule;
import org.point85.domain.plant.PlantEntity;

/**
 * Data Transfer Object (DTO) for a plant entity (e.g. Equipment}
 */
public class PlantEntityDto extends NamedObjectDto {
	private String parent;

	private String level;

	private Set<EntityScheduleDto> entitySchedules = new HashSet<>();

	private Long retentionDuration;

	protected PlantEntityDto(String name, String description, String level) {
		super(name, description);
		this.level = level;
	}

	protected PlantEntityDto(PlantEntity entity) {
		super(entity.getName(), entity.getDescription());
		this.level = entity.getLevel().name();

		for (EntitySchedule entitySchedule : entity.getSchedules()) {
			this.entitySchedules.add(new EntityScheduleDto(entitySchedule));
		}

		this.setRetentionDuration(
				entity.getRetentionDuration() != null ? entity.getRetentionDuration().getSeconds() : null);
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
		return sb.toString();
	}
}
