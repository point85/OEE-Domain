package org.point85.domain.plant;

import java.time.LocalDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.i18n.DomainLocalizer;
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
}
