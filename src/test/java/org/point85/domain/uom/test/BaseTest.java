package org.point85.domain.uom.test;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;

public class BaseTest {

	protected static double DELTA6 = 0.000001d;
	protected static double DELTA5 = 0.00001d;
	protected static double DELTA4 = 0.0001d;
	protected static double DELTA3 = 0.001d;
	protected static double DELTA2 = 0.01d;
	protected static double DELTA1 = 0.1d;
	protected static double DELTA0 = 1.0d;
	protected static double DELTA_10 = 10.0d;

	protected final static MeasurementSystem sys = MeasurementSystem.instance();

	protected BaseTest() {

	}

	protected void snapshotSymbolCache() {

		Map<String, UnitOfMeasure> treeMap = new TreeMap<>(sys.getSymbolCache());

		System.out.println("Symbol cache ...");
		int count = 0;
		for (Entry<String, UnitOfMeasure> entry : treeMap.entrySet()) {
			count++;
			System.out.println("(" + count + ") " + entry.getKey() + ", " + entry.getValue());
		}
	}

	protected void snapshotBaseSymbolCache() {
		Map<String, UnitOfMeasure> treeMap = new TreeMap<>(sys.getBaseSymbolCache());

		System.out.println("Base symbol cache ...");
		int count = 0;
		for (Entry<String, UnitOfMeasure> entry : treeMap.entrySet()) {
			count++;
			System.out.println("(" + count + ") " + entry.getKey() + ", " + entry.getValue());
		}
	}

	protected void snapshotUnitEnumerationCache() {
		Map<Unit, UnitOfMeasure> treeMap = new TreeMap<>(sys.getEnumerationCache());

		System.out.println("Enumeration cache ...");
		int count = 0;
		for (Entry<Unit, UnitOfMeasure> entry : treeMap.entrySet()) {
			count++;
			System.out.println("(" + count + ") " + entry.getKey() + ", " + entry.getValue());
		}
	}

	protected boolean isCloseTo(double actualValue, double expectedValue, double delta) {
		double diff = Math.abs(actualValue - expectedValue);
		return (diff <= delta) ? true : false;
	}
}
