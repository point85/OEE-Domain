package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquipmentLossManager {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(EquipmentLossManager.class);

	public static void calculateEquipmentLoss(EquipmentLoss equipmentLoss, OffsetDateTime from, OffsetDateTime to)
			throws Exception {

		Equipment equipment = equipmentLoss.getEquipment();
		Material material = equipmentLoss.getMaterial();

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
		List<OeeEvent> productions = PersistenceService.instance().fetchProduction(equipment, from, to);

		equipmentLoss.getEventRecords().addAll(productions);

		for (OeeEvent record : productions) {
			checkTimePeriod(record, equipmentLoss, from, to);

			Quantity quantity = record.getQuantity();

			switch (record.getEventType()) {
			case PROD_GOOD: {
				equipmentLoss.incrementGoodQuantity(quantity);
				break;
			}

			case PROD_REJECT: {
				equipmentLoss.incrementRejectQuantity(quantity);

				// convert to a time loss
				Duration lostTime = equipmentLoss.convertToLostTime(quantity);
				record.setLostTime(lostTime);
				break;
			}

			case PROD_STARTUP: {
				equipmentLoss.incrementStartupQuantity(quantity);

				// convert to a time loss
				Duration lostTime = equipmentLoss.convertToLostTime(quantity);
				record.setLostTime(lostTime);
				break;
			}

			default:
				break;
			}
		}

		// time from measured availability losses
		List<OeeEvent> records = PersistenceService.instance().fetchAvailability(equipment, from, to);
		equipmentLoss.getEventRecords().addAll(records);

		for (int i = 0; i < records.size(); i++) {
			OeeEvent record = records.get(i);

			checkTimePeriod(record, equipmentLoss, from, to);

			Duration eventDuration = record.getDuration();
			Duration duration = eventDuration;

			OffsetDateTime start = record.getStartTime();
			OffsetDateTime end = record.getEndTime();

			// check first record for edge time
			if (i == 0) {
				// first record
				if (from.isAfter(start)) {
					// get time in interval
					Duration edge = Duration.between(start, from);
					duration = eventDuration.minus(edge);
				}
			} else if (i == (records.size() - 1)) {
				// last record
				if (end == null || to.isBefore(end)) {
					// get time in interval
					Duration edge = Duration.between(start, to);

					// clip to event duration
					if (eventDuration != null) {
						if (edge.compareTo(eventDuration) < 0) {
							duration = edge;
						}
					}
				}
			}

			equipmentLoss.incrementLoss(record.getReason(), duration);

			// save in event record
			record.setLostTime(duration);
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

		if (logger.isTraceEnabled()) {
			logger.trace(equipmentLoss.toString());
		}
	}

	private static void checkTimePeriod(OeeEvent record, EquipmentLoss equipmentLoss, OffsetDateTime from,
			OffsetDateTime to) {
		// beginning time
		OffsetDateTime recordStart = record.getStartTime();
		OffsetDateTime recordEnd = record.getEndTime();

		if (recordStart.isBefore(from)) {
			recordStart = from;
		}

		if (recordEnd == null || recordEnd.isAfter(to)) {
			recordEnd = to;
		}

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
			if (recordEnd != null && recordEnd.isAfter(lossEnd)) {
				equipmentLoss.setEndDateTime(recordEnd);
			}
		}
	}

	public static List<ParetoItem> getParetoData(EquipmentLoss equipmentLoss, TimeLoss loss) throws Exception {
		// create the items to chart
		Map<Reason, Duration> reasonMap = equipmentLoss.getLossReasonsByCategory(loss);

		List<ParetoItem> items = new ArrayList<>(reasonMap.entrySet().size());

		for (Entry<Reason, Duration> entry : reasonMap.entrySet()) {
			ParetoItem item = new ParetoItem(entry.getKey().getName(), entry.getValue());
			items.add(item);

			logger.info("Pareto Reason: " + entry.getKey().getName() + ", duration: " + entry.getValue());
		}

		return items;
	}
}
