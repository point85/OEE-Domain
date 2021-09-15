package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.DomainUtils;
import org.point85.domain.oee.EquipmentLoss;
import org.point85.domain.oee.EquipmentLossManager;
import org.point85.domain.oee.ParetoItem;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.uom.Unit;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for OEE calculations
 */
public class OeeResponseDto {
	@SerializedName(value = "OEE")
	private float oee;

	@SerializedName(value = "performance")
	private float performance;

	@SerializedName(value = "availability")
	private float availability;

	@SerializedName(value = "quality")
	private float quality;

	@SerializedName(value = "equipment")
	private String equipmentName;

	@SerializedName(value = "material")
	private String material;

	@SerializedName(value = "start")
	private String startTimestamp;

	@SerializedName(value = "end")
	private String endTimestamp;

	// net times
	@SerializedName(value = "theoreticalTime")
	private long theoreticalTime;

	@SerializedName(value = "requiredOperationsTime")
	private long requiredOperationsTime;

	@SerializedName(value = "availableTime")
	private long availableTime;

	@SerializedName(value = "scheduledProductionTime")
	private long scheduledProductionTime;

	@SerializedName(value = "productionTime")
	private long productionTime;

	@SerializedName(value = "reportedProductionTime")
	private long reportedProductionTime;

	@SerializedName(value = "netProductionTime")
	private long netProductionTime;

	@SerializedName(value = "efficientTime")
	private long efficientTime;

	@SerializedName(value = "effectiveTime")
	private long effectiveTime;

	@SerializedName(value = "valueAddingTime")
	private long valueAddingTime;

	@SerializedName(value = "noDemandTime")
	private long noDemandTime;

	@SerializedName(value = "unscheduledTime")
	private long unscheduledTime;

	@SerializedName(value = "plannedDowntime")
	private long plannedDowntime;

	@SerializedName(value = "setupTime")
	private long setupTime;

	@SerializedName(value = "unplannedDowntime")
	private long unplannedDowntime;

	@SerializedName(value = "stoppagesTime")
	private long stoppagesTime;

	@SerializedName(value = "reducedSpeedTime")
	private long reducedSpeedTime;

	@SerializedName(value = "rejectsTime")
	private long rejectsTime;

	@SerializedName(value = "startupTime")
	private long startupTime;

	// first level Pareto
	@SerializedName(value = "firstLevelPareto")
	private List<ParetoItemDto> firstLevelPareto = new ArrayList<>();

	// second level Paretos
	@SerializedName(value = "stoppagesPareto")
	private List<ParetoItemDto> stoppagesPareto = new ArrayList<>();

	@SerializedName(value = "startupPareto")
	private List<ParetoItemDto> startupPareto = new ArrayList<>();

	@SerializedName(value = "rejectsPareto")
	private List<ParetoItemDto> rejectsPareto = new ArrayList<>();

	@SerializedName(value = "reducedSpeedPareto")
	private List<ParetoItemDto> reducedSpeedPareto = new ArrayList<>();

	@SerializedName(value = "unplannedDowntimePareto")
	private List<ParetoItemDto> unplannedDowntimePareto = new ArrayList<>();

	@SerializedName(value = "plannedDowntimePareto")
	private List<ParetoItemDto> plannedDowntimePareto = new ArrayList<>();

	@SerializedName(value = "setupPareto")
	private List<ParetoItemDto> setupPareto = new ArrayList<>();

	// production
	@SerializedName(value = "goodQuantity")
	private QuantityDto goodQuantity;

	@SerializedName(value = "rejectQuantity")
	private QuantityDto rejectQuantity;

	@SerializedName(value = "startupQuantity")
	private QuantityDto startupQuantity;

	public OeeResponseDto(EquipmentLoss equipmentLoss) throws Exception {
		// OEE
		this.oee = equipmentLoss.calculateOeePercentage();
		this.performance = equipmentLoss.calculatePerformancePercentage();
		this.availability = equipmentLoss.calculateAvailabilityPercentage();
		this.quality = equipmentLoss.calculateQualityPercentage();

		// equipment
		this.equipmentName = equipmentLoss.getEquipment() != null ? equipmentLoss.getEquipment().getName() : null;

		// material
		this.material = equipmentLoss.getMaterial() != null ? equipmentLoss.getMaterial().getName() : null;

		// time period
		this.startTimestamp = DomainUtils.offsetDateTimeToString(equipmentLoss.getStartDateTime(),
				DomainUtils.OFFSET_DATE_TIME_8601);
		this.endTimestamp = DomainUtils.offsetDateTimeToString(equipmentLoss.getEndDateTime(),
				DomainUtils.OFFSET_DATE_TIME_8601);

		// total elapsed time
		this.theoreticalTime = equipmentLoss.getDuration().getSeconds();

		// value adding + start up and yield
		this.valueAddingTime = equipmentLoss.getValueAddingTime().getSeconds();
		this.startupTime = equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD).getSeconds();

		// effective net production time + rejects and rework
		this.effectiveTime = equipmentLoss.getEffectiveNetProductionTime().getSeconds();
		this.rejectsTime = equipmentLoss.getLoss(TimeLoss.REJECT_REWORK).getSeconds();

		// efficient net production time + reduced speed
		this.efficientTime = equipmentLoss.getEfficientNetProductionTime().getSeconds();
		this.reducedSpeedTime = equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED).getSeconds();

		// net production time + minor stoppages
		this.netProductionTime = equipmentLoss.getNetProductionTime().getSeconds();
		this.stoppagesTime = equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES).getSeconds();

		// reported production time + unplanned downtime
		this.reportedProductionTime = equipmentLoss.getReportedProductionTime().getSeconds();
		this.unplannedDowntime = equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME).getSeconds();

		// production time + setup time
		this.productionTime = equipmentLoss.getProductionTime().getSeconds();
		this.setupTime = equipmentLoss.getLoss(TimeLoss.SETUP).getSeconds();

		// scheduled production time + planned downtime
		this.scheduledProductionTime = equipmentLoss.getScheduledProductionTime().getSeconds();
		this.plannedDowntime = equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME).getSeconds();

		// available time + planned downtime
		this.availableTime = equipmentLoss.getAvailableTime().getSeconds();
		this.unscheduledTime = equipmentLoss.getLoss(TimeLoss.UNSCHEDULED).getSeconds();

		// first-level pareto items
		List<ParetoItem> items = equipmentLoss.getLossItems(Unit.SECOND);

		for (ParetoItem item : items) {
			firstLevelPareto.add(new ParetoItemDto(item));
		}

		// second-level pareto items
		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.MINOR_STOPPAGES);
		for (ParetoItem item : items) {
			stoppagesPareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.PLANNED_DOWNTIME);
		for (ParetoItem item : items) {
			plannedDowntimePareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REDUCED_SPEED);
		for (ParetoItem item : items) {
			reducedSpeedPareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REJECT_REWORK);
		for (ParetoItem item : items) {
			rejectsPareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.SETUP);
		for (ParetoItem item : items) {
			setupPareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.STARTUP_YIELD);
		for (ParetoItem item : items) {
			startupPareto.add(new ParetoItemDto(item));
		}

		items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.UNPLANNED_DOWNTIME);
		for (ParetoItem item : items) {
			unplannedDowntimePareto.add(new ParetoItemDto(item));
		}

		// production quantities
		if (equipmentLoss.getGoodQuantity() != null) {
			this.goodQuantity = new QuantityDto(equipmentLoss.getGoodQuantity());
		}

		if (equipmentLoss.getStartupQuantity() != null) {
			this.startupQuantity = new QuantityDto(equipmentLoss.getStartupQuantity());
		}

		if (equipmentLoss.getRejectQuantity() != null) {
			this.rejectQuantity = new QuantityDto(equipmentLoss.getRejectQuantity());
		}

	}

	public String getStartTimestamp() {
		return startTimestamp;
	}

	public void setStartTimestamp(String timestamp) {
		this.startTimestamp = timestamp;
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

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("material: " + material);
		sb.append(", equipment: " + equipmentName);
		sb.append(", start timestamp: " + startTimestamp);
		sb.append(", end timestamp: " + endTimestamp);

		return sb.toString();
	}

	public long getTheoreticalTime() {
		return theoreticalTime;
	}

	public void setTheoreticalTime(long theoreticalTime) {
		this.theoreticalTime = theoreticalTime;
	}

	public long getValueAddingTime() {
		return valueAddingTime;
	}

	public void setValueAddingTime(long valueAddingTime) {
		this.valueAddingTime = valueAddingTime;
	}

	public long getStartupTime() {
		return startupTime;
	}

	public void setStartupTime(long startupTime) {
		this.startupTime = startupTime;
	}

	public long getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(long effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public long getRejectsTime() {
		return rejectsTime;
	}

	public void setRejectsTime(long rejectTime) {
		this.rejectsTime = rejectTime;
	}

	public List<ParetoItemDto> getFirstLevelPareto() {
		return this.firstLevelPareto;
	}

	public void setFirstLevelPareto(List<ParetoItemDto> items) {
		this.firstLevelPareto = items;
	}

	public QuantityDto getGoodQuantity() {
		return goodQuantity;
	}

	public void setGoodQuantity(QuantityDto goodQuantity) {
		this.goodQuantity = goodQuantity;
	}

	public QuantityDto getRejectQuantity() {
		return rejectQuantity;
	}

	public void setRejectQuantity(QuantityDto rejectQuantity) {
		this.rejectQuantity = rejectQuantity;
	}

	public QuantityDto getStartupQuantity() {
		return startupQuantity;
	}

	public void setStartupQuantity(QuantityDto startupQuantity) {
		this.startupQuantity = startupQuantity;
	}
}
