package org.point85.domain.dto;

import org.point85.domain.uom.UnitOfMeasure;

public class UnitOfMeasureDto extends NamedObjectDto {
	private String symbol;

	private Double scalingFactor;

	private Double offset;

	private UnitOfMeasureDto abscissaUnit;

	private String unit;

	private String unitType;

	private Double bridgeScalingFactor;

	private Double bridgeOffset;

	private UnitOfMeasureDto bridgeAbscissaUnit;

	private String category;

	private UnitOfMeasureDto uom1;

	private UnitOfMeasureDto uom2;

	private Integer exponent1;

	private Integer exponent2;

	public UnitOfMeasureDto(UnitOfMeasure uom) {
		super(uom);

		if (!uom.isTerminal()) {
			this.setAbscissaUnit(new UnitOfMeasureDto(uom.getAbscissaUnit()));
		}

		if (uom.getUOM1() != null) {
			this.setUOM1(new UnitOfMeasureDto(uom.getUOM1()));
		}

		if (uom.getUOM2() != null) {
			this.setUOM2(new UnitOfMeasureDto(uom.getUOM2()));
		}

		if (uom.getBridgeAbscissaUnit() != null) {
			this.setBridgeAbscissaUnit(new UnitOfMeasureDto(uom.getBridgeAbscissaUnit()));
		}
		this.setBridgeOffset(uom.getBridgeOffset());
		this.setBridgeScalingFactor(uom.getBridgeScalingFactor());
		this.setCategory(uom.getCategory());
		this.setExponent1(uom.getExponent1());
		this.setExponent2(uom.getExponent2());
		this.setOffset(uom.getOffset());
		this.setScalingFactor(uom.getScalingFactor());
		this.setSymbol(uom.getSymbol());
		this.setUnit(uom.getEnumeration() != null ? uom.getEnumeration().name() : null);
		this.setUnitType(uom.getUnitType().name());
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Double getScalingFactor() {
		return scalingFactor;
	}

	public void setScalingFactor(Double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public Double getOffset() {
		return offset;
	}

	public void setOffset(Double offset) {
		this.offset = offset;
	}

	public UnitOfMeasureDto getAbscissaUnit() {
		return abscissaUnit;
	}

	public void setAbscissaUnit(UnitOfMeasureDto abscissaUnit) {
		this.abscissaUnit = abscissaUnit;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getUnitType() {
		return unitType;
	}

	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	public Double getBridgeScalingFactor() {
		return bridgeScalingFactor;
	}

	public void setBridgeScalingFactor(Double bridgeScalingFactor) {
		this.bridgeScalingFactor = bridgeScalingFactor;
	}

	public Double getBridgeOffset() {
		return bridgeOffset;
	}

	public void setBridgeOffset(Double bridgeOffset) {
		this.bridgeOffset = bridgeOffset;
	}

	public UnitOfMeasureDto getBridgeAbscissaUnit() {
		return bridgeAbscissaUnit;
	}

	public void setBridgeAbscissaUnit(UnitOfMeasureDto bridgeAbscissaUnit) {
		this.bridgeAbscissaUnit = bridgeAbscissaUnit;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public UnitOfMeasureDto getUOM1() {
		return uom1;
	}

	public void setUOM1(UnitOfMeasureDto uom1) {
		this.uom1 = uom1;
	}

	public UnitOfMeasureDto getUOM2() {
		return uom2;
	}

	public void setUOM2(UnitOfMeasureDto uom2) {
		this.uom2 = uom2;
	}

	public Integer getExponent1() {
		return exponent1;
	}

	public void setExponent1(Integer exponent1) {
		this.exponent1 = exponent1;
	}

	public Integer getExponent2() {
		return exponent2;
	}

	public void setExponent2(Integer exponent2) {
		this.exponent2 = exponent2;
	}
}
