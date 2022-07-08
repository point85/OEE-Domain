package org.point85.domain.plant;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.dto.EquipmentMaterialDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@Table(name = "EQUIPMENT_MATERIAL")
@AttributeOverride(name = "primaryKey", column = @Column(name = "EM_KEY"))

public class EquipmentMaterial extends KeyedObject {

	// OEE target
	@Column(name = "OEE_TARGET")
	private Double oeeTarget;

	// ideal run rate
	@Column(name = "RUN_AMOUNT")
	private Double runRateAmount;

	@OneToOne
	@JoinColumn(name = "RUN_UOM_KEY")
	private UnitOfMeasure runRateUOM;

	@OneToOne
	@JoinColumn(name = "REJECT_UOM_KEY")
	private UnitOfMeasure rejectUOM;

	@OneToOne
	@JoinColumn(name = "MAT_KEY")
	private Material material;

	@OneToOne
	@JoinColumn(name = "EQ_KEY")
	private Equipment equipment;

	@Column(name = "IS_DEFAULT")
	private Boolean isDefault = false;

	public EquipmentMaterial() {
		super();
	}

	public EquipmentMaterial(Equipment equipment, Material material) {
		this.equipment = equipment;
		this.material = material;
	}

	public EquipmentMaterial(EquipmentMaterialDto dto) throws Exception {
		this.isDefault = dto.getIsDefault();
		this.oeeTarget = dto.getOeeTarget();
		this.runRateAmount = dto.getRunRateAmount();

		if (dto.getRejectUOM() != null) {
			UnitOfMeasure uom = PersistenceService.instance().fetchUomBySymbol(dto.getRejectUOM());

			if (uom == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.uom", dto.getRejectUOM()));
			}
			this.rejectUOM = uom;
		}

		if (dto.getRunRateUOM() != null) {
			UnitOfMeasure uom = PersistenceService.instance().fetchUomBySymbol(dto.getRunRateUOM());

			if (uom == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.uom", dto.getRunRateUOM()));
			}
			this.runRateUOM = uom;
		}

		if (dto.getMaterial() != null) {
			Material dtoMaterial = PersistenceService.instance().fetchMaterialByName(dto.getMaterial());

			if (dtoMaterial == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.material", dto.getMaterial()));
			}
			this.material = dtoMaterial;
		}
	}

	public Double getOeeTarget() {
		return oeeTarget;
	}

	public void setOeeTarget(Double oeeTarget) {
		this.oeeTarget = oeeTarget;
	}

	public Quantity getRunRate() {
		return new Quantity(runRateAmount, runRateUOM);
	}

	public Double getRunRateAmount() {
		return runRateAmount;
	}

	public UnitOfMeasure getRunRateUOM() {
		return runRateUOM;
	}

	public void setRunRate(Quantity runRate) {
		this.runRateAmount = runRate.getAmount();
		this.runRateUOM = runRate.getUOM();
	}

	public void setRunRateAmount(Double amount) {
		this.runRateAmount = amount;
	}

	public void setRunRateUOM(UnitOfMeasure uom) {
		this.runRateUOM = uom;
	}

	public UnitOfMeasure getRejectUOM() {
		return rejectUOM;
	}

	public void setRejectUOM(UnitOfMeasure uom) {
		this.rejectUOM = uom;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getEquipment(), getMaterial());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof EquipmentMaterial)) {
			return false;
		}
		EquipmentMaterial otherEquipmentMaterial = (EquipmentMaterial) other;

		if (getMaterial() == null || getEquipment() == null) {
			return true;
		}

		return getEquipment().equals(otherEquipmentMaterial.getEquipment())
				&& getMaterial().equals(otherEquipmentMaterial.getMaterial());
	}

	public Boolean isDefault() {
		return isDefault;
	}

	public void setDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public String toString() {
		return "Matl: " + getMaterial().getName() + ", Speed: " + getRunRate();
	}

}
