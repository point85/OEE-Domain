package org.point85.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeEventType;

public class OeeEquipmentEvent {
	private String sourceId; 
	private Object dataValue; 
	private OffsetDateTime startTimestamp;
	private OffsetDateTime endTimestamp;
	private Reason reason;
	private Equipment equipment;
	private Duration duration; 
	private OeeEventType eventType;
	private String job;
	
	public OeeEquipmentEvent (String sourceId, Object dataValue, OffsetDateTime startTimestamp) {
		this.sourceId = sourceId;
		this.dataValue = dataValue;
		this.startTimestamp = startTimestamp;
	}
	
	public String getSourceId() {
		return sourceId;
	}
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	public Object getDataValue() {
		return dataValue;
	}
	public void setDataValue(Object dataValue) {
		this.dataValue = dataValue;
	}
	public OffsetDateTime getStartTimestamp() {
		return startTimestamp;
	}
	public void setStartTimestamp(OffsetDateTime startTimestamp) {
		this.startTimestamp = startTimestamp;
	}
	public OffsetDateTime getEndTimestamp() {
		return endTimestamp;
	}
	public void setEndTimestamp(OffsetDateTime endTimestamp) {
		this.endTimestamp = endTimestamp;
	}
	public Reason getReason() {
		return reason;
	}
	public void setReason(Reason reason) {
		this.reason = reason;
	}
	public Equipment getEquipment() {
		return equipment;
	}
	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}
	public Duration getDuration() {
		return duration;
	}
	public void setDuration(Duration duration) {
		this.duration = duration;
	}
	public OeeEventType getEventType() {
		return eventType;
	}
	public void setEventType(OeeEventType eventType) {
		this.eventType = eventType;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

}
