package org.point85.domain.performance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;

public class EquipmentLoss {
	// date and time accumulation starts
	private LocalDateTime startDateTime;

	// all durations at resolution of seconds
	private Duration totalDuration = Duration.ZERO;

	private Map<TimeLoss, Duration> lossMap = new HashMap<>();

	public EquipmentLoss() {
		initialize();
	}

	private void initialize() {
		this.setLoss(TimeLoss.UNSCHEDULED, Duration.ZERO);
		this.setLoss(TimeLoss.MINOR_STOPPAGES, Duration.ZERO);
		this.setLoss(TimeLoss.PLANNED_DOWNTIME, Duration.ZERO);
		this.setLoss(TimeLoss.REDUCED_SPEED, Duration.ZERO);
		this.setLoss(TimeLoss.REJECT_REWORK, Duration.ZERO);
		this.setLoss(TimeLoss.SETUP, Duration.ZERO);
		this.setLoss(TimeLoss.UNPLANNED_DOWNTIME, Duration.ZERO);
		this.setLoss(TimeLoss.NOT_SCHEDULED, Duration.ZERO);
		this.setLoss(TimeLoss.STARTUP_YIELD, Duration.ZERO);
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

	public Duration getTotalTime() {
		return totalDuration;
	}

	public void setTotalTime(Duration totalTime) {
		this.totalDuration = totalTime;
	}

	public Duration getLoss(TimeLoss category) {
		return lossMap.get(category);
	}

	public void setLoss(TimeLoss category, Duration duration) {
		lossMap.put(category, duration);
	}

	public void addLoss(TimeLoss category, Duration duration) {
		Duration newDuration = lossMap.get(category).plus(duration);
		setLoss(category, newDuration);
	}

	public Duration getRequiredOperationsTime() {
		return getTotalTime().minus(getLoss(TimeLoss.NOT_SCHEDULED));
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

	public float calculateOEEPercentage() throws Exception {
		if (this.getAvailableTime() == null) {
			throw new Exception("No available time has been recorded.");
		}

		float vat = this.getValueAddingTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();
		return (vat / available) * 100.0f;
	}

	public float calculatePerformancePercentage() throws Exception {
		if (this.getReportedProductionTime() == null) {
			throw new Exception("No reported production time has been recorded.");
		}

		float eff = this.getEfficientNetProductionTime().getSeconds();
		float rpt = this.getReportedProductionTime().getSeconds();
		return (eff / rpt) * 100.0f;
	}

	public float calculateAvailabilityPercentage() throws Exception {
		if (this.getAvailableTime() == null) {
			throw new Exception("No available time has been recorded.");
		}

		float rpt = this.getReportedProductionTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();
		return (rpt / available) * 100.0f;
	}

	public float calculateQualityPercentage() throws Exception {
		if (this.getAvailableTime() == null) {
			throw new Exception("No efficient net production time has been recorded.");
		}

		float vat = this.getValueAddingTime().getSeconds();
		float eff = this.getEfficientNetProductionTime().getSeconds();
		return (vat / eff) * 100.0f;
	}

	/*
	 * private Quantity convertSpeed(Quantity speed) throws Exception {
	 * MeasurementSystem sys = MeasurementSystem.getSystem();
	 * 
	 * Quantity convertedSpeed = speed; UnitOfMeasure divisorUOM =
	 * speed.getUOM().getDivisor();
	 * 
	 * // ideal rate in units/second if (!divisorUOM.equals(sys.getSecond())) {
	 * BigDecimal factor = divisorUOM.getConversionFactor(sys.getSecond());
	 * convertedSpeed = speed.divide(factor); } return convertedSpeed; }
	 */

	public Duration calculateReducedSpeedLoss(Quantity actualSpeed, Quantity idealSpeed) throws Exception {
		// multiplier on NPT
		double npt = getNetProductionTime().getSeconds();
		Quantity q = idealSpeed.subtract(actualSpeed).divide(idealSpeed).multiply(npt);
		Duration duration = Duration.ofSeconds((long) q.getAmount());

		return duration;
	}

	public Duration convertUnitCountToTimeLoss(Quantity loss, Quantity idealSpeed) throws Exception {
		Quantity timeLoss = loss.divide(idealSpeed).convert(Unit.SECOND);
		Duration duration = Duration.ofSeconds((long) timeLoss.getAmount());
		return duration;
	}

	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public LocalDateTime getEndDateTime() {
		return startDateTime.plus(totalDuration);
	}

}
