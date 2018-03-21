package org.point85.domain.performance;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.point85.domain.collector.AvailabilitySummary;
import org.point85.domain.collector.BaseSummary;
import org.point85.domain.collector.ProductionSummary;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.uom.Quantity;

public class EquipmentLossManager {
	public static EquipmentLoss calculateEquipmentLoss(Equipment equipment, Material material, OffsetDateTime from,
			OffsetDateTime to) throws Exception {
		// losses
		EquipmentLoss equipmentLoss = new EquipmentLoss();

		// from the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();
		if (schedule == null) {
			throw new Exception("A work schedule must be defined for this equipment.");
		}

		// time from measured availability losses
		List<AvailabilitySummary> availabilities = PersistenceService.instance().fetchAvailabilitySummary(equipment,
				from, to);

		for (AvailabilitySummary summary : availabilities) {
			checkTimePeriod(summary, equipmentLoss);

			TimeLoss loss = summary.getReason().getLossCategory();
			equipmentLoss.incrementLoss(loss, summary.getDuration());
		}

		// System.out.println(this.equipmentLoss.toString());

		// time from measured production
		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);

		if (eqm == null || eqm.getRunRate() == null) {
			throw new Exception("The design speed must be defined for equipment " + equipment.getName()
					+ " and material " + material.getDisplayString());
		}
		equipmentLoss.setDesignSpeed(eqm.getRunRate());

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

		// compute reduced speed from the other losses
		equipmentLoss.setReducedSpeedLoss();

		OffsetDateTime odtStart = equipmentLoss.getStartDateTime();
		OffsetDateTime odtEnd = equipmentLoss.getEndDateTime();

		if (odtStart != null && odtEnd != null) {
			Duration notScheduled = schedule.calculateNonWorkingTime(odtStart.toLocalDateTime(),
					odtEnd.toLocalDateTime());
			equipmentLoss.setLoss(TimeLoss.NOT_SCHEDULED, notScheduled);
		}

		return equipmentLoss;
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
}
