package org.point85.domain.collector;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.persistence.EventTypeConverter;
import org.point85.domain.persistence.OffsetTimestamp;
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
	private static final int MAX_INPUT_VALUE = 64;

	@Column(name = "EVENT_TYPE")
	@Convert(converter = EventTypeConverter.class)
	private OeeEventType eventType;

	@OneToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "localDateTime", column = @Column(name = "START_TIME")),
			@AttributeOverride(name = "utcOffset", column = @Column(name = "START_TIME_OFFSET")) })
	private OffsetTimestamp startTime;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "localDateTime", column = @Column(name = "END_TIME")),
			@AttributeOverride(name = "utcOffset", column = @Column(name = "END_TIME_OFFSET")) })
	private OffsetTimestamp endTime;

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

	// source identifier
	@Column(name = "SOURCE_ID")
	private String sourceId;

	// computed lost time
	private transient Duration lostTime;

	// output value
	private transient Object outputValue;

	public OeeEvent() {
		super();
	}

	public OeeEvent(Equipment equipment) {
		super();
		this.equipment = equipment;
	}

	public OeeEvent(Equipment equipment, Object inputValue, Object outputValue) {
		super();
		this.equipment = equipment;

		String inValue = null;

		if (inputValue.getClass().isArray()) {
			inValue = "[0] = " + ((Object[]) inputValue)[0];
		} else {
			inValue = inputValue.toString();
		}
		this.input = inValue;

		this.outputValue = outputValue;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public OffsetTimestamp getOffsetStartTime() {
		return startTime;
	}

	public void setOffsetStartTime(OffsetTimestamp offsetTime) {
		startTime = offsetTime;
	}

	public OffsetTimestamp getOffsetEndTime() {
		return endTime;
	}

	public void setOffsetEndTime(OffsetTimestamp offsetTime) {
		this.endTime = offsetTime;
	}

	public OffsetDateTime getStartTime() {
		return startTime != null ? startTime.toOffsetDateTime() : null;
	}

	public void setStartTime(OffsetDateTime dateTime) {
		if (dateTime != null) {
			this.startTime = new OffsetTimestamp(dateTime);
		}
	}

	public OffsetDateTime getEndTime() {
		return endTime != null ? endTime.toOffsetDateTime() : null;
	}

	public void setEndTime(OffsetDateTime dateTime) {
		if (dateTime != null) {
			this.endTime = new OffsetTimestamp(dateTime);
		}
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

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public Object getInputValue() {
		return input;
	}

	public void setInputValue(String inputValue) {
		String value = inputValue;
		if (inputValue.length() > MAX_INPUT_VALUE) {
			value = inputValue.substring(0, MAX_INPUT_VALUE - 1);
		}
		this.input = value;
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
		return "Input: " + input + ", Start: " + startTime + ", End: " + endTime + ", Type: " + eventType
				+ ", Material: " + material + ", Job:" + job + ", Reason: " + reason;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}
}
