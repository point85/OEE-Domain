package org.point85.domain.dto;

import org.point85.domain.plant.EquipmentMaterial;

public class EquipmentMaterialDto {
	private Double oeeTarget;

	private Double runRateAmount;

	private String runRateUOM;

	private String rejectUOM;

	private String material;

	private Boolean isDefault;

	public EquipmentMaterialDto(EquipmentMaterial equipmentMaterial) {
		this.oeeTarget = equipmentMaterial.getOeeTarget();
		this.runRateAmount = equipmentMaterial.getRunRateAmount();
		this.runRateUOM = equipmentMaterial.getRunRateUOM() != null ? equipmentMaterial.getRunRateUOM().getSymbol()
				: null;
		this.rejectUOM = equipmentMaterial.getRejectUOM() != null ? equipmentMaterial.getRejectUOM().getSymbol() : null;
		this.material = equipmentMaterial.getMaterial() != null ? equipmentMaterial.getMaterial().getName() : null;
		this.isDefault = equipmentMaterial.isDefault();
	}

	public Double getOeeTarget() {
		return oeeTarget;
	}

	public void setOeeTarget(Double oeeTarget) {
		this.oeeTarget = oeeTarget;
	}

	public Double getRunRateAmount() {
		return runRateAmount;
	}

	public void setRunRateAmount(Double runRateAmount) {
		this.runRateAmount = runRateAmount;
	}

	public String getRunRateUOM() {
		return runRateUOM;
	}

	public void setRunRateUOM(String runRateUOM) {
		this.runRateUOM = runRateUOM;
	}

	public String getRejectUOM() {
		return rejectUOM;
	}

	public void setRejectUOM(String rejectUOM) {
		this.rejectUOM = rejectUOM;
	}

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

}
