package org.point85.domain.collector;

import java.time.OffsetDateTime;

import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.uom.Quantity;

public class LossSummary {
	private EventResolverType resolverType;
	private OffsetDateTime startTime;
	private OffsetDateTime endTime;
	private Reason reason;
	private Quantity quantity;
	private Equipment equipment;
	private Material material;

	public LossSummary() {

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

	public void setResolverType(EventResolverType resolverType) {
		this.resolverType = resolverType;
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

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}

	public Quantity getQuantity() {
		return quantity;
	}

	public void setQuantity(Quantity quantity) {
		this.quantity = quantity;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

}
