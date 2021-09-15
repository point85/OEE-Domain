package org.point85.domain.dto;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for a recorded OEE event
 */
public class OeeEventDto {
	@SerializedName(value = "type")
	private String eventType;

	@SerializedName(value = "equipment")
	private String equipmentName;

	@SerializedName(value = "source")
	private String sourceId;

	@SerializedName(value = "input")
	private String inputValue;

	@SerializedName(value = "output")
	private String outputValue;

	@SerializedName(value = "reason")
	private String reason;

	@SerializedName(value = "start")
	private String startTimestamp;

	@SerializedName(value = "end")
	private String endTimestamp;

	@SerializedName(value = "duration")
	private Long duration;

	@SerializedName(value = "job")
	private String job;

	@SerializedName(value = "collector")
	private String collector;

	@SerializedName(value = "amount")
	private Double amount;

	@SerializedName(value = "lostTime")
	private Long lostTime;

	@SerializedName(value = "material")
	private String material;

	@SerializedName(value = "shift")
	private String shift;

	@SerializedName(value = "team")
	private String team;

	@SerializedName(value = "uom")
	private String uom;

	public OeeEventDto(OeeEvent event) {
		this.eventType = event.getEventType() != null ? event.getEventType().serialize() : null;
		this.equipmentName = event.getEquipment() != null ? event.getEquipment().getName() : null;
		this.sourceId = event.getSourceId();
		this.inputValue = event.getInputValue() != null ? event.getInputValue().toString() : null;
		this.setOutputValue(event.getOutputValue() != null ? event.getOutputValue().toString() : null);
		this.reason = event.getReason() != null ? event.getReason().getName() : null;
		this.startTimestamp = DomainUtils.offsetDateTimeToString(event.getStartTime(),
				DomainUtils.OFFSET_DATE_TIME_8601);
		this.endTimestamp = DomainUtils.offsetDateTimeToString(event.getEndTime(), DomainUtils.OFFSET_DATE_TIME_8601);
		this.duration = event.getDuration() != null ? event.getDuration().getSeconds() : null;
		this.job = event.getJob();
		this.collector = event.getCollector();
		this.amount = event.getAmount();
		this.lostTime = (event.getLostTime() != null ? event.getLostTime().getSeconds() : null);
		this.material = (event.getMaterial() != null ? event.getMaterial().getName() : null);
		this.shift = (event.getShift() != null ? event.getShift().getName() : null);
		this.team = (event.getTeam() != null ? event.getTeam().getName() : null);
		this.uom = (event.getUOM() != null ? event.getUOM().getSymbol() : null);
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getValue() {
		return inputValue;
	}

	public void setValue(String value) {
		this.inputValue = value;
	}

	public String getStartTimestamp() {
		return startTimestamp;
	}

	public void setStartTimestamp(String timestamp) {
		this.startTimestamp = timestamp;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public String getEndTimestamp() {
		return endTimestamp;
	}

	public void setEndTimestamp(String endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	public String getEquipmentName() {
		return equipmentName;
	}

	public void setEquipmentName(String equipmentName) {
		this.equipmentName = equipmentName;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("source id: " + sourceId);
		sb.append(", equipment: " + equipmentName);
		sb.append(", event: " + eventType);
		sb.append(", value: " + inputValue);
		sb.append(", start timestamp: " + startTimestamp);
		sb.append(", end timestamp: " + endTimestamp);
		sb.append(", duration: " + duration);
		sb.append(", reason: " + reason);

		return sb.toString();
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String getOutputValue() {
		return outputValue;
	}

	public void setOutputValue(String outputValue) {
		this.outputValue = outputValue;
	}

	public String getCollector() {
		return collector;
	}

	public void setCollector(String collector) {
		this.collector = collector;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Long getLostTime() {
		return lostTime;
	}

	public void setLostTime(Long lostTime) {
		this.lostTime = lostTime;
	}

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	public String getShift() {
		return shift;
	}

	public void setShift(String shift) {
		this.shift = shift;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}
}
