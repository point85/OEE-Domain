package org.point85.domain.collector;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

@Entity
@Table(name = "PRODUCTION")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AttributeOverride(name = "primaryKey", column = @Column(name = "PROD_KEY"))

public class ProductionRecord extends BaseRecord {

	@Column(name = "AMOUNT")
	private double amount;

	@OneToOne
	@JoinColumn(name = "UOM_KEY")
	private UnitOfMeasure uom;

	public ProductionRecord() {
		super();
	}

	public ProductionRecord(ResolvedEvent event) throws Exception {
		super(event);

		if (event.getQuantity() != null) {
			this.amount = event.getQuantity().getAmount();

			Equipment equipment = event.getEquipment();
			Material material = event.getMaterial();
			this.uom = equipment.getUOM(material, event.getResolverType());
		}

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

	public Quantity getQuantity() {
		return new Quantity(amount, uom);
	}
}
