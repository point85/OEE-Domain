package org.point85.domain.uom.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.point85.domain.uom.Prefix;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

public class TestBridges extends BaseTest {

	@Test
	public void testBridges() throws Exception {

		// SI
		UnitOfMeasure kg = sys.getUOM(Unit.KILOGRAM);
		UnitOfMeasure m = sys.getUOM(Unit.METRE);
		UnitOfMeasure km = sys.getUOM(Prefix.KILO, m);
		UnitOfMeasure litre = sys.getUOM(Unit.LITRE);
		UnitOfMeasure N = sys.getUOM(Unit.NEWTON);
		UnitOfMeasure m3 = sys.getUOM(Unit.CUBIC_METRE);
		UnitOfMeasure m2 = sys.getUOM(Unit.SQUARE_METRE);
		UnitOfMeasure Nm = sys.getUOM(Unit.NEWTON_METRE);
		UnitOfMeasure kPa = sys.getUOM(Prefix.KILO, sys.getUOM(Unit.PASCAL));
		UnitOfMeasure celsius = sys.getUOM(Unit.CELSIUS);

		// US
		UnitOfMeasure lbm = sys.getUOM(Unit.POUND_MASS);
		UnitOfMeasure lbf = sys.getUOM(Unit.POUND_FORCE);
		UnitOfMeasure mi = sys.getUOM(Unit.MILE);
		UnitOfMeasure ft = sys.getUOM(Unit.FOOT);
		UnitOfMeasure gal = sys.getUOM(Unit.US_GALLON);
		UnitOfMeasure ft2 = sys.getUOM(Unit.SQUARE_FOOT);
		UnitOfMeasure ft3 = sys.getUOM(Unit.CUBIC_FOOT);
		UnitOfMeasure acre = sys.getUOM(Unit.ACRE);
		UnitOfMeasure ftlbf = sys.getUOM(Unit.FOOT_POUND_FORCE);
		UnitOfMeasure psi = sys.getUOM(Unit.PSI);
		UnitOfMeasure fahrenheit = sys.getUOM(Unit.FAHRENHEIT);

		assertTrue(ft.getBridgeOffset() == 0.0d);

		Quantity q1 = new Quantity(10d, ft);
		Quantity q2 = q1.convert(m);
		assertTrue(isCloseTo(q2.getAmount(), 3.048, DELTA6));
		Quantity q3 = q2.convert(q1.getUOM());
		assertTrue(isCloseTo(q3.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, kg);
		q2 = q1.convert(lbm);
		assertTrue(isCloseTo(q2.getAmount(), 22.0462, DELTA4));
		q3 = q2.convert(q1.getUOM());
		assertTrue(isCloseTo(q3.getAmount(), 10d, DELTA6));

		q1 = new Quantity(212d, fahrenheit);
		q2 = q1.convert(celsius);
		assertTrue(isCloseTo(q2.getAmount(), 100, DELTA6));
		q3 = q2.convert(q1.getUOM());
		assertTrue(isCloseTo(q3.getAmount(), 212, DELTA6));

		UnitOfMeasure mm = sys.createProductUOM(UnitType.AREA, "name", "mxm", "", m, m);

		q1 = new Quantity(10d, mm);
		q2 = q1.convert(ft2);
		assertTrue(isCloseTo(q2.getAmount(), 107.639104167, DELTA6));
		q2 = q2.convert(m2);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		UnitOfMeasure mhr = sys.getUOM("m/hr");

		if (mhr == null) {
			mhr = sys.createScalarUOM(UnitType.VELOCITY, "m/hr", "m/hr", "");
			mhr.setConversion(1d / 3600d, sys.getUOM(Unit.METRE_PER_SEC));
		}

		q1 = new Quantity(10d, psi);
		q2 = q1.convert(kPa);
		assertTrue(isCloseTo(q2.getAmount(), 68.94757280343134, DELTA6));
		q2 = q2.convert(psi);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, mhr);
		q2 = q1.convert(sys.getUOM(Unit.FEET_PER_SEC));
		assertTrue(isCloseTo(q2.getAmount(), 0.009113444152814231, DELTA6));
		q2 = q2.convert(mhr);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, gal);
		q2 = q1.convert(litre);
		assertTrue(isCloseTo(q2.getAmount(), 37.8541178, DELTA6));
		q2 = q2.convert(gal);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, m3);
		q2 = q1.convert(ft3);
		assertTrue(isCloseTo(q2.getAmount(), 353.1466672398284, DELTA6));
		q2 = q2.convert(m3);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, N);
		q2 = q1.convert(lbf);
		assertTrue(isCloseTo(q2.getAmount(), 2.24809, DELTA6));
		q2 = q2.convert(N);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, ftlbf);
		q2 = q1.convert(Nm);
		assertTrue(isCloseTo(q2.getAmount(), 13.558179483314004, DELTA6));
		q2 = q2.convert(ftlbf);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, lbm);
		q2 = q1.convert(kg);
		assertTrue(isCloseTo(q2.getAmount(), 4.5359237, DELTA6));
		q2 = q2.convert(lbm);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(10d, km);
		q2 = q1.convert(mi);
		assertTrue(isCloseTo(q2.getAmount(), 6.21371192237, DELTA6));
		q2 = q2.convert(km);
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		// length
		q1 = new Quantity(10d, sys.getUOM(Unit.METRE));
		q2 = q1.convert(sys.getUOM(Unit.INCH));
		assertTrue(isCloseTo(q2.getAmount(), 393.7007874015748, DELTA6));
		q2 = q2.convert(sys.getUOM(Unit.METRE));
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q2 = q1.convert(sys.getUOM(Unit.FOOT));
		assertTrue(isCloseTo(q2.getAmount(), 32.80839895013123, DELTA6));
		q2 = q2.convert(sys.getUOM(Unit.METRE));
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		// area
		q1 = new Quantity(10d, sys.getUOM(Unit.SQUARE_METRE));
		q2 = q1.convert(sys.getUOM(Unit.SQUARE_INCH));
		assertTrue(isCloseTo(q2.getAmount(), 15500.031000062, DELTA6));
		q2 = q2.convert(sys.getUOM(Unit.SQUARE_METRE));
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q2 = q1.convert(sys.getUOM(Unit.SQUARE_FOOT));
		assertTrue(isCloseTo(q2.getAmount(), 107.6391041670972, DELTA6));
		q2 = q2.convert(sys.getUOM(Unit.SQUARE_METRE));
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		// volume
		q1 = new Quantity(10d, sys.getUOM(Unit.LITRE));
		q2 = q1.convert(sys.getUOM(Unit.US_GALLON));
		assertTrue(isCloseTo(q2.getAmount(), 2.641720523581484, DELTA6));
		q2 = q2.convert(sys.getUOM(Unit.LITRE));
		assertTrue(isCloseTo(q2.getAmount(), 10d, DELTA6));

		q1 = new Quantity(4.0468564224, m);
		q2 = new Quantity(1000d, m);
		q3 = q1.multiply(q2);
		assertTrue(isCloseTo(q3.getAmount(), 4046.8564224, DELTA6));

		UnitOfMeasure uom = q3.getUOM();
		UnitOfMeasure base = uom.getPowerBase();
		double sf = uom.getScalingFactor();

		assertTrue(uom.getAbscissaUnit().equals(m2));
		assertTrue(base.equals(m));
		assertTrue(isCloseTo(sf, 1d, DELTA6));

		Quantity q4 = q3.convert(acre);
		assertTrue(isCloseTo(q4.getAmount(), 1d, DELTA6));
		assertTrue(q4.getUOM().equals(acre));

		UnitOfMeasure usSec = sys.getSecond();

		UnitOfMeasure v1 = sys.getUOM("m/hr");

		UnitOfMeasure v2 = sys.getUOM(Unit.METRE_PER_SEC);
		UnitOfMeasure v3 = sys.createQuotientUOM(UnitType.VELOCITY, "", "ft/usec", "", ft, usSec);

		UnitOfMeasure d1 = sys.getUOM(Unit.KILOGRAM_PER_CU_METRE);
		UnitOfMeasure d2 = sys.createQuotientUOM(UnitType.DENSITY, "density", "lbm/gal", "", lbm, gal);

		q1 = new Quantity(10d, v1);
		q2 = q1.convert(v3);

		q1 = new Quantity(10d, v1);
		q2 = q1.convert(v2);

		q1 = new Quantity(10d, d1);
		q2 = q1.convert(d2);

	}

	@Test
	public void testBridgeUnits() throws Exception {
		UnitOfMeasure bridge1 = sys.createScalarUOM(UnitType.UNCLASSIFIED, "Bridge1", "B1", "description");
		UnitOfMeasure bridge2 = sys.createScalarUOM(UnitType.UNCLASSIFIED, "Bridge2", "B2", "description");

		bridge1.setBridgeConversion(1d, bridge2, 0d);
		assertTrue(bridge1.getBridgeScalingFactor() == 1d);
		assertTrue(bridge1.getBridgeAbscissaUnit() == bridge2);
		assertTrue(bridge1.getBridgeOffset() == 0d);

		try {
			bridge1.setConversion(10d, bridge1, 0d);
			fail();
		} catch (Exception e) {

		}

		try {
			bridge1.setConversion(1d, bridge1, 10d);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void testGrade() throws Exception {
		UnitOfMeasure uomGrams = sys.getUOM(Unit.GRAM);

		UnitOfMeasure troyOzSI = sys.createScalarUOM(UnitType.MASS, "troy ounce SI", "troy oz",
				"Troy ounce SI conversion");
		troyOzSI.setConversion(31.1034768, uomGrams);

		UnitOfMeasure uomTonnes = sys.getUOM(Unit.TONNE);
		UnitOfMeasure uomShortTons = sys.getUOM(Unit.US_TON);
		UnitOfMeasure uomTroyOz = sys.getUOM(Unit.TROY_OUNCE);
		UnitOfMeasure uomGramsPerTonne = sys.createQuotientUOM(uomGrams, uomTonnes);
		UnitOfMeasure uomTonnePerGram = sys.createQuotientUOM(uomTonnes, uomGrams);
		UnitOfMeasure uomTroyOzPerTonne = sys.createQuotientUOM(troyOzSI, uomTonnes);
		UnitOfMeasure uomTonnePerTroyOz = sys.createQuotientUOM(uomTonnes, troyOzSI);

		// grams per metric ton
		UnitOfMeasure uomPennyweight = sys.createScalarUOM(UnitType.MASS, "pennyweight", "dwt", "Pennyweight");
		uomPennyweight.setConversion(0.05, uomTroyOz);

		assertTrue(isCloseTo(uomPennyweight.getConversionFactor(uomGrams), 1.5551738d, DELTA6));

		UnitOfMeasure uomPennyweightPerShortTon = sys.createQuotientUOM(uomPennyweight, uomShortTons);

		Quantity qGrade = null;
		Quantity qConverted = null;
		Quantity qBack = null;

		// troy oz per metric ton
		qGrade = new Quantity(0.95, uomGramsPerTonne);

		qConverted = qGrade.convert(uomPennyweightPerShortTon);
		assertTrue(isCloseTo(qConverted.getAmount(), 0.554167d, DELTA6));
		qBack = qConverted.convert(uomGramsPerTonne);
		assertTrue(isCloseTo(qBack.getAmount(), 0.95d, DELTA6));

		qConverted = qGrade.convert(uomTroyOzPerTonne);
		assertTrue(isCloseTo(qConverted.getAmount(), 0.0305432d, DELTA6));
		qBack = qConverted.convert(uomGramsPerTonne);
		assertTrue(qBack.equals(qGrade));

		// metric ton per troy oz
		qGrade = new Quantity(1.0526316d, uomTonnePerGram);
		qConverted = qGrade.convert(uomTonnePerTroyOz);
		assertTrue(isCloseTo(qConverted.getAmount(), 32.7405025d, DELTA6));
		qBack = qConverted.convert(uomTonnePerGram);
		assertTrue(qBack.equals(qGrade));
	}
}
