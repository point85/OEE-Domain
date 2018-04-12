package org.point85.domain.messaging;

import org.point85.domain.collector.AvailabilityEvent;
import org.point85.domain.collector.BaseEvent;
import org.point85.domain.collector.ProductionEvent;
import org.point85.domain.collector.SetupEvent;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolverType;

public class CollectorResolvedEventMessage extends ApplicationMessage {
	private String equipmentName;
	private EventResolverType resolverType;
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

	public void fromResolvedEvent(BaseEvent event) {
		this.setTimestamp(event.getStartTime());
		this.setResolverType(event.getResolverType());

		// equipment
		this.setEquipmentName(event.getEquipment().getName());

		// reason
		switch (event.getResolverType()) {
		case AVAILABILITY: {
			Reason reason = ((AvailabilityEvent) event).getReason();
			if (reason != null) {
				this.setReasonName(reason.getName());
				this.setReasonDescription(reason.getDescription());
				this.setLoss(reason.getLossCategory());
			}
			break;
		}
		case JOB_CHANGE: {
			// job
			String job = ((SetupEvent) event).getJob();
			this.setJob(job);
			break;
		}
		case MATL_CHANGE: {
			// material
			Material material = ((SetupEvent) event).getMaterial();

			if (material != null) {
				this.setMaterialName(material.getName());
				this.setMaterialDescription(material.getDescription());
			}
			break;
		}
		case OTHER:
			break;

		case PROD_GOOD:
		case PROD_REJECT:
		case PROD_STARTUP: {
			// production quantity
			this.setAmount(((ProductionEvent) event).getAmount());

			if (((ProductionEvent) event).getUOM() != null) {
				this.setUom(((ProductionEvent) event).getUOM().getName());
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

	public EventResolverType getResolverType() {
		return resolverType;
	}

	public void setResolverType(EventResolverType resolverType) {
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
