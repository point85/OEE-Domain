package org.point85.domain.uom.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

public class TestPerformance {

	// unit map
	private Map<UnitType, List<UnitOfMeasure>> unitListMap = new HashMap<UnitType, List<UnitOfMeasure>>();

	TestPerformance() throws Exception {
		MeasurementSystem sys = MeasurementSystem.instance();

		for (Unit u : Unit.values()) {
			UnitOfMeasure uom = sys.getUOM(u);
			this.addUnit(uom);
		}
	}

	private void addUnit(UnitOfMeasure uom) {
		List<UnitOfMeasure> unitList = unitListMap.get(uom.getUnitType());

		if (unitList == null) {
			unitList = new ArrayList<UnitOfMeasure>();
			unitListMap.put(uom.getUnitType(), unitList);
		}
		unitList.add(uom);
	}

	public void runSingleTest() throws Exception {
		// for each unit type, execute the quantity operations
		for (Entry<UnitType, List<UnitOfMeasure>> entry : unitListMap.entrySet()) {
			// run the matrix
			for (UnitOfMeasure rowUOM : entry.getValue()) {

				// row quantity
				Quantity rowQty = new Quantity(10.0d, rowUOM);

				for (UnitOfMeasure colUOM : entry.getValue()) {

					// column qty
					Quantity colQty = new Quantity(10.0d, colUOM);

					// arithmetic operations
					rowQty.add(colQty);
					rowQty.subtract(colQty);

					// offsets are not supported
					if (rowUOM.getOffset() == 0.0d && colUOM.getOffset() == 0.0d) {
						rowQty.multiply(colQty);
						rowQty.divide(colQty);
						rowQty.invert();
					}

					rowQty.convert(colUOM);
					rowQty.equals(colQty);
					rowQty.getAmount();
					rowQty.getUOM();
					rowQty.toString();
				}
			}
		}
	}
}
