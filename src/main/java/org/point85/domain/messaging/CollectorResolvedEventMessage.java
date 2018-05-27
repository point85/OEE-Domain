package org.point85.domain.messaging;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventType;

public class CollectorResolvedEventMessage extends ApplicationMessage {
	private String equipmentName;
	private EventType resolverType;
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

	public void fromResolvedEvent(OeeEvent event) {
		this.setTimestamp(event.getStartTime());
		this.setResolverType(event.getEventType());

		// equipment
		this.setEquipmentName(event.getEquipment().getName());

		// reason
		switch (event.getEventType()) {
		case AVAILABILITY: {
			Reason reason = event.getReason();
			if (reason != null) {
				this.setReasonName(reason.getName());
				this.setReasonDescription(reason.getDescription());
				this.setLoss(reason.getLossCategory());
			}
			break;
		}
		case JOB_CHANGE: {
			// job
			String job = event.getJob();
			this.setJob(job);
			break;
		}
		case MATL_CHANGE: {
			// material
			Material material = event.getMaterial();

			if (material != null) {
				this.setMaterialName(material.getName());
				this.setMaterialDescription(material.getDescription());
			}
			break;
		}

		case PROD_GOOD:
		case PROD_REJECT:
		case PROD_STARTUP: {
			// production quantity
			this.setAmount(event.getAmount());

			if (event.getUOM() != null) {
				this.setUom(event.getUOM().getName());
			}
			break;
		}
		default:
			break;
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

	public EventType getResolverType() {
		return resolverType;
	}

	public void setResolverType(EventType resolverType) {
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
