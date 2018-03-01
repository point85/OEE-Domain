package org.point85.domain.messaging;

import org.point85.domain.performance.TimeLoss;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.script.ScriptResolverType;
import org.point85.domain.uom.Quantity;

public class CollectorResolvedEventMessage extends ApplicationMessage {
	private String equipmentName;
	private ScriptResolverType resolverType;
	private String reasonName;
	private String reasonDescription;
	private TimeLoss loss;
	private String job;
	private String materialName;
	private String materialDescription;
	private Double amount;
	private String uom;

	public CollectorResolvedEventMessage(String senderHostName, String senderHostAddress) {
		super(senderHostName, senderHostAddress, MessageType.RESOLVED_EVENT);
	}

	public void fromResolvedEvent(ResolvedEvent event) {
		this.setTimestamp(event.getTimestamp());
		this.setResolverType(event.getResolverType());

		// equipment
		this.setEquipmentName(event.getEquipment().getName());

		// reason
		Reason reason = event.getReason();
		if (reason != null) {
			this.setReasonName(reason.getName());
			this.setReasonDescription(reason.getDescription());
			this.setLoss(reason.getLossCategory());
		}

		// job
		this.setJob(event.getJob());

		// material
		Material material = event.getMaterial();

		if (material != null) {
			this.setMaterialName(material.getName());
			this.setMaterialDescription(material.getDescription());
		}

		// production quantity
		Quantity qty = event.getQuantity();

		if (qty != null) {
			this.setAmount(qty.getAmount());

			if (qty.getUOM() != null) {
				this.setUom(qty.getUOM().getName());
			}
		}
	}

	public String getReasonName() {
		return reasonName;
	}

	public void setReasonName(String reasonName) {
		this.reasonName = reasonName;
	}

	public String getReasonDescription() {
		return reasonDescription;
	}

	public void setReasonDescription(String reasonDescription) {
		this.reasonDescription = reasonDescription;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public String getMaterialDescription() {
		return materialDescription;
	}

	public void setMaterialDescription(String materialDescription) {
		this.materialDescription = materialDescription;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getEquipmentName() {
		return equipmentName;
	}

	public void setEquipmentName(String equipmentName) {
		this.equipmentName = equipmentName;
	}

	public ScriptResolverType getResolverType() {
		return resolverType;
	}

	public void setResolverType(ScriptResolverType resolverType) {
		this.resolverType = resolverType;
	}

	public TimeLoss getLoss() {
		return loss;
	}

	public void setLoss(TimeLoss loss) {
		this.loss = loss;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}
}
