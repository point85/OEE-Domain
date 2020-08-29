package org.point85.domain.uom.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.point85.domain.uom.Constant;
import org.point85.domain.uom.Quantity;

public class TestNamedQuantity extends BaseTest {
	@Test
	public void testCase() throws Exception {

		for (Constant value : Constant.values()) {
			Quantity q = sys.getQuantity(value);
			assertTrue(q.getName() != null);
			assertTrue(q.getSymbol() != null);
			assertTrue(q.getDescription() != null);
			assertTrue(q.getUOM() != null);
			assertTrue(q.toString() != null);
		}
	}
}
