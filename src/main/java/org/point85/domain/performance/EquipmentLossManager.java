package org.point85.domain.performance;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.collector.AvailabilityHistory;
import org.point85.domain.collector.AvailabilitySummary;
import org.point85.domain.collector.BaseSummary;
import org.point85.domain.collector.ProductionSummary;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;

public class EquipmentLossManager {
	public static void calculateEquipmentLoss(EquipmentLoss equipmentLoss) throws Exception {

		Equipment equipment = equipmentLoss.getEquipment();

		Material material = equipmentLoss.getMaterial();

		OffsetDateTime from = equipmentLoss.getStartDateTime();
		OffsetDateTime to = equipmentLoss.getEndDateTime();

		// from the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();
		if (schedule == null) {
			throw new Exception("A work schedule must be defined for this equipment.");
		}

		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		if (eqm == null || eqm.getRunRate() == null) {
			throw new Exception("The design speed must be defined for equipment " + equipment.getName()
					+ " and material " + material.getDisplayString());
		}

		// IRR
		equipmentLoss.setDesignSpeed(eqm.getRunRate());

		// time from measured production
		List<ProductionSummary> productions = PersistenceService.instance().fetchProductionSummary(equipment, from, to);

		for (ProductionSummary summary : productions) {
			checkTimePeriod(summary, equipmentLoss);

			Quantity quantity = summary.getQuantity();

			switch (summary.getType()) {
			case PROD_GOOD:
				equipmentLoss.incrementGoodQuantity(quantity);
				break;

			case PROD_REJECT:
				equipmentLoss.incrementRejectQuantity(quantity);
				break;

			case PROD_STARTUP:
				equipmentLoss.incrementStartupQuantity(quantity);
				break;

			default:
				break;
			}
		}

		// System.out.println(equipmentLoss.toString());

		// time from measured availability losses
		List<AvailabilitySummary> availabilities = PersistenceService.instance().fetchAvailabilitySummary(equipment,
				from, to);

		for (AvailabilitySummary summary : availabilities) {
			checkTimePeriod(summary, equipmentLoss);

			TimeLoss loss = summary.getReason().getLossCategory();
			equipmentLoss.incrementLoss(loss, summary.getDuration());
		}

		// compute reduced speed from the other losses
		equipmentLoss.calculateReducedSpeedLoss();

		// calculate the non-working time based on the time frame
		OffsetDateTime odtStart = equipmentLoss.getStartDateTime();
		OffsetDateTime odtEnd = equipmentLoss.getEndDateTime();

		if (odtStart != null && odtEnd != null) {
			Duration notScheduled = schedule.calculateNonWorkingTime(odtStart.toLocalDateTime(),
					odtEnd.toLocalDateTime());
			equipmentLoss.setLoss(TimeLoss.NOT_SCHEDULED, notScheduled);
		}

		// System.out.println(equipmentLoss.toString());
	}

	private static void checkTimePeriod(BaseSummary summary, EquipmentLoss equipmentLoss) {
		// beginning time
		OffsetDateTime start = summary.getStartTime();
		OffsetDateTime end = summary.getEndTime();

		if (equipmentLoss.getStartDateTime() == null || start.compareTo(equipmentLoss.getStartDateTime()) == -1) {
			equipmentLoss.setStartDateTime(start);
		}

		// ending time
		if (equipmentLoss.getEndDateTime() == null || end.compareTo(equipmentLoss.getEndDateTime()) == 1) {
			equipmentLoss.setEndDateTime(end);
		}
	}

	public static List<ParetoItem> fetchParetoData(EquipmentLoss equipmentLoss, TimeLoss loss) throws Exception {
		// query for the history for this loss
		List<AvailabilityHistory> histories = PersistenceService.instance().fetchAvailabilityHistory(
				equipmentLoss.getEquipment(), loss, equipmentLoss.getStartDateTime(), equipmentLoss.getEndDateTime());

		// get last event
		AvailabilityHistory lastHistory = PersistenceService.instance()
				.fetchLastAvailabilityHistory(equipmentLoss.getEquipment());

		if (lastHistory != null && lastHistory.getReason().getLossCategory().equals(loss)) {

			// still an open event for this loss, add event to this time
			lastHistory.setSourceTimestamp(equipmentLoss.getEndDateTime());
			histories.add(lastHistory);
		}
		
		// TODO event previous to the startDateTime

		Map<Reason, Duration> paretoMap = new HashMap<>();

		OffsetDateTime end = null;

		for (AvailabilityHistory history : histories) {
			Reason reason = history.getReason();
			OffsetDateTime start = history.getSourceTimestamp();
			
			if (start == null) {
				continue;
			}

			if (end == null) {
				end = start;
			}

			Duration duration = Duration.between(end, start);

			Duration sum = paretoMap.get(reason);
			if (sum == null) {
				// new reason
				paretoMap.put(reason, duration);
			} else {
				// add time to existing reason
				paretoMap.put(reason, sum.plus(duration));
			}

			end = start;
		}

		List<ParetoItem> items = new ArrayList<>(paretoMap.entrySet().size());

		for (Entry<Reason, Duration> entry : paretoMap.entrySet()) {
			ParetoItem item = new ParetoItem(entry.getKey().getName(), entry.getValue());
			items.add(item);
		}

		return items;
	}

}
