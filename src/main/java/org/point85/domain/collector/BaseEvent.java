package org.point85.domain.collector;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.point85.domain.persistence.EventResolverTypeConverter;
import org.point85.domain.persistence.OffsetDateTimeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.schedule.Shift;
import org.point85.domain.script.EventResolverType;

@MappedSuperclass
public class BaseEvent extends KeyedObject {
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

	private transient String itemId;

	private transient Object inputValue;
	private transient Object outputValue;

	public BaseEvent() {

	}

	protected BaseEvent(Equipment equipment) {
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

	public EventResolverType getResolverType() {
		return resolverType;
	}

	public void setResolverType(EventResolverType type) {
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

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public Object getInputValue() {
		return inputValue;
	}

	public void setInputValue(Object sourceValue) {
		this.inputValue = sourceValue;
	}

	public Object getOutputValue() {
		return outputValue;
	}

	public void setOutputValue(Object result) {
		this.outputValue = result;
	}

	@Override
	public String toString() {
		return "Start: " + startTime + ", End: " + endTime;
	}
}
