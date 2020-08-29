package org.point85.domain.uom.test;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.point85.domain.uom.Unit;

public class TestCleanUp extends BaseTest {
	@AfterClass
	public static void cleanUp() throws Exception {

		assertTrue(sys.getSymbolCache().size() > 0);
		assertTrue(sys.getBaseSymbolCache().size() > 0);
		assertTrue(sys.getEnumerationCache().size() > 0);

		for (Unit unit : Unit.values()) {
			sys.getUOM(unit).clearCache();
		}

		sys.clearCache();

	}

}
