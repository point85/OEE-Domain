package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;

public class EquipmentLoss {
	// equipment
	private Equipment equipment;
	
	// material
	private Material material;
	
	// date and time accumulation starts
	private OffsetDateTime startDateTime;

	// date and time accumulation end
	private OffsetDateTime endDateTime;

	// map of losses
	private ConcurrentMap<TimeLoss, Duration> lossMap = new ConcurrentHashMap<>();

	// quantities produced
	private Quantity goodQuantity;
	private Quantity startupQuantity;
	private Quantity rejectQuantity;
	
	// equipment design speed
	private Quantity designSpeed;

	public EquipmentLoss(Equipment equipment) {
		this.equipment = equipment;
		resetLosses();
	}
	
	private void resetLosses() {
		setLoss(TimeLoss.NO_LOSS, Duration.ZERO);
		setLoss(TimeLoss.UNSCHEDULED, Duration.ZERO);
		setLoss(TimeLoss.MINOR_STOPPAGES, Duration.ZERO);
		setLoss(TimeLoss.PLANNED_DOWNTIME, Duration.ZERO);
		setLoss(TimeLoss.REDUCED_SPEED, Duration.ZERO);
		setLoss(TimeLoss.REJECT_REWORK, Duration.ZERO);
		setLoss(TimeLoss.SETUP, Duration.ZERO);
		setLoss(TimeLoss.UNPLANNED_DOWNTIME, Duration.ZERO);
		setLoss(TimeLoss.NOT_SCHEDULED, Duration.ZERO);
		setLoss(TimeLoss.STARTUP_YIELD, Duration.ZERO);
	}
	
	public void reset() {
		resetLosses();
		
		material = null;
		startDateTime = null;
		endDateTime = null;
		
		goodQuantity = null;
		startupQuantity = null;
		rejectQuantity = null;
		
		designSpeed = null;
	}

	public List<ParetoItem> getLossItems(Unit timeUnit) throws Exception {
		List<ParetoItem> items = new ArrayList<>();

		for (Entry<TimeLoss, Duration> entry : lossMap.entrySet()) {
			if (entry.getKey().isLoss()) {
				ParetoItem item = fromLossCategory(entry.getKey(), timeUnit);
				items.add(item);
			}
		}
		return items;
	}

	public Number convertSeconds(long seconds, Unit timeUnit) throws Exception {
		double loss = seconds;

		if (!timeUnit.equals(Unit.SECOND)) {
			Quantity q = new Quantity(loss, Unit.SECOND);
			loss = q.convert(timeUnit).getAmount();
		}
		return loss;
	}

	private ParetoItem fromLossCategory(TimeLoss category, Unit timeUnit) throws Exception {
		Number loss = convertSeconds(getLoss(category).getSeconds(), timeUnit);

		ParetoItem item = new ParetoItem(category.toString(), loss);
		return item;
	}

	public Duration getDuration() {
		if (startDateTime == null || endDateTime == null) {
			return Duration.ZERO;
		} else {
			return Duration.between(startDateTime, endDateTime);
		}
	}

	public Duration getLoss(TimeLoss category) {
		return lossMap.get(category);
	}

	public void setLoss(TimeLoss category, Duration duration) {
		lossMap.put(category, duration);
	}

	public void incrementLoss(TimeLoss category, Duration duration) {
		Duration newDuration = lossMap.get(category).plus(duration);
		setLoss(category, newDuration);
	}

	public Duration getRequiredOperationsTime() {
		return getDuration().minus(getLoss(TimeLoss.NOT_SCHEDULED));
	}

	public Duration getAvailableTime() {
		return getRequiredOperationsTime().minus(getLoss(TimeLoss.UNSCHEDULED));
	}

	public Duration getScheduledProductionTime() {
		return getAvailableTime().minus(getLoss(TimeLoss.PLANNED_DOWNTIME));
	}

	public Duration getProductionTime() {
		return getScheduledProductionTime().minus(getLoss(TimeLoss.SETUP));
	}

	public Duration getReportedProductionTime() {
		return getProductionTime().minus(getLoss(TimeLoss.UNPLANNED_DOWNTIME));
	}

	public Duration getNetProductionTime() {
		return getReportedProductionTime().minus(getLoss(TimeLoss.MINOR_STOPPAGES));
	}

	public Duration getEfficientNetProductionTime() {
		return getNetProductionTime().minus(getLoss(TimeLoss.REDUCED_SPEED));
	}

	public Duration getEffectiveNetProductionTime() {
		return getEfficientNetProductionTime().minus(getLoss(TimeLoss.REJECT_REWORK));
	}

	public Duration getValueAddingTime() {
		return getEffectiveNetProductionTime().minus(getLoss(TimeLoss.STARTUP_YIELD));
	}

	public float calculateHighLevelOeePercentage() throws Exception {
		if (getAvailableTime() == null || goodQuantity == null) {
			throw new Exception("No available time has been recorded.");
		}

		Quantity availableQty = new Quantity(getAvailableTime().getSeconds(), Unit.SECOND);
		Quantity denominator = availableQty.multiply(designSpeed);
		double hloee = goodQuantity.divide(denominator).getAmount();

		return Double.valueOf(hloee).floatValue();
	}

	public float calculateOeePercentage() throws Exception {
		float vat = this.getValueAddingTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();
		float oee = 0.0f;

		if (available != 0.0f) {
			oee = (vat / available) * 100.0f;
		}
		return oee;
	}

	public float calculatePerformancePercentage() throws Exception {
		float eff = this.getEfficientNetProductionTime().getSeconds();
		float rpt = this.getReportedProductionTime().getSeconds();

		float pp = 0.0f;

		if (rpt != 0.0f) {
			pp = (eff / rpt) * 100.0f;
		}
		return pp;
	}

	public float calculateAvailabilityPercentage() throws Exception {
		float rpt = this.getReportedProductionTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();

		float ap = 0.0f;

		if (available != 0.0f) {
			ap = (rpt / available) * 100.0f;
		}
		return ap;
	}

	public float calculateQualityPercentage() throws Exception {
		float vat = this.getValueAddingTime().getSeconds();
		float eff = this.getEfficientNetProductionTime().getSeconds();

		float qp = 0.0f;

		if (eff != 0.0f) {
			qp = (vat / eff) * 100.0f;
		}
		return qp;
	}

	public Duration calculateReducedSpeedLoss(Quantity actualSpeed, Quantity idealSpeed) throws Exception {
		// multiplier on NPT
		double npt = getNetProductionTime().getSeconds();
		Quantity q = idealSpeed.subtract(actualSpeed).divide(idealSpeed).multiply(npt);
		Duration duration = Duration.ofSeconds((long) q.getAmount());

		return duration;
	}

	public void calculateReducedSpeedLoss() throws Exception {
		Duration goodDur = convertQuantity(goodQuantity);
		setLoss(TimeLoss.NO_LOSS, goodDur);
		
		Duration rejectDur = convertQuantity(rejectQuantity);
		setLoss(TimeLoss.REJECT_REWORK, rejectDur);
		
		Duration startupDur = convertQuantity(startupQuantity);
		setLoss(TimeLoss.STARTUP_YIELD, startupDur);
		
		Duration npt = getNetProductionTime();

		// add up quality losses
		Duration reject = getLoss(TimeLoss.REJECT_REWORK);
		Duration startup = getLoss(TimeLoss.STARTUP_YIELD);
		Duration quality = reject.plus(startup);

		// subtract off good production and quality loss
		Duration noLoss = getLoss(TimeLoss.NO_LOSS);
		Duration reducedSpeed = npt.minus(quality).minus(noLoss);
		setLoss(TimeLoss.REDUCED_SPEED, reducedSpeed);
		
		System.out.println("NPT: " + npt);
		System.out.println("Reject: " + reject);
		System.out.println("Startup: " + startup);
		System.out.println("Quality: " + quality);
		System.out.println("No loss: " + noLoss);
		System.out.println("Reduced: " + reducedSpeed);
		
		System.out.println("Good Qty: " + this.goodQuantity);
		System.out.println("Reject Qty: " + this.rejectQuantity);
		System.out.println("Startup Qty: " + this.startupQuantity);
	}

	/*
	public Duration convertUnitCountToTimeLoss(Quantity loss, Quantity idealSpeed) throws Exception {
		Quantity timeLoss = loss.divide(idealSpeed).convert(Unit.SECOND);
		Duration duration = Duration.ofSeconds((long) timeLoss.getAmount());
		return duration;
	}
	*/

	public OffsetDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(OffsetDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public OffsetDateTime getEndDateTime() {
		return this.endDateTime;
	}

	public void setEndDateTime(OffsetDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if (startDateTime != null && endDateTime != null) {
			sb.append("From: ").append(startDateTime.toString()).append("To: ").append(endDateTime.toString())
					.append(", Duration: ").append(getDuration().toString());
		}
		
		// quantities
		sb.append("\nGood: ");
		if (goodQuantity != null) {
			sb.append(goodQuantity.getAmount()).append(' ').append(goodQuantity.getUOM().getSymbol());
		} else {
			sb.append('0');
		}

		sb.append("\nReject: ");
		if (rejectQuantity != null) {
			sb.append(rejectQuantity.getAmount()).append(' ').append(rejectQuantity.getUOM().getSymbol());
		} else {
			sb.append('0');
		}

		sb.append("\nStartup: ");
		if (startupQuantity != null) {
			sb.append(startupQuantity.getAmount()).append(' ').append(startupQuantity.getUOM().getSymbol());
		} else {
			sb.append('0');
		}

		// losses
		sb.append("\nLosses");
		for (Entry<TimeLoss, Duration> entry : lossMap.entrySet()) {
			sb.append('\n').append(entry.getKey().toString()).append(" = ").append(entry.getValue().toString());
		}

		// times
		sb.append("\nTimes");
		sb.append("\n Required Operations: ").append(getRequiredOperationsTime().toString());
		sb.append("\n Available: ").append(getAvailableTime().toString());
		sb.append("\n Scheduled Production: ").append(getScheduledProductionTime().toString());
		sb.append("\n Production: ").append(getProductionTime().toString());
		sb.append("\n Reported Production: ").append(getReportedProductionTime().toString());
		sb.append("\n Net Production: ").append(getNetProductionTime().toString());
		sb.append("\n Efficient Net Production: ").append(getEfficientNetProductionTime().toString());
		sb.append("\n Effective Net Production: ").append(getEffectiveNetProductionTime().toString());
		sb.append("\n Value Adding: ").append(getValueAddingTime().toString());

		return sb.toString();
	}

	private Duration convertQuantity(Quantity quantity) throws Exception {
		if (quantity == null) {
			return Duration.ZERO;
		}
		
		Quantity irr = getDesignSpeedQuantity();
		
		Quantity timeQty = quantity.divide(irr).convert(Unit.SECOND);
		long seconds = Double.valueOf(timeQty.getAmount()).longValue();
		
		Duration duration = Duration.ofSeconds(seconds);
		return duration;
	}

	public Quantity getDesignSpeedQuantity() {
		return designSpeed;
	}

	public void setDesignSpeed(Quantity designSpeedQuantity) {
		this.designSpeed = designSpeedQuantity;
	}

	public Quantity getGoodQuantity() {
		return goodQuantity;
	}

	public void setGoodQuantity(Quantity goodQuantity) {
		this.goodQuantity = goodQuantity;
	}

	public Quantity getStartupQuantity() {
		return startupQuantity;
	}

	public void setStartupQuantity(Quantity startupQuantity) {
		this.startupQuantity = startupQuantity;
	}

	public Quantity getRejectQuantity() {
		return rejectQuantity;
	}

	public void setRejectQuantity(Quantity rejectQuantity) {
		this.rejectQuantity = rejectQuantity;
	}
	
	public Quantity incrementGoodQuantity(Quantity quantity) throws Exception {
		Quantity total = goodQuantity;

		if (total != null) {
			total = total.add(quantity);
		} else {
			total = quantity;
		}
		setGoodQuantity(total);

		return total;
	}

	public Quantity incrementStartupQuantity(Quantity quantity) throws Exception {
		Quantity total = startupQuantity;

		if (total != null) {
			total = total.add(quantity);
		} else {
			total = quantity;
		}
		setStartupQuantity(total);

		return total;
	}

	public Quantity incrementRejectQuantity(Quantity quantity) throws Exception {
		Quantity total = rejectQuantity;

		if (total != null) {
			total = total.add(quantity);
		} else {
			total = quantity;
		}
		setRejectQuantity(total);

		return total;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}
}
