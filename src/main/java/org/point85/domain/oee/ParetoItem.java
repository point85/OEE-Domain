package org.point85.domain.oee;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

import org.point85.domain.opc.ua.packml.PackMLState;
import org.point85.domain.plant.Reason;

public class ParetoItem implements Comparable<ParetoItem> {
	// x-axis category
	private String category;

	// y-axis value
	private Number value;

	// time loss in this category
	private TimeLoss loss;

	// PackML state in this category
	private PackMLState packMLState;

	public ParetoItem(TimeLoss loss, Number value) {
		this.category = loss.toString();
		this.value = value;
		this.loss = loss;
	}

	public ParetoItem(PackMLState state, Number value) {
		this.category = state.toString();
		this.value = value;
		this.packMLState = state;
	}

	public ParetoItem(Reason reason, Number value) {
		this.category = reason.getName();
		this.value = value;
	}

	public ParetoItem(String category, Duration duration) {
		this.category = category;
		this.value = duration.getSeconds();
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
	public boolean equals(Object obj) {
		if (obj instanceof ParetoItem) {
			ParetoItem other = (ParetoItem) obj;
			if (category.equals(other.getCategory())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(category);
	}

	@Override
	public int compareTo(ParetoItem other) {
		// assume both are same class
		Number value1 = getValue();
		Number value2 = other.getValue();
		int comparison = 0;

		if (value1 instanceof BigDecimal) {
			return ((BigDecimal) value1).compareTo((BigDecimal) value2);
		} else if (value1 instanceof Integer) {
			return ((Integer) value1).compareTo((Integer) value2);
		} else if (value1 instanceof Float) {
			return ((Float) value1).compareTo((Float) value2);
		} else if (value1 instanceof Double) {
			return ((Double) value1).compareTo((Double) value2);
		} else if (value1 instanceof Long) {
			return ((Long) value1).compareTo((Long) value2);
		}

		return comparison;
	}

	@Override
	public String toString() {
		return "Category: " + category + ", Value: " + value;
	}

	public TimeLoss getLoss() {
		return loss;
	}

	public void setLoss(TimeLoss loss) {
		this.loss = loss;
	}

	public PackMLState getPackMLState() {
		return packMLState;
	}

	public void setPackMLState(PackMLState packMLState) {
		this.packMLState = packMLState;
	}
}
