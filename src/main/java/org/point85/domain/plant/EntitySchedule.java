package org.point85.domain.plant;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.DomainUtils;
import org.point85.domain.dto.EntityScheduleDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.schedule.WorkSchedule;

@Entity
@Table(name = "ENTITY_SCHEDULE")
@AttributeOverride(name = "primaryKey", column = @Column(name = "ES_KEY"))

public class EntitySchedule extends KeyedObject implements Comparable<EntitySchedule> {
	// starting effective date and time of day, assumed to repeat yearly
	@Column(name = "START_DATE_TIME")
	private LocalDateTime startDateTime = LocalDateTime.now();

	// ending effective date and time of day, assumed to repeat yearly
	@Column(name = "END_DATE_TIME")
	private LocalDateTime endDateTime = LocalDateTime.now();

	// owning plant entity
	@ManyToOne
	@JoinColumn(name = "ENT_KEY")
	private PlantEntity plantEntity;

	@OneToOne
	@JoinColumn(name = "WS_KEY")
	private WorkSchedule workSchedule;

	public EntitySchedule() {
		super();
	}

	public EntitySchedule(PlantEntity entity, WorkSchedule schedule, LocalDateTime startDateTime,
			LocalDateTime endDateTime) {
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.plantEntity = entity;
		this.workSchedule = schedule;
	}

	public EntitySchedule(EntityScheduleDto dto) throws Exception {
		if (dto.getWorkSchedule() != null) {
			WorkSchedule schedule = PersistenceService.instance().fetchWorkScheduleByName(dto.getWorkSchedule());

			if (schedule == null) {
				throw new Exception(
						DomainLocalizer.instance().getErrorString("no.work.schedule", dto.getWorkSchedule()));
			}

			this.workSchedule = schedule;
		}

		this.startDateTime = dto.getStartDateTime() != null
				? DomainUtils.localDateTimeFromString(dto.getStartDateTime(), DomainUtils.LOCAL_DATE_TIME_8601)
				: null;

		this.endDateTime = dto.getEndDateTime() != null
				? DomainUtils.localDateTimeFromString(dto.getEndDateTime(), DomainUtils.LOCAL_DATE_TIME_8601)
				: null;
	}

	/**
	 * Get effective start date and time
	 * 
	 * @return Start date and time
	 */
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	/**
	 * Set effective start date and time
	 * 
	 * @param startDateTime period start
	 * @throws Exception exception
	 */
	public void setStartDateTime(LocalDateTime startDateTime) throws Exception {
		if (startDateTime == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("start.not.defined"));
		}

		this.startDateTime = startDateTime;
	}

	/**
	 * Get effective end date and time
	 * 
	 * @return Start date and time
	 */
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	/**
	 * Set effective end date and time
	 * 
	 * @param endDateTime period end
	 * @throws Exception exception
	 */
	public void setEndDateTime(LocalDateTime endDateTime) throws Exception {
		if (endDateTime == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("end.not.defined"));
		}

		this.endDateTime = endDateTime;
	}

	public PlantEntity getPlantEntity() {
		return plantEntity;
	}

	public void setPlantEntity(PlantEntity plantEntity) {
		this.plantEntity = plantEntity;
	}

	public WorkSchedule getWorkSchedule() {
		return workSchedule;
	}

	public void setWorkSchedule(WorkSchedule workSchedule) {
		this.workSchedule = workSchedule;
	}

	@Override
	public int compareTo(EntitySchedule other) {
		return getStartDateTime().compareTo(other.getStartDateTime());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getWorkSchedule(), getPlantEntity());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntitySchedule) {
			EntitySchedule other = (EntitySchedule) obj;

			WorkSchedule thisSchedule = getWorkSchedule();
			WorkSchedule otherSchedule = other.getWorkSchedule();

			PlantEntity thisEntity = getPlantEntity();
			PlantEntity otherEntity = other.getPlantEntity();

			if (thisSchedule != null && otherSchedule != null && thisEntity != null && otherEntity != null) {
				return thisSchedule.equals(otherSchedule) && thisEntity.equals(otherEntity);
			}
		}
		return false;
	}
}
