package org.point85.domain.collector;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.persistence.EventTypeConverter;
import org.point85.domain.persistence.OffsetDateTimeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@Table(name = "OEE_EVENT")

@AttributeOverride(name = "primaryKey", column = @Column(name = "EVENT_KEY"))

public class OeeEvent extends KeyedObject {
	@Column(name = "EVENT_TYPE")
	@Convert(converter = EventTypeConverter.class)
	private OeeEventType eventType;

	@OneToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;

	@Column(name = "START_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime startTime;

	@Column(name = "END_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime endTime;

	@OneToOne
	@JoinColumn(name = "SHIFT_KEY")
	private Shift shift;

	@OneToOne
	@JoinColumn(name = "TEAM_KEY")
	private Team team;

	@Column(name = "DURATION")
	private Duration duration;

	@OneToOne
	@JoinColumn(name = "REASON_KEY")
	private Reason reason;

	@Column(name = "AMOUNT")
	private Double amount;

	@OneToOne
	@JoinColumn(name = "UOM_KEY")
	private UnitOfMeasure uom;

	@OneToOne
	@JoinColumn(name = "MATL_KEY")
	private Material material;

	@Column(name = "JOB")
	private String job;
	
	@Column(name = "IN_VALUE")
	private String input;

	// computed lost time
	private transient Duration lostTime;

	// source identifier
	private transient String itemId;

	// input value
	private transient Object inputValue;

	// output value
	private transient Object outputValue;

	public OeeEvent() {
		super();
	}

	public OeeEvent(Equipment equipment) {
		this.equipment = equipment;
	}

	public OeeEvent(Equipment equipment, Object inputValue, Object outputValue) {
		this.equipment = equipment;
		this.inputValue = inputValue;
		this.input = inputValue.toString();
		this.outputValue = outputValue;
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

	public OeeEventType getEventType() {
		return eventType;
	}

	public void setEventType(OeeEventType type) {
		this.eventType = type;
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

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public UnitOfMeasure getUOM() {
		return uom;
	}

	public void setUOM(UnitOfMeasure uom) {
		this.uom = uom;
	}

	public Quantity getQuantity() {
		return new Quantity(amount, uom);
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public boolean isAvailability() {
		return eventType.equals(OeeEventType.AVAILABILITY);
	}

	public boolean isProduction() {
		boolean isProduction = false;

		if (eventType.equals(OeeEventType.PROD_GOOD) || eventType.equals(OeeEventType.PROD_REJECT)
				|| eventType.equals(OeeEventType.PROD_STARTUP)) {
			isProduction = true;
		}
		return isProduction;
	}

	public boolean isSetup() {
		boolean isSetup = false;
		if (eventType.equals(OeeEventType.MATL_CHANGE) || eventType.equals(OeeEventType.JOB_CHANGE)) {
			isSetup = true;
		}
		return isSetup;
	}

	@Override
	public String toString() {
		return "Input: " + input + ", Start: " + startTime + ", End: " + endTime + ", Type: " + eventType + ", Material: " + material
				+ ", Job:" + job + ", Reason: " + reason;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}
}
