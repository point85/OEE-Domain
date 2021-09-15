package org.point85.domain.oee;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

			// skip no loss records
			if (event.getReason() != null) {
				TimeLoss lossCategory = event.getReason().getLossCategory();

				if (lossCategory.equals(TimeLoss.NO_LOSS)) {
					continue;
				}
			}

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

	public static List<ParetoItem> getParetoData(EquipmentLoss equipmentLoss, TimeLoss loss) {
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

	public static void buildLoss(EquipmentLoss equipmentLoss, String materialId, OffsetDateTime odtStart,
			OffsetDateTime odtEnd) throws Exception {

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
