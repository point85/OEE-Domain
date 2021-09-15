package org.point85.domain.dto;

import org.point85.domain.oee.ParetoItem;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object (DTO) for OEE Pareto item data
 */
public class ParetoItemDto {
	// x-axis category
	@SerializedName(value = "category")
	private String category;

	// y-axis value
	@SerializedName(value = "value")
	private Number value;

	@SerializedName(value = "timeLoss")
	private String timeLoss;

	public ParetoItemDto(ParetoItem item) {
		if (item.getLoss() != null) {
			this.setTimeLoss(item.getLoss().serialize());
		}
		this.value = item.getValue();
		this.category = item.getCategory();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("category: " + category);
		sb.append(", value: " + value);

		return sb.toString();
	}

	public String getTimeLoss() {
		return timeLoss;
	}

	public void setTimeLoss(String timeLoss) {
		this.timeLoss = timeLoss;
	}

}
