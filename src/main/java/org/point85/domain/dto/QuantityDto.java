package org.point85.domain.dto;

import org.point85.domain.uom.Quantity;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for a quantity
 */
public class QuantityDto {
	@SerializedName(value = "amount")
	private double amount;

	@SerializedName(value = "uom")
	private String uom;

	public QuantityDto(Quantity quantity) {
		this.setAmount(quantity.getAmount());
		this.setUom(quantity.getUOM().getSymbol());
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getUom() {
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("amount: " + amount);
		sb.append(", uom: " + uom);
		return sb.toString();
	}
}
