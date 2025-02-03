package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.opc.ua.packml.PackMLState;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;

public class EquipmentLoss {
	// equipment
	private Equipment equipment;

	// material
	private Material material;

	// date and time accumulation starts
	private OffsetDateTime startDateTime;

	// date and time accumulation ends
	private OffsetDateTime endDateTime;

	// accumulated working time
	private Duration totalWorkingTime;

	// map of losses
	private final EnumMap<TimeLoss, Duration> lossMap = new EnumMap<>(TimeLoss.class);

	// map of reasons
	private final EnumMap<TimeLoss, Map<Reason, Duration>> reasonMap = new EnumMap<>(TimeLoss.class);

	// quantities produced
	private Quantity goodQuantity;
	private Quantity startupQuantity;
	private Quantity rejectQuantity;

	// equipment design speed
	private Quantity designSpeed;

	// history
	private List<OeeEvent> eventRecords = new ArrayList<>();

	// MTBF
	private OeeEvent lastFailure;
	private List<Duration> failures = new ArrayList<>();

	// MTTR
	private OeeEvent lastRepair;
	private List<Duration> repairs = new ArrayList<>();

	// PackML time in state
	private Map<PackMLState, Duration> packMLStateDurations = new HashMap<>();
	
	// PackML time with reason for the state
	private Map<Reason, Duration> packMLReasonDurations = new HashMap<>();

	public EquipmentLoss(Equipment equipment) {
		this.equipment = equipment;
		resetLosses();
	}

	private void resetLosses() {
		// summary losses
		lossMap.put(TimeLoss.NO_LOSS, Duration.ZERO);
		lossMap.put(TimeLoss.UNSCHEDULED, Duration.ZERO);
		lossMap.put(TimeLoss.MINOR_STOPPAGES, Duration.ZERO);
		lossMap.put(TimeLoss.PLANNED_DOWNTIME, Duration.ZERO);
		lossMap.put(TimeLoss.REDUCED_SPEED, Duration.ZERO);
		lossMap.put(TimeLoss.REJECT_REWORK, Duration.ZERO);
		lossMap.put(TimeLoss.SETUP, Duration.ZERO);
		lossMap.put(TimeLoss.UNPLANNED_DOWNTIME, Duration.ZERO);
		lossMap.put(TimeLoss.NOT_SCHEDULED, Duration.ZERO);
		lossMap.put(TimeLoss.STARTUP_YIELD, Duration.ZERO);

		// reason losses
		reasonMap.put(TimeLoss.NO_LOSS, new HashMap<>());
		reasonMap.put(TimeLoss.UNSCHEDULED, new HashMap<>());
		reasonMap.put(TimeLoss.MINOR_STOPPAGES, new HashMap<>());
		reasonMap.put(TimeLoss.PLANNED_DOWNTIME, new HashMap<>());
		reasonMap.put(TimeLoss.REDUCED_SPEED, new HashMap<>());
		reasonMap.put(TimeLoss.REJECT_REWORK, new HashMap<>());
		reasonMap.put(TimeLoss.SETUP, new HashMap<>());
		reasonMap.put(TimeLoss.UNPLANNED_DOWNTIME, new HashMap<>());
		reasonMap.put(TimeLoss.NOT_SCHEDULED, new HashMap<>());
		reasonMap.put(TimeLoss.STARTUP_YIELD, new HashMap<>());
	}

	public void reset() {
		resetLosses();

		eventRecords = new ArrayList<>();

		material = null;
		startDateTime = null;
		endDateTime = null;
		totalWorkingTime = null;

		goodQuantity = null;
		startupQuantity = null;
		rejectQuantity = null;

		designSpeed = null;

		lastFailure = null;
		failures.clear();

		lastRepair = null;
		repairs.clear();

		packMLStateDurations.clear();
		packMLReasonDurations.clear();
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
	
	public Object[] getPackMLStateItems(Unit timeUnit) throws Exception {
		List<ParetoItem> items = new ArrayList<>();
		Duration total = Duration.ZERO;
		
		for (Entry<PackMLState, Duration> entry : packMLStateDurations.entrySet()) {
			// skip execute state
			if (!entry.getKey().equals(PackMLState.Execute)) {
				long seconds = entry.getValue().getSeconds();
				total = total.plusSeconds(seconds);
				Number loss = convertSeconds(seconds, timeUnit);

				ParetoItem item= new ParetoItem(entry.getKey(), loss);
				items.add(item);
			}
		}
		Object[] values = new Object[2];
		values[0] = items;
		values[1] = total;
		
		return values;
	}
	
	public Object[] getPackMLReasonItems(Unit timeUnit) throws Exception {
		List<ParetoItem> items = new ArrayList<>();
		Duration total = Duration.ZERO;
		
		for (Entry<Reason, Duration> entry : packMLReasonDurations.entrySet()) {
			// skip value adding
			OeeComponent component =  null;
			if (entry.getKey().getLossCategory() != null) {
				component = entry.getKey().getLossCategory().getComponent();
			}
			
			if (component != null && !component.equals(OeeComponent.NORMAL)) {
				long seconds = entry.getValue().getSeconds();
				total = total.plusSeconds(seconds);
				Number loss = convertSeconds(seconds, timeUnit);

				ParetoItem item= new ParetoItem(entry.getKey(), loss);
				items.add(item);
			}
		}
		Object[] values = new Object[2];
		values[0] = items;
		values[1] = total;
		
		return values;
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

		return new ParetoItem(category, loss);
	}

	public Duration getDuration() {
		if (totalWorkingTime == null && startDateTime != null && endDateTime != null) {
			// get the work schedule working time
			WorkSchedule currentSchedule = equipment.findWorkSchedule();

			if (currentSchedule != null) {
				try {
					totalWorkingTime = currentSchedule.calculateWorkingTime(startDateTime.toLocalDateTime(),
							endDateTime.toLocalDateTime());
				} catch (Exception e) {
					// caller must enter valid starting and ending dates
				}
			} else {
				// assume all time is working time
				totalWorkingTime = Duration.between(startDateTime, endDateTime);
			}
		}
		return totalWorkingTime;
	}

	public Duration getLoss(TimeLoss category) {
		return lossMap.get(category);
	}

	void setLoss(TimeLoss category, Duration duration) {
		lossMap.put(category, duration);
	}

	public void incrementReasonLoss(Reason reason, Duration duration) {
		TimeLoss loss = reason.getLossCategory();
		Map<Reason, Duration> losses = reasonMap.get(loss);

		Duration reasonDuration = losses.get(reason);

		if (reasonDuration == null) {
			reasonDuration = Duration.ZERO;
		}

		Duration newDuration = reasonDuration.plus(duration);
		losses.put(reason, newDuration);
	}

	public void incrementLoss(Reason reason, Duration duration) {
		if (reason == null || duration == null) {
			return;
		}
		TimeLoss category = reason.getLossCategory();

		// skip no loss records
		if (category.equals(TimeLoss.NO_LOSS)) {
			return;
		}

		// summary map
		Duration newDuration = lossMap.get(category).plus(duration);
		setLoss(category, newDuration);

		// reason map too
		incrementReasonLoss(reason, duration);
	}

	public Map<Reason, Duration> getLossReasonsByCategory(TimeLoss loss) {
		Map<Reason, Duration> losses = reasonMap.get(loss);

		if (losses == null) {
			losses = new HashMap<>();
		}
		return losses;
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
			throw new Exception(DomainLocalizer.instance().getErrorString("no.time"));
		}

		// available time in seconds
		Quantity availableTime = new Quantity(getAvailableTime().getSeconds(), Unit.SECOND);

		// convert available time to design speed unit
		UnitOfMeasure timeUnit = designSpeed.getUOM().getDivisor();
		Quantity convertedTime = availableTime.convert(timeUnit);

		Quantity denominator = convertedTime.multiply(designSpeed);

		// High level OEE in percent
		double hloee = goodQuantity.divide(denominator).getAmount() * 100.0d;
		return (float) hloee;
	}

	public float calculateOeePercentage() {
		float vat = this.getValueAddingTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();
		float oee = 0.0f;

		if (available != 0.0f) {
			oee = (vat / available) * 100.0f;
		}
		return oee;
	}

	public float calculatePerformancePercentage() {
		float eff = this.getEfficientNetProductionTime().getSeconds();
		float rpt = this.getReportedProductionTime().getSeconds();

		float pp = 0.0f;

		if (rpt != 0.0f) {
			pp = (eff / rpt) * 100.0f;
		}
		return pp;
	}

	public float calculateAvailabilityPercentage() {
		float rpt = this.getReportedProductionTime().getSeconds();
		float available = this.getAvailableTime().getSeconds();

		float ap = 0.0f;

		if (available != 0.0f) {
			ap = (rpt / available) * 100.0f;
		}
		return ap;
	}

	public float calculateQualityPercentage() {
		float vat = this.getValueAddingTime().getSeconds();
		float eff = this.getEfficientNetProductionTime().getSeconds();

		float qp = 0.0f;

		if (eff != 0.0f) {
			qp = (vat / eff) * 100.0f;
		}
		return qp;
	}

	public void calculateReducedSpeedLoss() throws Exception {
		Duration goodDur = convertToLostTime(goodQuantity);
		setLoss(TimeLoss.NO_LOSS, goodDur);

		Duration rejectDur = convertToLostTime(rejectQuantity);
		setLoss(TimeLoss.REJECT_REWORK, rejectDur);

		Duration startupDur = convertToLostTime(startupQuantity);
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
	}

	public Quantity getTotalQuantity(UnitOfMeasure uom) throws Exception {
		Quantity total = new Quantity(0.0d, uom);

		if (goodQuantity != null) {
			total = total.add(goodQuantity);
		}

		if (rejectQuantity != null) {
			total = total.add(rejectQuantity.convert(uom));
		}

		if (startupQuantity != null) {
			total = total.add(startupQuantity.convert(uom));
		}

		return total;
	}

	public Quantity calculateActualSpeed(Quantity designSpeed) throws Exception {
		Quantity speed = null;

		if (!getAvailableTime().equals(Duration.ZERO)) {
			Quantity timeQty = new Quantity(getAvailableTime().getSeconds(), Unit.SECOND);
			UnitOfMeasure goodUOM = designSpeed.getUOM().getDividend();
			speed = getTotalQuantity(goodUOM).divide(timeQty).convert(designSpeed.getUOM());
		}
		return speed;
	}

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
		StringBuilder sb = new StringBuilder();

		if (startDateTime != null && endDateTime != null) {
			sb.append("\nFrom: ").append(startDateTime.toString()).append(" To: ").append(endDateTime.toString())
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

	Duration convertToLostTime(Quantity quantity) throws Exception {
		if (quantity == null) {
			return Duration.ZERO;
		}

		Quantity irr = getDesignSpeedQuantity();

		Quantity timeQty = quantity.divide(irr).convert(Unit.SECOND);
		long seconds = (long) timeQty.getAmount();

		return Duration.ofSeconds(seconds);
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

	public List<OeeEvent> getEventRecords() {
		return eventRecords;
	}

	public void setEventRecords(List<OeeEvent> records) {
		this.eventRecords = records;
	}

	public EquipmentMaterial getEquipmentMaterial() {
		EquipmentMaterial eqm = null;
		if (equipment != null && material != null) {
			eqm = equipment.getEquipmentMaterial(material);
		}
		return eqm;
	}

	public OeeEvent getLastFailure() {
		return lastFailure;
	}

	public void setLastFailure(OeeEvent lastFailure) {
		this.lastFailure = lastFailure;
	}

	public OeeEvent getLastRepair() {
		return lastRepair;
	}

	public void setLastRepair(OeeEvent lastRepair) {
		this.lastRepair = lastRepair;
	}

	public void addMTBF(Duration duration) {
		this.failures.add(duration);
	}

	public List<Duration> getFailures() {
		return failures;
	}

	public void addMTTR(Duration duration) {
		this.repairs.add(duration);
	}

	public List<Duration> getRepairs() {
		return this.repairs;
	}

	public Map<PackMLState, Duration> getPackMLDurations() {
		return this.packMLStateDurations;
	}

	/**
	 * Compute the Mean Time Between Failures (MTBF) based on previously collected
	 * failure data. A failure is an unplanned downtime event.
	 * 
	 * @return average duration of a failure.
	 */
	public Duration calculateMTBF() {
		// skip summary records
		Duration duration = Duration.ZERO;

		for (Duration failure : failures) {
			duration = duration.plus(failure);
		}

		if (!failures.isEmpty()) {
			float totalSeconds = duration.getSeconds();
			float count = failures.size();
			duration = Duration.ofSeconds(Math.round(totalSeconds / count));
		}
		return duration;
	}

	/**
	 * Compute the Mean Time To Repair (MTTR) based on previously collected repair
	 * data. A repair duration is the time between a failure and the next
	 * non-failure availability event.
	 * 
	 * @return average duration of a repair
	 */
	public Duration calculateMTTR() {
		Duration duration = Duration.ZERO;

		for (Duration repair : repairs) {
			duration = duration.plus(repair);
		}

		if (!repairs.isEmpty()) {
			float totalSeconds = duration.getSeconds();
			float count = repairs.size();
			duration = Duration.ofSeconds(Math.round(totalSeconds / count));
		}
		return duration;
	}

	// failure event is defined as the end time minus start time period equal to the
	// duration
	private boolean isFailureEvent(OeeEvent event) {
		boolean isEvent = false;
		OffsetDateTime eventStartTime = event.getStartTime();
		OffsetDateTime eventEndTime = event.getEndTime();

		if (eventEndTime != null) {
			Duration periodDuration = Duration.between(eventStartTime, eventEndTime);
			if (periodDuration.compareTo(event.getDuration()) == 0) {
				isEvent = true;
			}
		}

		return isEvent;
	}

	// collect time in PackML states
	public void collectPackMLStateData(Reason reason, Duration duration) {
		if (reason == null || duration == null) {
			return;
		}

		PackMLState state = reason.getPackMLState();

		if (state == null) {
			// non-PackML reason
			return;
		}

		// add up time in PackML states
		Duration total = packMLStateDurations.get(state);
		if (total == null) {
			total = Duration.ZERO;
			packMLStateDurations.put(state, total);
		}
		packMLStateDurations.put(state, total.plus(duration));
		
		// add up time for this reason
		total = packMLReasonDurations.get(reason);
		if (total == null) {
			total = Duration.ZERO;
			packMLReasonDurations.put(reason, total);
		}
		packMLReasonDurations.put(reason, total.plus(duration));
	}

	/**
	 * Accumulate duration of failure and repair events to use in the mean
	 * calculations. Summary data is excluded.
	 * 
	 * @param event {@link OeeEvent}
	 */
	public void collectMeanData(OeeEvent event) {
		if (event.getReason() == null) {
			return;
		}

		// save data for MTBF and MTTR, must not be summarized data
		TimeLoss lossCategory = event.getReason().getLossCategory();
		OffsetDateTime eventStartTime = event.getStartTime();
		OeeEvent failure = getLastFailure();

		if (lossCategory.equals(TimeLoss.UNPLANNED_DOWNTIME)) {
			// check last failure
			if (failure != null && isFailureEvent(failure)) {
				// another failure, look for events only - no summaries
				if (isFailureEvent(event)) {
					OffsetDateTime lastTime = failure.getStartTime();
					Duration duration = Duration.between(lastTime, eventStartTime);
					addMTBF(duration);
				}
			}
			setLastFailure(event);
			setLastRepair(null);
		} else {
			// check for a repair after a failure, look for events only - no summaries
			if (lossCategory.equals(TimeLoss.NO_LOSS) || lossCategory.equals(TimeLoss.SETUP)
					|| lossCategory.equals(TimeLoss.PLANNED_DOWNTIME)) {
				// repair event
				if (failure != null && isFailureEvent(failure) && getLastRepair() == null) {
					// calculate repair time
					OffsetDateTime lastTime = failure.getStartTime();
					Duration duration = Duration.between(lastTime, eventStartTime);
					addMTTR(duration);
				}
			}
			setLastRepair(event);
		}
	}
}
