package org.point85.domain.collector;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@Table(name = "PROD_HISTORY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ProductionHistory extends BaseEvent {
	@Column(name = "AMOUNT")
	private double amount;

	@OneToOne
	@JoinColumn(name = "UOM_KEY")
	private UnitOfMeasure uom;

	public ProductionHistory() {
		super();
	}

	public ProductionHistory(ResolvedEvent event) {
		super(event);
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
