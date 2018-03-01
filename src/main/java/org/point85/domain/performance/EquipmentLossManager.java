package org.point85.domain.performance;

import java.time.Duration;

import org.point85.domain.plant.Equipment;

public class EquipmentLossManager {
	
	public EquipmentLossManager() {
		
	}

	// TODO remove
	public static EquipmentLoss getEquipmentLoss(Equipment equipment) {
		EquipmentLoss equipmentLoss = new EquipmentLoss();
		equipmentLoss.setTotalTime(Duration.ofSeconds(240 * 60));
		equipmentLoss.setLoss(TimeLoss.UNSCHEDULED, Duration.ofSeconds(40 * 60));
		equipmentLoss.setLoss(TimeLoss.MINOR_STOPPAGES, Duration.ofSeconds(40 * 60));
		equipmentLoss.setLoss(TimeLoss.PLANNED_DOWNTIME, Duration.ofSeconds(10 * 60));
		equipmentLoss.setLoss(TimeLoss.SETUP, Duration.ofSeconds(16 * 60));
		equipmentLoss.setLoss(TimeLoss.UNPLANNED_DOWNTIME, Duration.ofSeconds(24 * 60));
		equipmentLoss.setLoss(TimeLoss.REDUCED_SPEED, Duration.ofSeconds(8 * 60));
		equipmentLoss.setLoss(TimeLoss.REJECT_REWORK, Duration.ofSeconds(2 * 60));

		return equipmentLoss;
	}
}
