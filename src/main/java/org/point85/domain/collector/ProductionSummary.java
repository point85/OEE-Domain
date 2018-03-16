package org.point85.domain.collector;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.persistence.EventResolverTypeConverter;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@Table(name = "PROD_SUMMARY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ProductionSummary extends BaseSummary {
	@Column(name = "TYPE")
	@Convert(converter = EventResolverTypeConverter.class)
	private EventResolverType type;

	@Column(name = "AMOUNT")
	private double amount;

	@OneToOne
	@JoinColumn(name = "UOM_KEY")
	private UnitOfMeasure uom;

	public ProductionSummary() {
		super();
	}

	public ProductionSummary(LossSummary summary) {
		super(summary);
		this.type = summary.getResolverType();

		if (summary.getQuantity() != null) {
			this.amount = summary.getQuantity().getAmount();
			this.uom = summary.getQuantity().getUOM();
		}
	}

	public EventResolverType getType() {
		return type;
	}

	public void setType(EventResolverType type) {
		this.type = type;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public UnitOfMeasure getUOM() {
		return uom;
	}

	public void setUOM(UnitOfMeasure uom) {
		this.uom = uom;
	}

}
