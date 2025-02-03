package org.point85.domain.oee;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.Break;
import org.point85.domain.schedule.ExceptionPeriod;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EquipmentLossManager {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(EquipmentLossManager.class);

	private EquipmentLossManager() {
	}

	private static void calculateEquipmentLoss(EquipmentLoss equipmentLoss, OffsetDateTime from, OffsetDateTime to)
			throws Exception {

		// time period
		equipmentLoss.setStartDateTime(from);
		equipmentLoss.setEndDateTime(to);

		Equipment equipment = equipmentLoss.getEquipment();
		Material material = equipmentLoss.getMaterial();

		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		if (eqm == null || eqm.getRunRate() == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.speed", equipment.getName(),
					material.getDisplayString()));
		}

		// IRR
		equipmentLoss.setDesignSpeed(eqm.getRunRate());

		// material production
		List<OeeEvent> productions = PersistenceService.instance().fetchProduction(equipment, material, from, to);

		equipmentLoss.getEventRecords().addAll(productions);

		for (OeeEvent production : productions) {
			Quantity quantity = production.getQuantity();

			if (quantity.getUOM() == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.uom.production", quantity.getAmount(),
						production.getSourceId(), production.getOffsetStartTime()));
			}
			Duration lostTime = null;

			switch (production.getEventType()) {
			case PROD_GOOD: {
				equipmentLoss.incrementGoodQuantity(quantity);
				break;
			}

			case PROD_REJECT: {
				equipmentLoss.incrementRejectQuantity(quantity);

				// convert to a time loss
				lostTime = equipmentLoss.convertToLostTime(quantity);
				production.setLostTime(lostTime);
				break;
			}

			case PROD_STARTUP: {
				equipmentLoss.incrementStartupQuantity(quantity);

				// convert to a time loss
				lostTime = equipmentLoss.convertToLostTime(quantity);
				production.setLostTime(lostTime);
				break;
			}

			default:
				break;
			}

			if (production.getReason() != null && lostTime != null) {
				// reason map too
				equipmentLoss.incrementReasonLoss(production.getReason(), lostTime);
			}
		}

		// availability losses
		List<OeeEvent> events = PersistenceService.instance().fetchAvailability(equipment, from, to);

		equipmentLoss.getEventRecords().addAll(events);

		for (int i = 0; i < events.size(); i++) {
			OeeEvent event = events.get(i);

			// first gather MTBF & MTTR data
			equipmentLoss.collectMeanData(event);

			// check for edge effects
			Duration eventDuration = event.getDuration();
			Duration duration = eventDuration;

			OffsetDateTime start = event.getStartTime();
			OffsetDateTime end = event.getEndTime();

			// check first record for edge time
			if (i == 0) {
				// first record
				if (from.isAfter(start)) {
					// get time in interval
					Duration edge = Duration.between(start, from);
					duration = eventDuration.minus(edge);
				}
			} else if (i == (events.size() - 1)) {
				// last record
				if (end == null || to.isBefore(end)) {
					// get time in interval
					Duration edge = Duration.between(start, to);

					// clip to event duration
					if (eventDuration != null && edge.compareTo(eventDuration) < 0) {
						duration = edge;
					}
				}
			}

			// increment the loss for this reason
			equipmentLoss.incrementLoss(event.getReason(), duration);

			// save in event record
			event.setLostTime(duration);

			// collect PackML data
			equipmentLoss.collectPackMLStateData(event.getReason(), duration);
		}

		// find the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();

		// calculate the non-working time flagged as lost time
		addExceptionPeriodLoss(equipmentLoss, schedule);

		// calculate shift breaks flagged as lost time
		addBreakLoss(equipmentLoss, schedule);

		// compute reduced speed from the other losses
		equipmentLoss.calculateReducedSpeedLoss();

		if (logger.isTraceEnabled()) {
			logger.trace(equipmentLoss.toString());
		}
	}

	private static void addExceptionPeriodLoss(EquipmentLoss equipmentLoss, WorkSchedule schedule) throws Exception {
		if (schedule == null) {
			return;
		}

		// calculate the non-working time based on the time frame
		OffsetDateTime odtStart = equipmentLoss.getStartDateTime();
		OffsetDateTime odtEnd = equipmentLoss.getEndDateTime();

		if (odtStart == null || odtEnd == null) {
			return;
		}

		LocalDateTime from = odtStart.toLocalDateTime();
		LocalDateTime to = odtEnd.toLocalDateTime();

		final ZoneId ZONE_ID = ZoneId.of("Z");
		long fromSeconds = from.atZone(ZONE_ID).toEpochSecond();
		long toSeconds = to.atZone(ZONE_ID).toEpochSecond();

		Map<TimeLoss, Duration> exceptionLossMap = new EnumMap<>(TimeLoss.class);

		for (ExceptionPeriod period : schedule.getExceptionPeriods()) {
			LocalDateTime start = period.getStartDateTime();
			long startSeconds = start.atZone(ZONE_ID).toEpochSecond();

			LocalDateTime end = period.getEndDateTime();
			long endSeconds = end.atZone(ZONE_ID).toEpochSecond();

			if (fromSeconds >= endSeconds) {
				// look at next period
				continue;
			}

			if (toSeconds <= startSeconds) {
				// done with periods
				break;
			}

			// found a period, check edge conditions
			if (fromSeconds > startSeconds) {
				startSeconds = fromSeconds;
			}

			if (toSeconds < endSeconds) {
				endSeconds = toSeconds;
			}

			Duration periodDuration = Duration.ofSeconds(endSeconds - startSeconds);
			TimeLoss periodLoss = period.getLossCategory();
			Duration sum = exceptionLossMap.get(periodLoss);

			if (sum == null) {
				exceptionLossMap.put(periodLoss, periodDuration);
			} else {
				Duration total = sum.plus(periodDuration);
				exceptionLossMap.put(periodLoss, total);
			}

			if (toSeconds <= endSeconds) {
				break;
			}
		}

		// add to equipment losses
		for (Entry<TimeLoss, Duration> entry : exceptionLossMap.entrySet()) {
			if (!entry.getKey().equals(TimeLoss.NO_LOSS)) {
				Duration accumulated = equipmentLoss.getLoss(entry.getKey());
				equipmentLoss.setLoss(entry.getKey(), entry.getValue().plus(accumulated));
			}
		}
	}

	private static void addBreakLoss(EquipmentLoss equipmentLoss, WorkSchedule schedule) throws Exception {
		if (schedule == null) {
			return;
		}

		// see if any breaks have loss time
		boolean checkBreaks = false;

		for (Shift shift : schedule.getShifts()) {
			for (Break period : shift.getBreaks()) {
				TimeLoss loss = period.getLossCategory();
				if (loss != null && !loss.equals(TimeLoss.NO_LOSS)) {
					checkBreaks = true;
					break;
				}
			}

			if (checkBreaks) {
				break;
			}
		}

		if (!checkBreaks) {
			return;
		}

		OffsetDateTime odtStart = equipmentLoss.getStartDateTime();
		OffsetDateTime odtEnd = equipmentLoss.getEndDateTime();

		// get shift instances
		LocalDate startDate = odtStart.toLocalDate();
		LocalDate endDate = odtEnd.toLocalDate();

		long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

		LocalDate day = startDate;

		Map<TimeLoss, Duration> breakLossMap = new EnumMap<>(TimeLoss.class);

		// check each day in the period
		for (long i = 0; i < days; i++) {
			List<ShiftInstance> instances = schedule.getShiftInstancesForDay(day);

			for (ShiftInstance instance : instances) {
				List<Break> breaks = instance.getShift().getBreaks();

				for (Break period : breaks) {
					// check loss category
					if (period.getLossCategory() == null || (period.getLossCategory() != null
							&& period.getLossCategory().equals(TimeLoss.NO_LOSS))) {
						continue;
					}

					// only include contained breaks
					if (i == 0) {
						if (period.getStart().isBefore(odtStart.toLocalTime())) {
							if (logger.isWarnEnabled()) {
								logger.warn(DomainLocalizer.instance().getErrorString("break.start.before",
										period.getStart(), odtStart.toLocalTime()));
							}
							continue;
						}
					}

					if (i == (days - 1)) {
						LocalTime endTime = odtEnd.toLocalTime();
						if (!endTime.equals(LocalTime.MIDNIGHT) && endTime.isBefore(period.getEnd())) {
							if (logger.isWarnEnabled()) {
								logger.warn(DomainLocalizer.instance().getErrorString("break.end.after",
										period.getEnd(), odtEnd.toLocalTime()));
							}
							continue;
						}
					}

					// add to total
					Duration breakDuration = period.getDuration();
					TimeLoss breakLoss = period.getLossCategory();
					Duration sum = breakLossMap.get(breakLoss);

					if (sum == null) {
						sum = Duration.ZERO;
					}

					if (breakLoss != null) {
						Duration total = sum.plus(breakDuration);
						breakLossMap.put(breakLoss, total);
					}
				} /// end each break
			} // end each shift
			day = day.plusDays(1);
		} // end day-by-day

		// add to equipment losses
		for (Entry<TimeLoss, Duration> entry : breakLossMap.entrySet()) {
			Duration accumulated = equipmentLoss.getLoss(entry.getKey());
			equipmentLoss.setLoss(entry.getKey(), entry.getValue().plus(accumulated));
		}
	}

	public static List<ParetoItem> getParetoData(EquipmentLoss equipmentLoss, TimeLoss loss) {
		// create the items to chart
		Map<Reason, Duration> reasonMap = equipmentLoss.getLossReasonsByCategory(loss);

		List<ParetoItem> items = new ArrayList<>(reasonMap.entrySet().size());

		for (Entry<Reason, Duration> entry : reasonMap.entrySet()) {
			ParetoItem item = new ParetoItem(entry.getKey().getName(), entry.getValue());
			items.add(item);
		}
		return items;
	}

	public static void buildLoss(EquipmentLoss equipmentLoss, String materialId, OffsetDateTime odtStart,
			OffsetDateTime odtEnd) throws Exception {

		if (odtStart == null || odtEnd == null) {
			return;
		}

		Map<String, Material> materialMap = new HashMap<>();

		List<OeeEvent> setups = null;
		if (materialId != null) {
			Material material = PersistenceService.instance().fetchMaterialByName(materialId);

			// filter for a specific material
			setups = PersistenceService.instance().fetchSetupsForPeriodAndMaterial(equipmentLoss.getEquipment(),
					odtStart, odtEnd, material);
		} else {
			// material and job during this period from setups
			setups = PersistenceService.instance().fetchSetupsForPeriod(equipmentLoss.getEquipment(), odtStart, odtEnd);
		}

		if (setups.isEmpty()) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.setup",
					DomainUtils.offsetDateTimeToString(odtStart, DomainUtils.OFFSET_DATE_TIME_PATTERN),
					DomainUtils.offsetDateTimeToString(odtEnd, DomainUtils.OFFSET_DATE_TIME_PATTERN)));
		}

		// add setup events
		equipmentLoss.getEventRecords().addAll(setups);

		// step through each setup period since materials could have changed
		for (OeeEvent setup : setups) {
			if (setup.getMaterial() == null) {
				continue;
			}

			String id = setup.getMaterial().getDisplayString();

			if (materialMap.get(id) == null) {
				materialMap.put(id, setup.getMaterial());
			}

			equipmentLoss.setMaterial(setup.getMaterial());

			// calculate the time losses over the setup period
			OffsetDateTime periodStart = setup.getStartTime();

			if (periodStart.compareTo(odtStart) < 0) {
				periodStart = odtStart;
			}

			OffsetDateTime periodEnd = setup.getEndTime();

			if (periodEnd == null || (periodEnd.compareTo(odtEnd) > 0)) {
				periodEnd = odtEnd;
			}

			/// calculate the time losses for this material and time period
			EquipmentLossManager.calculateEquipmentLoss(equipmentLoss, periodStart, periodEnd);
		}
	}
}
