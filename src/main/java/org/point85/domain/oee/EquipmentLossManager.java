package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.collector.AvailabilityRecord;
import org.point85.domain.collector.BaseRecord;
import org.point85.domain.collector.ProductionRecord;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;

public class EquipmentLossManager {
	public static void calculateEquipmentLoss(EquipmentLoss equipmentLoss, OffsetDateTime from, OffsetDateTime to) throws Exception {

		Equipment equipment = equipmentLoss.getEquipment();

		Material material = equipmentLoss.getMaterial();

		//OffsetDateTime from = equipmentLoss.getStartDateTime();
		//OffsetDateTime to = equipmentLoss.getEndDateTime();

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
		List<ProductionRecord> productions = PersistenceService.instance().fetchProduction(equipment, from, to);

		for (ProductionRecord summary : productions) {
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
		List<AvailabilityRecord> availabilities = PersistenceService.instance().fetchAvailability(equipment,
				from, to);

		for (AvailabilityRecord summary : availabilities) {
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

	private static void checkTimePeriod(BaseRecord record, EquipmentLoss equipmentLoss) {
		// beginning time
		OffsetDateTime recordStart = record.getStartTime();
		OffsetDateTime recordEnd = record.getEndTime();
		
		OffsetDateTime lossStart = equipmentLoss.getStartDateTime();
		OffsetDateTime lossEnd = equipmentLoss.getEndDateTime();
		
		if (lossStart == null) {
			equipmentLoss.setStartDateTime(recordStart);
		} else {
			if (recordStart.isBefore(lossStart)) {
				equipmentLoss.setStartDateTime(recordStart);
			}
		}
		
		if (lossEnd == null) {
			equipmentLoss.setEndDateTime(recordEnd);
		} else {
			if (recordEnd.isAfter(lossEnd)) {
				equipmentLoss.setEndDateTime(recordEnd);
			}
		}
		/*


		if (lossStart == null || recordStart.compareTo(lossStart) == -1) {
			equipmentLoss.setStartDateTime(recordStart);
		}

		// ending time
		if (lossEnd== null || recordEnd.compareTo(lossEnd) == 1) {
			equipmentLoss.setEndDateTime(recordEnd);
		}
		*/
	}

	public static List<ParetoItem> fetchParetoData(EquipmentLoss equipmentLoss, TimeLoss loss) throws Exception {
		// query for the history for this loss
		OffsetDateTime startTime = equipmentLoss.getStartDateTime();
		OffsetDateTime endTime = equipmentLoss.getEndDateTime();
		
		List<AvailabilityRecord> records = PersistenceService.instance().fetchAvailability(
				equipmentLoss.getEquipment(), loss, startTime, endTime);

		// get last event
		/*
		AvailabilityRecord lastHistory = PersistenceService.instance()
				.fetchLastAvailability(equipmentLoss.getEquipment());

		if (lastHistory != null && lastHistory.getReason().getLossCategory().equals(loss)) {

			// still an open event for this loss, add event to this time
			lastHistory.setStartTime(equipmentLoss.getEndDateTime());
			histories.add(lastHistory);
		}
		*/

		// build up event durations
		Map<Reason, Duration> paretoMap = new HashMap<>();

		for (int i = 0; i <  records.size(); i++) {
			AvailabilityRecord record = records.get(i);
			
			Reason reason = record.getReason();
			OffsetDateTime start = record.getStartTime();
			Duration eventDuration = record.getDuration();
			
			Duration duration = eventDuration;
			
			// check first record for edge time
			if (i == 0) {
				// first record
				if (reason.getLossCategory().equals(loss)) {
					// get time in interval
					Duration edge = Duration.between(start, startTime);
					duration = eventDuration.minus(edge);
				}
			}  else if(i == (records.size() - 1)) {
				// last record
				if (reason.getLossCategory().equals(loss)) {
					// get time in interval
					duration = Duration.between(start, endTime);
				}
			}
			
			Duration sum = paretoMap.get(reason);
			if (sum == null) {
				// new reason
				paretoMap.put(reason, duration);
			} else {
				// add time to existing reason
				paretoMap.put(reason, sum.plus(duration));
			}
		}

		// create the items to chart
		List<ParetoItem> items = new ArrayList<>(paretoMap.entrySet().size());

		for (Entry<Reason, Duration> entry : paretoMap.entrySet()) {
			ParetoItem item = new ParetoItem(entry.getKey().getName(), entry.getValue());
			items.add(item);
		}

		return items;
	}

}
