package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.collector.OeeEvent;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EquipmentLossManager {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(EquipmentLossManager.class);

	private EquipmentLossManager() {
	}

	public static void calculateEquipmentLoss(EquipmentLoss equipmentLoss, OffsetDateTime from, OffsetDateTime to)
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

		for (OeeEvent record : productions) {
			Quantity quantity = record.getQuantity();

			if (quantity.getUOM() == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.uom.production", quantity.getAmount(),
						record.getSourceId(), record.getOffsetStartTime()));
			}
			Duration lostTime = null;

			switch (record.getEventType()) {
			case PROD_GOOD: {
				equipmentLoss.incrementGoodQuantity(quantity);
				break;
			}

			case PROD_REJECT: {
				equipmentLoss.incrementRejectQuantity(quantity);

				// convert to a time loss
				lostTime = equipmentLoss.convertToLostTime(quantity);
				record.setLostTime(lostTime);
				break;
			}

			case PROD_STARTUP: {
				equipmentLoss.incrementStartupQuantity(quantity);

				// convert to a time loss
				lostTime = equipmentLoss.convertToLostTime(quantity);
				record.setLostTime(lostTime);
				break;
			}

			default:
				break;
			}

			if (record.getReason() != null && lostTime != null) {
				// reason map too
				equipmentLoss.incrementReasonLoss(record.getReason(), lostTime);
			}
		}

		// availability losses
		List<OeeEvent> records = PersistenceService.instance().fetchAvailability(equipment, from, to);

		equipmentLoss.getEventRecords().addAll(records);

		for (int i = 0; i < records.size(); i++) {
			OeeEvent record = records.get(i);

			// skip no loss records
			if (record.getReason() != null) {
				TimeLoss lossCategory = record.getReason().getLossCategory();

				if (lossCategory.equals(TimeLoss.NO_LOSS)) {
					continue;
				}
			}

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

			// increment the loss for this reason
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
			Duration notScheduled = Duration.ZERO;
			Duration extraNotScheduled = Duration.ZERO;

			// from the work schedule
			WorkSchedule schedule = equipment.findWorkSchedule();
			if (schedule != null) {
				notScheduled = schedule.calculateNonWorkingTime(odtStart.toLocalDateTime(), odtEnd.toLocalDateTime());

				// add any additional time not scheduled
				extraNotScheduled = equipmentLoss.getLoss(TimeLoss.NOT_SCHEDULED);
			}
			equipmentLoss.setLoss(TimeLoss.NOT_SCHEDULED, notScheduled.plus(extraNotScheduled));
		}

		if (logger.isTraceEnabled()) {
			logger.trace(equipmentLoss.toString());
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
