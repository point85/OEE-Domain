package org.point85.domain.script;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.point85.domain.collector.AvailabilityRecord;
import org.point85.domain.collector.BaseRecord;
import org.point85.domain.collector.ProductionRecord;
import org.point85.domain.collector.SetupRecord;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.Shift;
import org.point85.domain.uom.Quantity;

public class ResolvedEvent {
	private EventResolverType resolverType;
	private Object inputValue;
	private Object outputValue;
	private String itemId;
	private OffsetDateTime startTime;
	private OffsetDateTime endTime;
	private Duration duration;
	private Reason reason;
	private String job;
	private Material material;
	private Quantity quantity;
	private Equipment equipment;
	private Shift shift;
	private Duration lostTime;
	private Long recordKey;

	public ResolvedEvent(Equipment equipment) {
		this.equipment = equipment;
	}

	public ResolvedEvent(BaseRecord baseRecord) {
		this.setRecordKey(baseRecord.getKey());
		
		this.resolverType = baseRecord.getType();
		this.equipment = baseRecord.getEquipment();
		this.startTime = baseRecord.getStartTime();
		this.endTime = baseRecord.getEndTime();
		this.shift = baseRecord.getShift();
		this.lostTime = baseRecord.getLostTime();

		if (baseRecord instanceof AvailabilityRecord) {
			AvailabilityRecord availability = ((AvailabilityRecord) baseRecord);
			this.reason = availability.getReason();

			if (endTime != null) {
				this.duration = availability.getDuration();
			} else {
				this.duration = Duration.between(startTime, OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
			}
		} else if (baseRecord instanceof ProductionRecord) {
			this.quantity = ((ProductionRecord) baseRecord).getQuantity();
		} else if (baseRecord instanceof SetupRecord) {
			this.material = ((SetupRecord) baseRecord).getMaterial();
			this.job = ((SetupRecord) baseRecord).getJob();
		}
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(OffsetDateTime odt) {
		this.startTime = odt;
	}

	public OffsetDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(OffsetDateTime odt) {
		this.endTime = odt;
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
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

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public EventResolverType getResolverType() {
		return resolverType;
	}

	public void setResolverType(EventResolverType type) {
		this.resolverType = type;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public Quantity getQuantity() {
		return quantity;
	}

	public void setQuantity(Quantity quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Timestamp: ").append(startTime.toString()).append(", Equipment: ");
		sb.append(equipment.getName()).append(", Reason: ");
		sb.append((reason != null) ? reason.getName() : "").append(", Material: ");
		sb.append((material != null) ? material.getName() : "").append(", Job: ");
		sb.append((job != null) ? job : "").append(", Qty: ");
		sb.append(quantity != null ? quantity.toString() : "");

		return sb.toString();
	}

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public Duration getLostTime() {
		return lostTime;
	}

	public void setLostTime(Duration lostTime) {
		this.lostTime = lostTime;
	}

	public Long getRecordKey() {
		return recordKey;
	}

	public void setRecordKey(Long recordKey) {
		this.recordKey = recordKey;
	}
}
