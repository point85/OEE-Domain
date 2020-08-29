package org.point85.domain.uom.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.point85.domain.uom.Constant;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

public class TestSystems extends BaseTest {

	@Test
	public void testUnifiedSystem() throws Exception {

		assertFalse(sys.equals(null));

		Map<String, UnitOfMeasure> unitMap = new HashMap<>();

		// check the SI units
		for (Unit unit : Unit.values()) {
			UnitOfMeasure uom = sys.getUOM(unit);

			assertNotNull(uom);
			assertNotNull(uom.getName());
			assertNotNull(uom.getSymbol());
			assertNotNull(uom.getDescription());
			assertNotNull(uom.toString());
			assertNotNull(uom.getBaseSymbol());
			assertNotNull(uom.getAbscissaUnit());

			// symbol uniqueness
			assertFalse(unitMap.containsKey(uom.getSymbol()));
			unitMap.put(uom.getSymbol(), uom);
		}

		List<Unit> allUnits = new ArrayList<Unit>();

		for (Unit u : Unit.values()) {
			allUnits.add(u);
		}

		for (UnitOfMeasure uom : sys.getRegisteredUnits()) {
			if (uom.getEnumeration() != null) {
				assertTrue(allUnits.contains(uom.getEnumeration()));
			}
		}

		for (UnitType unitType : UnitType.values()) {
			UnitType found = null;
			for (UnitOfMeasure u : sys.getRegisteredUnits()) {
				if (u.getUnitType().equals(unitType)) {
					found = u.getUnitType();
					break;
				}
			}

			if (found == null && !unitType.equals(UnitType.UNCLASSIFIED)) {
				fail("No unit found for type " + unitType);
			}
		}

		// constants
		for (Constant c : Constant.values()) {
			assertTrue(sys.getQuantity(c) != null);
		}
	}

	@Test
	public void testCache() throws Exception {

		// unit cache
		sys.getOne();

		int before = sys.getRegisteredUnits().size();

		for (int i = 0; i < 10; i++) {
			sys.createScalarUOM(UnitType.UNCLASSIFIED, null, UUID.randomUUID().toString(), null);
		}

		int after = sys.getRegisteredUnits().size();

		assertTrue(after == (before + 10));

	}

	@Test
	public void testGetUnits() throws Exception {
		for (UnitType type : UnitType.values()) {
			List<UnitOfMeasure> uoms = sys.getUnitsOfMeasure(type);
			if (!type.equals(UnitType.UNCLASSIFIED)) {
				assertTrue(uoms.size() > 0);
			}
		}
	}
}
