package org.point85.domain.collector;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.point85.domain.persistence.EventResolverTypeConverter;
import org.point85.domain.persistence.OffsetDateTimeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.schedule.Shift;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.script.ResolvedEvent;

@MappedSuperclass
public class BaseRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EVENT_KEY")
	private Long primaryKey;

	@OneToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;

	@Column(name = "START_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime startTime;

	@Column(name = "END_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime endTime;

	@Column(name = "EVENT_TYPE")
	@Convert(converter = EventResolverTypeConverter.class)
	private EventResolverType resolverType;

	@OneToOne
	@JoinColumn(name = "SHIFT_KEY")
	private Shift shift;

	// computed lost time
	private transient Duration lostTime;

	protected BaseRecord() {

	}

	protected BaseRecord(ResolvedEvent event) {
		this.equipment = event.getEquipment();
		this.startTime = event.getStartTime();
		this.endTime = event.getEndTime();
		this.resolverType = event.getResolverType();
		this.shift = event.getShift();
	}

	protected BaseRecord(Equipment equipment) {
		this.equipment = equipment;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
	}

	public OffsetDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(OffsetDateTime endTime) {
		this.endTime = endTime;
	}

	public EventResolverType getType() {
		return resolverType;
	}

	public void setType(EventResolverType type) {
		this.resolverType = type;
	}

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public Duration getLostTime() {
		return lostTime;
	}

	public void setLostTime(Duration lostTime) {
		this.lostTime = lostTime;
	}

	@Override
	public String toString() {
		return "Start: " + startTime + ", End: " + endTime;
	}
}
