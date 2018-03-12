package org.point85.domain.script;

import java.time.OffsetDateTime;

import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.uom.Quantity;

public class ResolvedEvent {
	private ScriptResolverType resolverType;
	private Object inputValue;
	private Object outputValue;
	private String itemId;
	private OffsetDateTime timestamp;
	private Reason reason;
	private String job;
	private Material material;
	private Quantity quantity;
	private Equipment equipment;

	public ResolvedEvent() {

	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
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

	public ScriptResolverType getResolverType() {
		return resolverType;
	}

	public void setResolverType(ScriptResolverType type) {
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
		sb.append("Timestamp: ").append(timestamp.toString()).append(", Equipment: ");
		sb.append(equipment.getName()).append(", Reason: ");
		sb.append((reason != null) ? reason.getName() : "").append(", Material: ");
		sb.append((material != null) ? material.getName() : "").append(", Job: ");
		sb.append((job != null) ? job : "").append(", Qty: ");
		sb.append(quantity != null ? quantity.toString() : "");

		return sb.toString();
	}
}
