/*
MIT License

Copyright (c) 2016 Kent Randall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.point85.domain.uom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.persistence.PersistenceService;

/**
 * A MeasurementSystem is a collection of units of measure that have a linear
 * relationship to each other: y = ax + b where x is the unit to be converted, y
 * is the converted unit, a is the scaling factor and b is the offset. <br>
 * See
 * <ul>
 * <li>Wikipedia: <i><a href=
 * "https://en.wikipedia.org/wiki/International_System_of_Units">International
 * System of Units</a></i></li>
 * <li>Table of conversions:
 * <i><a href="https://en.wikipedia.org/wiki/Conversion_of_units">Conversion of
 * Units</a></i></li>
 * <li>Unified Code for Units of Measure :
 * <i><a href="http://unitsofmeasure.org/trac">UCUM</a></i></li>
 * <li>SI derived units:
 * <i><a href="https://en.wikipedia.org/wiki/SI_derived_unit">SI Derived
 * Units</a></i></li>
 * <li>US system:
 * <i><a href="https://en.wikipedia.org/wiki/United_States_customary_units">US
 * Units</a></i></li>
 * <li>British Imperial system:
 * <i><a href="https://en.wikipedia.org/wiki/Imperial_units">British Imperial
 * Units</a></i></li>
 * <li>JSR 363: <i><a href=
 * "https://java.net/downloads/unitsofmeasurement/JSR363Specification_EDR.pdf">JSR
 * 363 Specification</a></i></li>
 * </ul>
 * <br>
 * The MeasurementSystem class creates:
 * <ul>
 * <li>7 SI fundamental units of measure</li>
 * <li>20 SI units derived from these fundamental units</li>
 * <li>other units in the International Customary, US and British Imperial
 * systems</li>
 * <li>any number of custom units of measure</li>
 * </ul>
 *
 */

public class MeasurementSystem {
	// standard unified system Singleton
	private static MeasurementSystem unifiedSystem;

	// UOM cache manager
	private final CacheManager cacheManager;

	private MeasurementSystem() {
		cacheManager = new CacheManager();
	}

	/**
	 * Get the unified system of units of measure for International Customary, SI,
	 * US, British Imperial as well as custom systems
	 * 
	 * @return {@link MeasurementSystem}
	 */
	public static synchronized MeasurementSystem instance() {
		if (unifiedSystem == null) {
			unifiedSystem = new MeasurementSystem();
		}
		return unifiedSystem;
	}

	private UnitOfMeasure createUOM(Unit enumeration) throws Exception {
		UnitOfMeasure uom = null;

		// SI
		uom = createSIUnit(enumeration);

		if (uom != null) {
			return uom;
		}

		// Customary
		uom = createCustomaryUnit(enumeration);

		if (uom != null) {
			return uom;
		}

		// US
		uom = createUSUnit(enumeration);

		if (uom != null) {
			return uom;
		}

		// British
		uom = createBRUnit(enumeration);

		if (uom != null) {
			return uom;
		}

		// currency
		uom = createFinancialUnit(enumeration);

		return uom;
	}

	/**
	 * Fetch a unit of measure by its symbol
	 * 
	 * @param symbol Symbol
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getUomBySymbol(String symbol) throws Exception {
		if (symbol == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("symbol.cannot.be.null"));
		}

		// try cache
		UnitOfMeasure uom = getUOM(symbol);

		if (uom == null) {
			// not cached
			uom = PersistenceService.instance().fetchUomBySymbol(symbol);
		}

		if (uom == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("no.uom", symbol));
		}
		return uom;
	}

	/**
	 * Get the quantity defined as a contant value
	 * 
	 * @param constant {@link Constant}
	 * @return {@link Quantity}
	 * @throws Exception Exception
	 */
	public final Quantity getQuantity(Constant constant) throws Exception {
		Quantity named = null;

		switch (constant) {
		case LIGHT_VELOCITY:
			named = new Quantity(299792458d, getUOM(Unit.METRE_PER_SEC));
			named.setName(DomainLocalizer.instance().getUnitString("light.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("light.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("light.desc"));
			break;

		case LIGHT_YEAR:
			Quantity year = new Quantity(1.0, getUOM(Unit.JULIAN_YEAR));
			named = getQuantity(Constant.LIGHT_VELOCITY).multiply(year);
			named.setName(DomainLocalizer.instance().getUnitString("ly.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("ly.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("ly.desc"));
			break;

		case GRAVITY:
			named = new Quantity(9.80665, getUOM(Unit.METRE_PER_SEC_SQUARED));
			named.setName(DomainLocalizer.instance().getUnitString("gravity.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("gravity.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("gravity.desc"));
			break;

		case PLANCK_CONSTANT:
			UnitOfMeasure js = createProductUOM(getUOM(Unit.JOULE), getSecond());
			named = new Quantity(6.62607015E-34, js);
			named.setName(DomainLocalizer.instance().getUnitString("planck.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("planck.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("planck.desc"));
			break;

		case BOLTZMANN_CONSTANT:
			UnitOfMeasure jk = createQuotientUOM(getUOM(Unit.JOULE), getUOM(Unit.KELVIN));
			named = new Quantity(1.380649E-23, jk);
			named.setName(DomainLocalizer.instance().getUnitString("boltzmann.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("boltzmann.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("boltzmann.desc"));
			break;

		case AVAGADRO_CONSTANT:
			// NA
			named = new Quantity(6.02214076E+23, getOne());
			named.setName(DomainLocalizer.instance().getUnitString("avo.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("avo.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("avo.desc"));
			break;

		case GAS_CONSTANT:
			// R
			named = getQuantity(Constant.BOLTZMANN_CONSTANT).multiply(getQuantity(Constant.AVAGADRO_CONSTANT));
			named.setName(DomainLocalizer.instance().getUnitString("gas.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("gas.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("gas.desc"));
			break;

		case ELEMENTARY_CHARGE:
			// e
			named = new Quantity(1.602176634E-19, getUOM(Unit.COULOMB));
			named.setName(DomainLocalizer.instance().getUnitString("e.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("e.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("e.desc"));
			break;

		case FARADAY_CONSTANT:
			// F = e.NA
			Quantity qe = getQuantity(Constant.ELEMENTARY_CHARGE);
			named = qe.multiply(getQuantity(Constant.AVAGADRO_CONSTANT));
			named.setName(DomainLocalizer.instance().getUnitString("faraday.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("faraday.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("faraday.desc"));
			break;

		case ELECTRIC_PERMITTIVITY:
			// epsilon0 = 1/(mu0*c^2)
			Quantity vc = getQuantity(Constant.LIGHT_VELOCITY);
			named = getQuantity(Constant.MAGNETIC_PERMEABILITY).multiply(vc).multiply(vc).invert();
			named.setName(DomainLocalizer.instance().getUnitString("eps0.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("eps0.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("eps0.desc"));
			break;

		case MAGNETIC_PERMEABILITY:
			// mu0
			UnitOfMeasure hm = createQuotientUOM(getUOM(Unit.HENRY), getUOM(Unit.METRE));
			double fourPi = 4.0 * Math.PI * 1.0E-07;
			named = new Quantity(fourPi, hm);
			named.setName(DomainLocalizer.instance().getUnitString("mu0.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("mu0.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("mu0.desc"));
			break;

		case ELECTRON_MASS:
			// me
			named = new Quantity(9.1093835611E-28, getUOM(Unit.GRAM));
			named.setName(DomainLocalizer.instance().getUnitString("me.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("me.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("me.desc"));
			break;

		case PROTON_MASS:
			// mp
			named = new Quantity(1.67262189821E-24, getUOM(Unit.GRAM));
			named.setName(DomainLocalizer.instance().getUnitString("mp.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("mp.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("mp.desc"));
			break;

		case STEFAN_BOLTZMANN:
			UnitOfMeasure k4 = createPowerUOM(getUOM(Unit.KELVIN), 4);
			UnitOfMeasure sb = createQuotientUOM(getUOM(Unit.WATTS_PER_SQ_METRE), k4);
			named = new Quantity(5.67036713E-08, sb);
			named.setName(DomainLocalizer.instance().getUnitString("sb.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("sb.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("sb.desc"));
			break;

		case HUBBLE_CONSTANT:
			UnitOfMeasure kps = getUOM(Prefix.KILO, getUOM(Unit.METRE_PER_SEC));
			UnitOfMeasure mpc = getUOM(Prefix.MEGA, getUOM(Unit.PARSEC));
			UnitOfMeasure hubble = createQuotientUOM(kps, mpc);
			named = new Quantity(71.9, hubble);
			named.setName(DomainLocalizer.instance().getUnitString("hubble.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("hubble.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("hubble.desc"));
			break;

		case CAESIUM_FREQUENCY:
			named = new Quantity(9192631770d, getUOM(Unit.HERTZ));
			named.setName(DomainLocalizer.instance().getUnitString("caesium.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("caesium.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("caesium.desc"));
			break;

		case LUMINOUS_EFFICACY:
			UnitOfMeasure kcd = createQuotientUOM(getUOM(Unit.LUMEN), getUOM(Unit.WATT));
			named = new Quantity(683d, kcd);
			named.setName(DomainLocalizer.instance().getUnitString("kcd.name"));
			named.setSymbol(DomainLocalizer.instance().getUnitString("kcd.symbol"));
			named.setDescription(DomainLocalizer.instance().getUnitString("kcd.desc"));
			break;

		default:
			break;
		}

		return named;
	}

	private UnitOfMeasure createSIUnit(Unit unit) throws Exception {
		// In addition to the two dimensionless derived units radian (rad) and
		// steradian (sr), 20 other derived units have special names as defined
		// below. The seven fundamental SI units are metre, kilogram, kelvin,
		// ampere, candela and mole.

		UnitOfMeasure uom = null;

		switch (unit) {

		case ONE:
			// unity
			uom = createScalarUOM(UnitType.UNITY, Unit.ONE, DomainLocalizer.instance().getUnitString("one.name"),
					DomainLocalizer.instance().getUnitString("one.symbol"),
					DomainLocalizer.instance().getUnitString("one.desc"));
			break;

		case PERCENT:
			uom = createScalarUOM(UnitType.UNITY, Unit.PERCENT,
					DomainLocalizer.instance().getUnitString("percent.name"),
					DomainLocalizer.instance().getUnitString("percent.symbol"),
					DomainLocalizer.instance().getUnitString("percent.desc"));
			uom.setConversion(0.01, getOne());
			break;

		case SECOND:
			// second
			uom = createScalarUOM(UnitType.TIME, Unit.SECOND, DomainLocalizer.instance().getUnitString("sec.name"),
					DomainLocalizer.instance().getUnitString("sec.symbol"),
					DomainLocalizer.instance().getUnitString("sec.desc"));
			break;

		case MINUTE:
			// minute
			uom = createScalarUOM(UnitType.TIME, Unit.MINUTE, DomainLocalizer.instance().getUnitString("min.name"),
					DomainLocalizer.instance().getUnitString("min.symbol"),
					DomainLocalizer.instance().getUnitString("min.desc"));
			uom.setConversion(60d, getUOM(Unit.SECOND));
			break;

		case HOUR:
			// hour
			uom = createScalarUOM(UnitType.TIME, Unit.HOUR, DomainLocalizer.instance().getUnitString("hr.name"),
					DomainLocalizer.instance().getUnitString("hr.symbol"),
					DomainLocalizer.instance().getUnitString("hr.desc"));
			uom.setConversion(3600d, getUOM(Unit.SECOND));
			break;

		case DAY:
			// day
			uom = createScalarUOM(UnitType.TIME, Unit.DAY, DomainLocalizer.instance().getUnitString("day.name"),
					DomainLocalizer.instance().getUnitString("day.symbol"),
					DomainLocalizer.instance().getUnitString("day.desc"));
			uom.setConversion(86400d, getUOM(Unit.SECOND));
			break;

		case WEEK:
			// week
			uom = createScalarUOM(UnitType.TIME, Unit.WEEK, DomainLocalizer.instance().getUnitString("week.name"),
					DomainLocalizer.instance().getUnitString("week.symbol"),
					DomainLocalizer.instance().getUnitString("week.desc"));
			uom.setConversion(604800d, getUOM(Unit.SECOND));
			break;

		case JULIAN_YEAR:
			// Julian year
			uom = createScalarUOM(UnitType.TIME, Unit.JULIAN_YEAR,
					DomainLocalizer.instance().getUnitString("jyear.name"),
					DomainLocalizer.instance().getUnitString("jyear.symbol"),
					DomainLocalizer.instance().getUnitString("jyear.desc"));
			uom.setConversion(3.1557600E+07, getUOM(Unit.SECOND));
			break;

		case SQUARE_SECOND:
			// square second
			uom = createPowerUOM(UnitType.TIME_SQUARED, Unit.SQUARE_SECOND,
					DomainLocalizer.instance().getUnitString("s2.name"),
					DomainLocalizer.instance().getUnitString("s2.symbol"),
					DomainLocalizer.instance().getUnitString("s2.desc"), getUOM(Unit.SECOND), 2);
			break;

		case MOLE:
			// substance amount
			uom = createScalarUOM(UnitType.SUBSTANCE_AMOUNT, Unit.MOLE,
					DomainLocalizer.instance().getUnitString("mole.name"),
					DomainLocalizer.instance().getUnitString("mole.symbol"),
					DomainLocalizer.instance().getUnitString("mole.desc"));
			break;

		case EQUIVALENT:
			// substance amount
			uom = createScalarUOM(UnitType.SUBSTANCE_AMOUNT, Unit.EQUIVALENT,
					DomainLocalizer.instance().getUnitString("equivalent.name"),
					DomainLocalizer.instance().getUnitString("equivalent.symbol"),
					DomainLocalizer.instance().getUnitString("equivalent.desc"));
			break;

		case DECIBEL:
			// decibel
			uom = createScalarUOM(UnitType.INTENSITY, Unit.DECIBEL, DomainLocalizer.instance().getUnitString("db.name"),
					DomainLocalizer.instance().getUnitString("db.symbol"),
					DomainLocalizer.instance().getUnitString("db.desc"));
			break;

		case RADIAN:
			// plane angle radian (rad)
			uom = createScalarUOM(UnitType.PLANE_ANGLE, Unit.RADIAN,
					DomainLocalizer.instance().getUnitString("radian.name"),
					DomainLocalizer.instance().getUnitString("radian.symbol"),
					DomainLocalizer.instance().getUnitString("radian.desc"));
			uom.setConversion(getOne());
			break;

		case STERADIAN:
			// solid angle steradian (sr)
			uom = createScalarUOM(UnitType.SOLID_ANGLE, Unit.STERADIAN,
					DomainLocalizer.instance().getUnitString("steradian.name"),
					DomainLocalizer.instance().getUnitString("steradian.symbol"),
					DomainLocalizer.instance().getUnitString("steradian.desc"));
			uom.setConversion(getOne());
			break;

		case DEGREE:
			// degree of arc
			uom = createScalarUOM(UnitType.PLANE_ANGLE, Unit.DEGREE,
					DomainLocalizer.instance().getUnitString("degree.name"),
					DomainLocalizer.instance().getUnitString("degree.symbol"),
					DomainLocalizer.instance().getUnitString("degree.desc"));
			uom.setConversion(Math.PI / 180d, getUOM(Unit.RADIAN));
			break;

		case ARC_SECOND:
			// degree of arc
			uom = createScalarUOM(UnitType.PLANE_ANGLE, Unit.ARC_SECOND,
					DomainLocalizer.instance().getUnitString("arcsec.name"),
					DomainLocalizer.instance().getUnitString("arcsec.symbol"),
					DomainLocalizer.instance().getUnitString("arcsec.desc"));
			uom.setConversion(Math.PI / 648000d, getUOM(Unit.RADIAN));
			break;

		case METRE:
			// fundamental length
			uom = createScalarUOM(UnitType.LENGTH, Unit.METRE, DomainLocalizer.instance().getUnitString("m.name"),
					DomainLocalizer.instance().getUnitString("m.symbol"),
					DomainLocalizer.instance().getUnitString("m.desc"));
			break;

		case DIOPTER:
			// per metre
			uom = createQuotientUOM(UnitType.RECIPROCAL_LENGTH, Unit.DIOPTER,
					DomainLocalizer.instance().getUnitString("diopter.name"),
					DomainLocalizer.instance().getUnitString("diopter.symbol"),
					DomainLocalizer.instance().getUnitString("diopter.desc"), getOne(), getUOM(Unit.METRE));
			break;

		case KILOGRAM:
			// fundamental mass
			uom = createScalarUOM(UnitType.MASS, Unit.KILOGRAM, DomainLocalizer.instance().getUnitString("kg.name"),
					DomainLocalizer.instance().getUnitString("kg.symbol"),
					DomainLocalizer.instance().getUnitString("kg.desc"));
			break;

		case TONNE:
			// mass
			uom = createScalarUOM(UnitType.MASS, Unit.TONNE, DomainLocalizer.instance().getUnitString("tonne.name"),
					DomainLocalizer.instance().getUnitString("tonne.symbol"),
					DomainLocalizer.instance().getUnitString("tonne.desc"));
			uom.setConversion(Prefix.KILO.getFactor(), getUOM(Unit.KILOGRAM));
			break;

		case KELVIN:
			// fundamental temperature
			uom = createScalarUOM(UnitType.TEMPERATURE, Unit.KELVIN,
					DomainLocalizer.instance().getUnitString("kelvin.name"),
					DomainLocalizer.instance().getUnitString("kelvin.symbol"),
					DomainLocalizer.instance().getUnitString("kelvin.desc"));
			break;

		case AMPERE:
			// electric current
			uom = createScalarUOM(UnitType.ELECTRIC_CURRENT, Unit.AMPERE,
					DomainLocalizer.instance().getUnitString("amp.name"),
					DomainLocalizer.instance().getUnitString("amp.symbol"),
					DomainLocalizer.instance().getUnitString("amp.desc"));
			break;

		case CANDELA:
			// luminosity
			uom = createScalarUOM(UnitType.LUMINOSITY, Unit.CANDELA,
					DomainLocalizer.instance().getUnitString("cd.name"),
					DomainLocalizer.instance().getUnitString("cd.symbol"),
					DomainLocalizer.instance().getUnitString("cd.desc"));
			break;

		case MOLARITY:
			// molar concentration
			uom = createQuotientUOM(UnitType.MOLAR_CONCENTRATION, Unit.MOLARITY,
					DomainLocalizer.instance().getUnitString("molarity.name"),
					DomainLocalizer.instance().getUnitString("molarity.symbol"),
					DomainLocalizer.instance().getUnitString("molarity.desc"), getUOM(Unit.MOLE), getUOM(Unit.LITRE));
			break;

		case PH:
			// molar concentration
			uom = createScalarUOM(UnitType.MOLAR_CONCENTRATION, Unit.PH,
					DomainLocalizer.instance().getUnitString("ph.name"),
					DomainLocalizer.instance().getUnitString("ph.symbol"),
					DomainLocalizer.instance().getUnitString("ph.desc"));
			break;

		case GRAM: // gram
			uom = createScalarUOM(UnitType.MASS, Unit.GRAM, DomainLocalizer.instance().getUnitString("gram.name"),
					DomainLocalizer.instance().getUnitString("gram.symbol"),
					DomainLocalizer.instance().getUnitString("gram.desc"));
			uom.setConversion(Prefix.MILLI.getFactor(), getUOM(Unit.KILOGRAM));
			break;

		case CARAT:
			// carat
			uom = createScalarUOM(UnitType.MASS, Unit.CARAT, DomainLocalizer.instance().getUnitString("carat.name"),
					DomainLocalizer.instance().getUnitString("carat.symbol"),
					DomainLocalizer.instance().getUnitString("carat.desc"));
			uom.setConversion(0.2, getUOM(Unit.GRAM));
			break;

		case SQUARE_METRE:
			// square metre
			uom = createPowerUOM(UnitType.AREA, Unit.SQUARE_METRE, DomainLocalizer.instance().getUnitString("m2.name"),
					DomainLocalizer.instance().getUnitString("m2.symbol"),
					DomainLocalizer.instance().getUnitString("m2.desc"), getUOM(Unit.METRE), 2);
			break;

		case HECTARE:
			// hectare
			uom = createScalarUOM(UnitType.AREA, Unit.HECTARE, DomainLocalizer.instance().getUnitString("hectare.name"),
					DomainLocalizer.instance().getUnitString("hectare.symbol"),
					DomainLocalizer.instance().getUnitString("hectare.desc"));
			uom.setConversion(10000d, getUOM(Unit.SQUARE_METRE));
			break;

		case METRE_PER_SEC:
			// velocity
			uom = createQuotientUOM(UnitType.VELOCITY, Unit.METRE_PER_SEC,
					DomainLocalizer.instance().getUnitString("mps.name"),
					DomainLocalizer.instance().getUnitString("mps.symbol"),
					DomainLocalizer.instance().getUnitString("mps.desc"), getUOM(Unit.METRE), getSecond());
			break;

		case METRE_PER_SEC_SQUARED:
			// acceleration
			uom = createQuotientUOM(UnitType.ACCELERATION, Unit.METRE_PER_SEC_SQUARED,
					DomainLocalizer.instance().getUnitString("mps2.name"),
					DomainLocalizer.instance().getUnitString("mps2.symbol"),
					DomainLocalizer.instance().getUnitString("mps2.desc"), getUOM(Unit.METRE),
					getUOM(Unit.SQUARE_SECOND));
			break;

		case CUBIC_METRE:
			// cubic metre
			uom = createPowerUOM(UnitType.VOLUME, Unit.CUBIC_METRE, DomainLocalizer.instance().getUnitString("m3.name"),
					DomainLocalizer.instance().getUnitString("m3.symbol"),
					DomainLocalizer.instance().getUnitString("m3.desc"), getUOM(Unit.METRE), 3);
			break;

		case LITRE:
			// litre
			uom = createScalarUOM(UnitType.VOLUME, Unit.LITRE, DomainLocalizer.instance().getUnitString("litre.name"),
					DomainLocalizer.instance().getUnitString("litre.symbol"),
					DomainLocalizer.instance().getUnitString("litre.desc"));
			uom.setConversion(Prefix.MILLI.getFactor(), getUOM(Unit.CUBIC_METRE));
			break;

		case CUBIC_METRE_PER_SEC:
			// flow (volume)
			uom = createQuotientUOM(UnitType.VOLUMETRIC_FLOW, Unit.CUBIC_METRE_PER_SEC,
					DomainLocalizer.instance().getUnitString("m3PerSec.name"),
					DomainLocalizer.instance().getUnitString("m3PerSec.symbol"),
					DomainLocalizer.instance().getUnitString("m3PerSec.desc"), getUOM(Unit.CUBIC_METRE), getSecond());
			break;

		case KILOGRAM_PER_SEC:
			// flow (mass)
			uom = createQuotientUOM(UnitType.MASS_FLOW, Unit.KILOGRAM_PER_SEC,
					DomainLocalizer.instance().getUnitString("kgPerSec.name"),
					DomainLocalizer.instance().getUnitString("kgPerSec.symbol"),
					DomainLocalizer.instance().getUnitString("kgPerSec.desc"), getUOM(Unit.KILOGRAM), getSecond());
			break;

		case KILOGRAM_PER_CU_METRE:
			// kg/m^3
			uom = createQuotientUOM(UnitType.DENSITY, Unit.KILOGRAM_PER_CU_METRE,
					DomainLocalizer.instance().getUnitString("kg_m3.name"),
					DomainLocalizer.instance().getUnitString("kg_m3.symbol"),
					DomainLocalizer.instance().getUnitString("kg_m3.desc"), getUOM(Unit.KILOGRAM),
					getUOM(Unit.CUBIC_METRE));
			break;

		case PASCAL_SECOND:
			// dynamic viscosity
			uom = createProductUOM(UnitType.DYNAMIC_VISCOSITY, Unit.PASCAL_SECOND,
					DomainLocalizer.instance().getUnitString("pascal_sec.name"),
					DomainLocalizer.instance().getUnitString("pascal_sec.symbol"),
					DomainLocalizer.instance().getUnitString("pascal_sec.desc"), getUOM(Unit.PASCAL), getSecond());
			break;

		case SQUARE_METRE_PER_SEC:
			// kinematic viscosity
			uom = createQuotientUOM(UnitType.KINEMATIC_VISCOSITY, Unit.SQUARE_METRE_PER_SEC,
					DomainLocalizer.instance().getUnitString("m2PerSec.name"),
					DomainLocalizer.instance().getUnitString("m2PerSec.symbol"),
					DomainLocalizer.instance().getUnitString("m2PerSec.desc"), getUOM(Unit.SQUARE_METRE), getSecond());
			break;

		case CALORIE:
			// thermodynamic calorie
			uom = createScalarUOM(UnitType.ENERGY, Unit.CALORIE,
					DomainLocalizer.instance().getUnitString("calorie.name"),
					DomainLocalizer.instance().getUnitString("calorie.symbol"),
					DomainLocalizer.instance().getUnitString("calorie.desc"));
			uom.setConversion(4.184, getUOM(Unit.JOULE));
			break;

		case NEWTON:
			// force F = m�A (newton)
			uom = createProductUOM(UnitType.FORCE, Unit.NEWTON, DomainLocalizer.instance().getUnitString("newton.name"),
					DomainLocalizer.instance().getUnitString("newton.symbol"),
					DomainLocalizer.instance().getUnitString("newton.desc"), getUOM(Unit.KILOGRAM),
					getUOM(Unit.METRE_PER_SEC_SQUARED));
			break;

		case NEWTON_METRE:
			// newton-metre
			uom = createProductUOM(UnitType.ENERGY, Unit.NEWTON_METRE,
					DomainLocalizer.instance().getUnitString("n_m.name"),
					DomainLocalizer.instance().getUnitString("n_m.symbol"),
					DomainLocalizer.instance().getUnitString("n_m.desc"), getUOM(Unit.NEWTON), getUOM(Unit.METRE));
			break;

		case JOULE:
			// energy (joule)
			uom = createProductUOM(UnitType.ENERGY, Unit.JOULE, DomainLocalizer.instance().getUnitString("joule.name"),
					DomainLocalizer.instance().getUnitString("joule.symbol"),
					DomainLocalizer.instance().getUnitString("joule.desc"), getUOM(Unit.NEWTON), getUOM(Unit.METRE));
			break;

		case ELECTRON_VOLT:
			// ev
			Quantity e = getQuantity(Constant.ELEMENTARY_CHARGE);
			uom = createProductUOM(UnitType.ENERGY, Unit.ELECTRON_VOLT,
					DomainLocalizer.instance().getUnitString("ev.name"),
					DomainLocalizer.instance().getUnitString("ev.symbol"),
					DomainLocalizer.instance().getUnitString("ev.desc"), e.getUOM(), getUOM(Unit.VOLT));
			uom.setScalingFactor(e.getAmount());
			break;

		case WATT_HOUR:
			// watt-hour
			uom = createProductUOM(UnitType.ENERGY, Unit.WATT_HOUR, DomainLocalizer.instance().getUnitString("wh.name"),
					DomainLocalizer.instance().getUnitString("wh.symbol"),
					DomainLocalizer.instance().getUnitString("wh.desc"), getUOM(Unit.WATT), getHour());
			break;

		case WATT:
			// power (watt)
			uom = createQuotientUOM(UnitType.POWER, Unit.WATT, DomainLocalizer.instance().getUnitString("watt.name"),
					DomainLocalizer.instance().getUnitString("watt.symbol"),
					DomainLocalizer.instance().getUnitString("watt.desc"), getUOM(Unit.JOULE), getSecond());
			break;

		case HERTZ:
			// frequency (hertz)
			uom = createQuotientUOM(UnitType.FREQUENCY, Unit.HERTZ,
					DomainLocalizer.instance().getUnitString("hertz.name"),
					DomainLocalizer.instance().getUnitString("hertz.symbol"),
					DomainLocalizer.instance().getUnitString("hertz.desc"), getOne(), getSecond());
			break;

		case RAD_PER_SEC:
			// angular frequency
			uom = createQuotientUOM(UnitType.FREQUENCY, Unit.RAD_PER_SEC,
					DomainLocalizer.instance().getUnitString("radpers.name"),
					DomainLocalizer.instance().getUnitString("radpers.symbol"),
					DomainLocalizer.instance().getUnitString("radpers.desc"), getUOM(Unit.RADIAN), getSecond());
			uom.setConversion(1.0 / (2.0d * Math.PI), getUOM(Unit.HERTZ));
			break;

		case PASCAL:
			// pressure
			uom = createQuotientUOM(UnitType.PRESSURE, Unit.PASCAL,
					DomainLocalizer.instance().getUnitString("pascal.name"),
					DomainLocalizer.instance().getUnitString("pascal.symbol"),
					DomainLocalizer.instance().getUnitString("pascal.desc"), getUOM(Unit.NEWTON),
					getUOM(Unit.SQUARE_METRE));
			break;

		case ATMOSPHERE:
			// pressure
			uom = createScalarUOM(UnitType.PRESSURE, Unit.ATMOSPHERE,
					DomainLocalizer.instance().getUnitString("atm.name"),
					DomainLocalizer.instance().getUnitString("atm.symbol"),
					DomainLocalizer.instance().getUnitString("atm.desc"));
			uom.setConversion(101325d, getUOM(Unit.PASCAL));
			break;

		case BAR:
			// pressure
			uom = createScalarUOM(UnitType.PRESSURE, Unit.BAR, DomainLocalizer.instance().getUnitString("bar.name"),
					DomainLocalizer.instance().getUnitString("bar.symbol"),
					DomainLocalizer.instance().getUnitString("bar.desc"));
			uom.setConversion(1.0, getUOM(Unit.PASCAL), 1.0E+05);
			break;

		case COULOMB:
			// charge (coulomb)
			uom = createProductUOM(UnitType.ELECTRIC_CHARGE, Unit.COULOMB,
					DomainLocalizer.instance().getUnitString("coulomb.name"),
					DomainLocalizer.instance().getUnitString("coulomb.symbol"),
					DomainLocalizer.instance().getUnitString("coulomb.desc"), getUOM(Unit.AMPERE), getSecond());
			break;

		case VOLT:
			// voltage (volt)
			uom = createQuotientUOM(UnitType.ELECTROMOTIVE_FORCE, Unit.VOLT,
					DomainLocalizer.instance().getUnitString("volt.name"),
					DomainLocalizer.instance().getUnitString("volt.symbol"),
					DomainLocalizer.instance().getUnitString("volt.desc"), getUOM(Unit.WATT), getUOM(Unit.AMPERE));
			break;

		case OHM:
			// resistance (ohm)
			uom = createQuotientUOM(UnitType.ELECTRIC_RESISTANCE, Unit.OHM,
					DomainLocalizer.instance().getUnitString("ohm.name"),
					DomainLocalizer.instance().getUnitString("ohm.symbol"),
					DomainLocalizer.instance().getUnitString("ohm.desc"), getUOM(Unit.VOLT), getUOM(Unit.AMPERE));
			break;

		case FARAD:
			// capacitance (farad)
			uom = createQuotientUOM(UnitType.ELECTRIC_CAPACITANCE, Unit.FARAD,
					DomainLocalizer.instance().getUnitString("farad.name"),
					DomainLocalizer.instance().getUnitString("farad.symbol"),
					DomainLocalizer.instance().getUnitString("farad.desc"), getUOM(Unit.COULOMB), getUOM(Unit.VOLT));
			break;

		case FARAD_PER_METRE:
			// electric permittivity (farad/metre)
			uom = createQuotientUOM(UnitType.ELECTRIC_PERMITTIVITY, Unit.FARAD_PER_METRE,
					DomainLocalizer.instance().getUnitString("fperm.name"),
					DomainLocalizer.instance().getUnitString("fperm.symbol"),
					DomainLocalizer.instance().getUnitString("fperm.desc"), getUOM(Unit.FARAD), getUOM(Unit.METRE));
			break;

		case AMPERE_PER_METRE:
			// electric field strength(ampere/metre)
			uom = createQuotientUOM(UnitType.ELECTRIC_FIELD_STRENGTH, Unit.AMPERE_PER_METRE,
					DomainLocalizer.instance().getUnitString("aperm.name"),
					DomainLocalizer.instance().getUnitString("aperm.symbol"),
					DomainLocalizer.instance().getUnitString("aperm.desc"), getUOM(Unit.AMPERE), getUOM(Unit.METRE));
			break;

		case WEBER:
			// magnetic flux (weber)
			uom = createProductUOM(UnitType.MAGNETIC_FLUX, Unit.WEBER,
					DomainLocalizer.instance().getUnitString("weber.name"),
					DomainLocalizer.instance().getUnitString("weber.symbol"),
					DomainLocalizer.instance().getUnitString("weber.desc"), getUOM(Unit.VOLT), getSecond());
			break;

		case TESLA:
			// magnetic flux density (tesla)
			uom = createQuotientUOM(UnitType.MAGNETIC_FLUX_DENSITY, Unit.TESLA,
					DomainLocalizer.instance().getUnitString("tesla.name"),
					DomainLocalizer.instance().getUnitString("tesla.symbol"),
					DomainLocalizer.instance().getUnitString("tesla.desc"), getUOM(Unit.WEBER),
					getUOM(Unit.SQUARE_METRE));
			break;

		case HENRY:
			// inductance (henry)
			uom = createQuotientUOM(UnitType.ELECTRIC_INDUCTANCE, Unit.HENRY,
					DomainLocalizer.instance().getUnitString("henry.name"),
					DomainLocalizer.instance().getUnitString("henry.symbol"),
					DomainLocalizer.instance().getUnitString("henry.desc"), getUOM(Unit.WEBER), getUOM(Unit.AMPERE));
			break;

		case SIEMENS:
			// electrical conductance (siemens)
			uom = createQuotientUOM(UnitType.ELECTRIC_CONDUCTANCE, Unit.SIEMENS,
					DomainLocalizer.instance().getUnitString("siemens.name"),
					DomainLocalizer.instance().getUnitString("siemens.symbol"),
					DomainLocalizer.instance().getUnitString("siemens.desc"), getUOM(Unit.AMPERE), getUOM(Unit.VOLT));
			break;

		case CELSIUS:
			// �C = �K - 273.15
			uom = createScalarUOM(UnitType.TEMPERATURE, Unit.CELSIUS,
					DomainLocalizer.instance().getUnitString("celsius.name"),
					DomainLocalizer.instance().getUnitString("celsius.symbol"),
					DomainLocalizer.instance().getUnitString("celsius.desc"));
			uom.setConversion(1.0, getUOM(Unit.KELVIN), 273.15);
			break;

		case LUMEN:
			// luminous flux (lumen)
			uom = createProductUOM(UnitType.LUMINOUS_FLUX, Unit.LUMEN,
					DomainLocalizer.instance().getUnitString("lumen.name"),
					DomainLocalizer.instance().getUnitString("lumen.symbol"),
					DomainLocalizer.instance().getUnitString("lumen.desc"), getUOM(Unit.CANDELA),
					getUOM(Unit.STERADIAN));
			break;

		case LUX:
			// illuminance (lux)
			uom = createQuotientUOM(UnitType.ILLUMINANCE, Unit.LUX,
					DomainLocalizer.instance().getUnitString("lux.name"),
					DomainLocalizer.instance().getUnitString("lux.symbol"),
					DomainLocalizer.instance().getUnitString("lux.desc"), getUOM(Unit.LUMEN),
					getUOM(Unit.SQUARE_METRE));
			break;

		case BECQUEREL:
			// radioactivity (becquerel). Same base symbol as Hertz
			uom = createScalarUOM(UnitType.RADIOACTIVITY, Unit.BECQUEREL,
					DomainLocalizer.instance().getUnitString("becquerel.name"),
					DomainLocalizer.instance().getUnitString("becquerel.symbol"),
					DomainLocalizer.instance().getUnitString("becquerel.desc"));
			break;

		case GRAY:
			// gray (Gy)
			uom = createQuotientUOM(UnitType.RADIATION_DOSE_ABSORBED, Unit.GRAY,
					DomainLocalizer.instance().getUnitString("gray.name"),
					DomainLocalizer.instance().getUnitString("gray.symbol"),
					DomainLocalizer.instance().getUnitString("gray.desc"), getUOM(Unit.JOULE), getUOM(Unit.KILOGRAM));
			break;

		case SIEVERT:
			// sievert (Sv)
			uom = createQuotientUOM(UnitType.RADIATION_DOSE_EFFECTIVE, Unit.SIEVERT,
					DomainLocalizer.instance().getUnitString("sievert.name"),
					DomainLocalizer.instance().getUnitString("sievert.symbol"),
					DomainLocalizer.instance().getUnitString("sievert.desc"), getUOM(Unit.JOULE),
					getUOM(Unit.KILOGRAM));
			break;

		case SIEVERTS_PER_HOUR:
			uom = createQuotientUOM(UnitType.RADIATION_DOSE_RATE, Unit.SIEVERTS_PER_HOUR,
					DomainLocalizer.instance().getUnitString("sph.name"),
					DomainLocalizer.instance().getUnitString("sph.symbol"),
					DomainLocalizer.instance().getUnitString("sph.desc"), getUOM(Unit.SIEVERT), getHour());
			break;

		case KATAL:
			// katal (kat)
			uom = createQuotientUOM(UnitType.CATALYTIC_ACTIVITY, Unit.KATAL,
					DomainLocalizer.instance().getUnitString("katal.name"),
					DomainLocalizer.instance().getUnitString("katal.symbol"),
					DomainLocalizer.instance().getUnitString("katal.desc"), getUOM(Unit.MOLE), getSecond());
			break;

		case UNIT:
			// Unit (U)
			uom = createScalarUOM(UnitType.CATALYTIC_ACTIVITY, Unit.UNIT,
					DomainLocalizer.instance().getUnitString("unit.name"),
					DomainLocalizer.instance().getUnitString("unit.symbol"),
					DomainLocalizer.instance().getUnitString("unit.desc"));
			uom.setConversion(1.0E-06 / 60d, getUOM(Unit.KATAL));
			break;

		case INTERNATIONAL_UNIT:
			uom = createScalarUOM(UnitType.SUBSTANCE_AMOUNT, Unit.INTERNATIONAL_UNIT,
					DomainLocalizer.instance().getUnitString("iu.name"),
					DomainLocalizer.instance().getUnitString("iu.symbol"),
					DomainLocalizer.instance().getUnitString("iu.desc"));
			break;

		case ANGSTROM:
			// length
			uom = createScalarUOM(UnitType.LENGTH, Unit.ANGSTROM,
					DomainLocalizer.instance().getUnitString("angstrom.name"),
					DomainLocalizer.instance().getUnitString("angstrom.symbol"),
					DomainLocalizer.instance().getUnitString("angstrom.desc"));
			uom.setConversion(0.1, getUOM(Prefix.NANO, getUOM(Unit.METRE)));
			break;

		case BIT:
			// computer bit
			uom = createScalarUOM(UnitType.COMPUTER_SCIENCE, Unit.BIT,
					DomainLocalizer.instance().getUnitString("bit.name"),
					DomainLocalizer.instance().getUnitString("bit.symbol"),
					DomainLocalizer.instance().getUnitString("bit.desc"));
			break;

		case BYTE:
			// computer byte
			uom = createScalarUOM(UnitType.COMPUTER_SCIENCE, Unit.BYTE,
					DomainLocalizer.instance().getUnitString("byte.name"),
					DomainLocalizer.instance().getUnitString("byte.symbol"),
					DomainLocalizer.instance().getUnitString("byte.desc"));
			uom.setConversion(8d, getUOM(Unit.BIT));
			break;

		case WATTS_PER_SQ_METRE:
			uom = createQuotientUOM(UnitType.IRRADIANCE, Unit.WATTS_PER_SQ_METRE,
					DomainLocalizer.instance().getUnitString("wsm.name"),
					DomainLocalizer.instance().getUnitString("wsm.symbol"),
					DomainLocalizer.instance().getUnitString("wsm.desc"), getUOM(Unit.WATT), getUOM(Unit.SQUARE_METRE));
			break;

		case PARSEC:
			uom = createScalarUOM(UnitType.LENGTH, Unit.PARSEC, DomainLocalizer.instance().getUnitString("parsec.name"),
					DomainLocalizer.instance().getUnitString("parsec.symbol"),
					DomainLocalizer.instance().getUnitString("parsec.desc"));
			uom.setConversion(3.08567758149137E+16, getUOM(Unit.METRE));
			break;

		case ASTRONOMICAL_UNIT:
			uom = createScalarUOM(UnitType.LENGTH, Unit.ASTRONOMICAL_UNIT,
					DomainLocalizer.instance().getUnitString("au.name"),
					DomainLocalizer.instance().getUnitString("au.symbol"),
					DomainLocalizer.instance().getUnitString("au.desc"));
			uom.setConversion(1.49597870700E+11, getUOM(Unit.METRE));
			break;

		case NORMALITY:
			// equivalent concentration
			uom = createScalarUOM(UnitType.MOLAR_CONCENTRATION, Unit.NORMALITY,
					DomainLocalizer.instance().getUnitString("normal.name"),
					DomainLocalizer.instance().getUnitString("normal.symbol"),
					DomainLocalizer.instance().getUnitString("normal.desc"));
			break;

		default:
			break;
		}

		return uom;
	}

	private UnitOfMeasure createCustomaryUnit(Unit unit) throws Exception {
		UnitOfMeasure uom = null;

		switch (unit) {

		case RANKINE:
			// Rankine (base)
			uom = createScalarUOM(UnitType.TEMPERATURE, Unit.RANKINE,
					DomainLocalizer.instance().getUnitString("rankine.name"),
					DomainLocalizer.instance().getUnitString("rankine.symbol"),
					DomainLocalizer.instance().getUnitString("rankine.desc"));

			// create bridge to SI
			uom.setBridgeConversion(5d / 9d, getUOM(Unit.KELVIN), 0.0d);
			break;

		case FAHRENHEIT:
			// Fahrenheit
			uom = createScalarUOM(UnitType.TEMPERATURE, Unit.FAHRENHEIT,
					DomainLocalizer.instance().getUnitString("fahrenheit.name"),
					DomainLocalizer.instance().getUnitString("fahrenheit.symbol"),
					DomainLocalizer.instance().getUnitString("fahrenheit.desc"));
			uom.setConversion(1.0, getUOM(Unit.RANKINE), 459.67);
			break;

		case POUND_MASS:
			// lb mass (base)
			uom = createScalarUOM(UnitType.MASS, Unit.POUND_MASS, DomainLocalizer.instance().getUnitString("lbm.name"),
					DomainLocalizer.instance().getUnitString("lbm.symbol"),
					DomainLocalizer.instance().getUnitString("lbm.desc"));

			// create bridge to SI
			uom.setBridgeConversion(0.45359237, getUOM(Unit.KILOGRAM), 0.0d);
			break;

		case OUNCE:
			// ounce
			uom = createScalarUOM(UnitType.MASS, Unit.OUNCE, DomainLocalizer.instance().getUnitString("ounce.name"),
					DomainLocalizer.instance().getUnitString("ounce.symbol"),
					DomainLocalizer.instance().getUnitString("ounce.desc"));
			uom.setConversion(0.0625, getUOM(Unit.POUND_MASS));
			break;

		case TROY_OUNCE:
			// troy ounce
			uom = createScalarUOM(UnitType.MASS, Unit.TROY_OUNCE,
					DomainLocalizer.instance().getUnitString("troy_oz.name"),
					DomainLocalizer.instance().getUnitString("troy_oz.symbol"),
					DomainLocalizer.instance().getUnitString("troy_oz.desc"));
			uom.setConversion(0.06857142857, getUOM(Unit.POUND_MASS));
			break;

		case SLUG:
			// slug
			uom = createScalarUOM(UnitType.MASS, Unit.SLUG, DomainLocalizer.instance().getUnitString("slug.name"),
					DomainLocalizer.instance().getUnitString("slug.symbol"),
					DomainLocalizer.instance().getUnitString("slug.desc"));
			Quantity g = getQuantity(Constant.GRAVITY).convert(getUOM(Unit.FEET_PER_SEC_SQUARED));
			uom.setConversion(g.getAmount(), getUOM(Unit.POUND_MASS));
			break;

		case FOOT:
			// foot (foot is base conversion unit)
			uom = createScalarUOM(UnitType.LENGTH, Unit.FOOT, DomainLocalizer.instance().getUnitString("foot.name"),
					DomainLocalizer.instance().getUnitString("foot.symbol"),
					DomainLocalizer.instance().getUnitString("foot.desc"));

			// bridge to SI
			uom.setBridgeConversion(0.3048, getUOM(Unit.METRE), 0);
			break;

		case INCH:
			// inch
			uom = createScalarUOM(UnitType.LENGTH, Unit.INCH, DomainLocalizer.instance().getUnitString("inch.name"),
					DomainLocalizer.instance().getUnitString("inch.symbol"),
					DomainLocalizer.instance().getUnitString("inch.desc"));
			uom.setConversion(1d / 12d, getUOM(Unit.FOOT));
			break;

		case MIL:
			// inch
			uom = createScalarUOM(UnitType.LENGTH, Unit.MIL, DomainLocalizer.instance().getUnitString("mil.name"),
					DomainLocalizer.instance().getUnitString("mil.symbol"),
					DomainLocalizer.instance().getUnitString("mil.desc"));
			uom.setConversion(Prefix.MILLI.getFactor(), getUOM(Unit.INCH));
			break;

		case POINT:
			// point
			uom = createScalarUOM(UnitType.LENGTH, Unit.POINT, DomainLocalizer.instance().getUnitString("point.name"),
					DomainLocalizer.instance().getUnitString("point.symbol"),
					DomainLocalizer.instance().getUnitString("point.desc"));
			uom.setConversion(1d / 72d, getUOM(Unit.INCH));
			break;

		case YARD:
			// yard
			uom = createScalarUOM(UnitType.LENGTH, Unit.YARD, DomainLocalizer.instance().getUnitString("yard.name"),
					DomainLocalizer.instance().getUnitString("yard.symbol"),
					DomainLocalizer.instance().getUnitString("yard.desc"));
			uom.setConversion(3d, getUOM(Unit.FOOT));
			break;

		case MILE:
			// mile
			uom = createScalarUOM(UnitType.LENGTH, Unit.MILE, DomainLocalizer.instance().getUnitString("mile.name"),
					DomainLocalizer.instance().getUnitString("mile.symbol"),
					DomainLocalizer.instance().getUnitString("mile.desc"));
			uom.setConversion(5280d, getUOM(Unit.FOOT));
			break;

		case NAUTICAL_MILE:
			// nautical mile
			uom = createScalarUOM(UnitType.LENGTH, Unit.NAUTICAL_MILE,
					DomainLocalizer.instance().getUnitString("NM.name"),
					DomainLocalizer.instance().getUnitString("NM.symbol"),
					DomainLocalizer.instance().getUnitString("NM.desc"));
			uom.setConversion(6080d, getUOM(Unit.FOOT));
			break;

		case FATHOM:
			// fathom
			uom = createScalarUOM(UnitType.LENGTH, Unit.FATHOM, DomainLocalizer.instance().getUnitString("fth.name"),
					DomainLocalizer.instance().getUnitString("fth.symbol"),
					DomainLocalizer.instance().getUnitString("fth.desc"));
			uom.setConversion(6d, getUOM(Unit.FOOT));

			break;

		case PSI:
			// psi
			uom = createQuotientUOM(UnitType.PRESSURE, Unit.PSI, DomainLocalizer.instance().getUnitString("psi.name"),
					DomainLocalizer.instance().getUnitString("psi.symbol"),
					DomainLocalizer.instance().getUnitString("psi.desc"), getUOM(Unit.POUND_FORCE),
					getUOM(Unit.SQUARE_INCH));
			break;

		case IN_HG:
			// inches of Mercury
			uom = createScalarUOM(UnitType.PRESSURE, Unit.IN_HG, DomainLocalizer.instance().getUnitString("inhg.name"),
					DomainLocalizer.instance().getUnitString("inhg.symbol"),
					DomainLocalizer.instance().getUnitString("inhg.desc"));
			UnitOfMeasure u1 = createProductUOM(getUOM(Unit.FOOT), getUOM(Unit.SQUARE_SECOND));
			UnitOfMeasure u2 = createQuotientUOM(getUOM(Unit.POUND_MASS), u1);
			uom.setConversion(2275.520677, u2);
			break;

		case SQUARE_INCH:
			// square inch
			uom = createPowerUOM(UnitType.AREA, Unit.SQUARE_INCH, DomainLocalizer.instance().getUnitString("in2.name"),
					DomainLocalizer.instance().getUnitString("in2.symbol"),
					DomainLocalizer.instance().getUnitString("in2.desc"), getUOM(Unit.INCH), 2);
			uom.setConversion(1d / 144d, getUOM(Unit.SQUARE_FOOT));
			break;

		case SQUARE_FOOT:
			// square foot
			uom = createPowerUOM(UnitType.AREA, Unit.SQUARE_FOOT, DomainLocalizer.instance().getUnitString("ft2.name"),
					DomainLocalizer.instance().getUnitString("ft2.symbol"),
					DomainLocalizer.instance().getUnitString("ft2.desc"), getUOM(Unit.FOOT), 2);
			break;

		case SQUARE_YARD:
			// square yard
			uom = createPowerUOM(UnitType.AREA, Unit.SQUARE_YARD, DomainLocalizer.instance().getUnitString("yd2.name"),
					DomainLocalizer.instance().getUnitString("yd2.symbol"),
					DomainLocalizer.instance().getUnitString("yd2.desc"), getUOM(Unit.YARD), 2);
			break;

		case ACRE:
			// acre
			uom = createScalarUOM(UnitType.AREA, Unit.ACRE, DomainLocalizer.instance().getUnitString("acre.name"),
					DomainLocalizer.instance().getUnitString("acre.symbol"),
					DomainLocalizer.instance().getUnitString("acre.desc"));
			uom.setConversion(43560d, getUOM(Unit.SQUARE_FOOT));
			break;

		case CUBIC_INCH:
			// cubic inch
			uom = createPowerUOM(UnitType.VOLUME, Unit.CUBIC_INCH, DomainLocalizer.instance().getUnitString("in3.name"),
					DomainLocalizer.instance().getUnitString("in3.symbol"),
					DomainLocalizer.instance().getUnitString("in3.desc"), getUOM(Unit.INCH), 3);
			uom.setConversion(1d / 1728d, getUOM(Unit.CUBIC_FOOT));
			break;

		case CUBIC_FOOT:
			// cubic feet
			uom = createPowerUOM(UnitType.VOLUME, Unit.CUBIC_FOOT, DomainLocalizer.instance().getUnitString("ft3.name"),
					DomainLocalizer.instance().getUnitString("ft3.symbol"),
					DomainLocalizer.instance().getUnitString("ft3.desc"), getUOM(Unit.FOOT), 3);
			break;

		case CUBIC_FEET_PER_SEC:
			// flow (volume)
			uom = createQuotientUOM(UnitType.VOLUMETRIC_FLOW, Unit.CUBIC_FEET_PER_SEC,
					DomainLocalizer.instance().getUnitString("ft3PerSec.name"),
					DomainLocalizer.instance().getUnitString("ft3PerSec.symbol"),
					DomainLocalizer.instance().getUnitString("ft3PerSec.desc"), getUOM(Unit.CUBIC_FOOT), getSecond());
			break;

		case CORD:
			// cord
			uom = createScalarUOM(UnitType.VOLUME, Unit.CORD, DomainLocalizer.instance().getUnitString("cord.name"),
					DomainLocalizer.instance().getUnitString("cord.symbol"),
					DomainLocalizer.instance().getUnitString("cord.desc"));
			uom.setConversion(128d, getUOM(Unit.CUBIC_FOOT));
			break;

		case CUBIC_YARD:
			// cubic yard
			uom = createPowerUOM(UnitType.VOLUME, Unit.CUBIC_YARD, DomainLocalizer.instance().getUnitString("yd3.name"),
					DomainLocalizer.instance().getUnitString("yd3.symbol"),
					DomainLocalizer.instance().getUnitString("yd3.desc"), getUOM(Unit.YARD), 3);
			break;

		case FEET_PER_SEC:
			// feet/sec
			uom = createQuotientUOM(UnitType.VELOCITY, Unit.FEET_PER_SEC,
					DomainLocalizer.instance().getUnitString("fps.name"),
					DomainLocalizer.instance().getUnitString("fps.symbol"),
					DomainLocalizer.instance().getUnitString("fps.desc"), getUOM(Unit.FOOT), getSecond());
			break;

		case KNOT:
			// knot
			uom = createScalarUOM(UnitType.VELOCITY, Unit.KNOT, DomainLocalizer.instance().getUnitString("knot.name"),
					DomainLocalizer.instance().getUnitString("knot.symbol"),
					DomainLocalizer.instance().getUnitString("knot.desc"));
			uom.setConversion(6080d / 3600d, getUOM(Unit.FEET_PER_SEC));
			break;

		case FEET_PER_SEC_SQUARED:
			// acceleration
			uom = createQuotientUOM(UnitType.ACCELERATION, Unit.FEET_PER_SEC_SQUARED,
					DomainLocalizer.instance().getUnitString("ftps2.name"),
					DomainLocalizer.instance().getUnitString("ftps2.symbol"),
					DomainLocalizer.instance().getUnitString("ftps2.desc"), getUOM(Unit.FOOT),
					getUOM(Unit.SQUARE_SECOND));
			break;

		case HP:
			// HP (mechanical)
			uom = createProductUOM(UnitType.POWER, Unit.HP, DomainLocalizer.instance().getUnitString("hp.name"),
					DomainLocalizer.instance().getUnitString("hp.symbol"),
					DomainLocalizer.instance().getUnitString("hp.desc"), getUOM(Unit.POUND_FORCE),
					getUOM(Unit.FEET_PER_SEC));
			uom.setScalingFactor(550d);
			break;

		case BTU:
			// BTU = 1055.056 Joules (778.169 ft-lbf)
			uom = createScalarUOM(UnitType.ENERGY, Unit.BTU, DomainLocalizer.instance().getUnitString("btu.name"),
					DomainLocalizer.instance().getUnitString("btu.symbol"),
					DomainLocalizer.instance().getUnitString("btu.desc"));
			uom.setConversion(778.1692622659652, getUOM(Unit.FOOT_POUND_FORCE));
			break;

		case FOOT_POUND_FORCE:
			// ft-lbf
			uom = createProductUOM(UnitType.ENERGY, Unit.FOOT_POUND_FORCE,
					DomainLocalizer.instance().getUnitString("ft_lbf.name"),
					DomainLocalizer.instance().getUnitString("ft_lbf.symbol"),
					DomainLocalizer.instance().getUnitString("ft_lbf.desc"), getUOM(Unit.FOOT),
					getUOM(Unit.POUND_FORCE));
			break;

		case POUND_FORCE:
			// force F = m�A (lbf)
			uom = createProductUOM(UnitType.FORCE, Unit.POUND_FORCE,
					DomainLocalizer.instance().getUnitString("lbf.name"),
					DomainLocalizer.instance().getUnitString("lbf.symbol"),
					DomainLocalizer.instance().getUnitString("lbf.desc"), getUOM(Unit.POUND_MASS),
					getUOM(Unit.FEET_PER_SEC_SQUARED));

			// factor is acceleration of gravity
			Quantity gravity = getQuantity(Constant.GRAVITY).convert(getUOM(Unit.FEET_PER_SEC_SQUARED));
			uom.setScalingFactor(gravity.getAmount());
			break;

		case GRAIN:
			// mass
			uom = createScalarUOM(UnitType.MASS, Unit.GRAIN, DomainLocalizer.instance().getUnitString("grain.name"),
					DomainLocalizer.instance().getUnitString("grain.symbol"),
					DomainLocalizer.instance().getUnitString("grain.desc"));
			uom.setConversion(1d / 7000d, getUOM(Unit.POUND_MASS));
			break;

		case MILES_PER_HOUR:
			// velocity
			uom = createScalarUOM(UnitType.VELOCITY, Unit.MILES_PER_HOUR,
					DomainLocalizer.instance().getUnitString("mph.name"),
					DomainLocalizer.instance().getUnitString("mph.symbol"),
					DomainLocalizer.instance().getUnitString("mph.desc"));
			uom.setConversion(5280d / 3600d, getUOM(Unit.FEET_PER_SEC));
			break;

		case REV_PER_MIN:
			// rpm
			uom = createQuotientUOM(UnitType.FREQUENCY, Unit.REV_PER_MIN,
					DomainLocalizer.instance().getUnitString("rpm.name"),
					DomainLocalizer.instance().getUnitString("rpm.symbol"),
					DomainLocalizer.instance().getUnitString("rpm.desc"), getOne(), getMinute());
			break;

		default:
			break;
		}

		return uom;
	}

	private UnitOfMeasure createUSUnit(Unit unit) throws Exception {
		UnitOfMeasure uom = null;

		switch (unit) {

		case US_GALLON:
			// gallon
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_GALLON,
					DomainLocalizer.instance().getUnitString("us_gallon.name"),
					DomainLocalizer.instance().getUnitString("us_gallon.symbol"),
					DomainLocalizer.instance().getUnitString("us_gallon.desc"));
			uom.setConversion(231d, getUOM(Unit.CUBIC_INCH));
			break;

		case US_BARREL:
			// barrel
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_BARREL,
					DomainLocalizer.instance().getUnitString("us_bbl.name"),
					DomainLocalizer.instance().getUnitString("us_bbl.symbol"),
					DomainLocalizer.instance().getUnitString("us_bbl.desc"));
			uom.setConversion(42d, getUOM(Unit.US_GALLON));
			break;

		case US_BUSHEL:
			// bushel
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_BUSHEL,
					DomainLocalizer.instance().getUnitString("us_bu.name"),
					DomainLocalizer.instance().getUnitString("us_bu.symbol"),
					DomainLocalizer.instance().getUnitString("us_bu.desc"));
			uom.setConversion(2150.42058, getUOM(Unit.CUBIC_INCH));
			break;

		case US_FLUID_OUNCE:
			// fluid ounce
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_FLUID_OUNCE,
					DomainLocalizer.instance().getUnitString("us_fl_oz.name"),
					DomainLocalizer.instance().getUnitString("us_fl_oz.symbol"),
					DomainLocalizer.instance().getUnitString("us_fl_oz.desc"));
			uom.setConversion(0.0078125, getUOM(Unit.US_GALLON));
			break;

		case US_CUP:
			// cup
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_CUP, DomainLocalizer.instance().getUnitString("us_cup.name"),
					DomainLocalizer.instance().getUnitString("us_cup.symbol"),
					DomainLocalizer.instance().getUnitString("us_cup.desc"));
			uom.setConversion(8d, getUOM(Unit.US_FLUID_OUNCE));
			break;

		case US_PINT:
			// pint
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_PINT,
					DomainLocalizer.instance().getUnitString("us_pint.name"),
					DomainLocalizer.instance().getUnitString("us_pint.symbol"),
					DomainLocalizer.instance().getUnitString("us_pint.desc"));
			uom.setConversion(16d, getUOM(Unit.US_FLUID_OUNCE));
			break;

		case US_QUART:
			// quart
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_QUART,
					DomainLocalizer.instance().getUnitString("us_quart.name"),
					DomainLocalizer.instance().getUnitString("us_quart.symbol"),
					DomainLocalizer.instance().getUnitString("us_quart.desc"));
			uom.setConversion(32d, getUOM(Unit.US_FLUID_OUNCE));
			break;

		case US_TABLESPOON:
			// tablespoon
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_TABLESPOON,
					DomainLocalizer.instance().getUnitString("us_tbsp.name"),
					DomainLocalizer.instance().getUnitString("us_tbsp.symbol"),
					DomainLocalizer.instance().getUnitString("us_tbsp.desc"));
			uom.setConversion(0.5, getUOM(Unit.US_FLUID_OUNCE));
			break;

		case US_TEASPOON:
			// teaspoon
			uom = createScalarUOM(UnitType.VOLUME, Unit.US_TEASPOON,
					DomainLocalizer.instance().getUnitString("us_tsp.name"),
					DomainLocalizer.instance().getUnitString("us_tsp.symbol"),
					DomainLocalizer.instance().getUnitString("us_tsp.desc"));
			uom.setConversion(1d / 6d, getUOM(Unit.US_FLUID_OUNCE));
			break;

		case US_TON:
			// ton
			uom = createScalarUOM(UnitType.MASS, Unit.US_TON, DomainLocalizer.instance().getUnitString("us_ton.name"),
					DomainLocalizer.instance().getUnitString("us_ton.symbol"),
					DomainLocalizer.instance().getUnitString("us_ton.desc"));
			uom.setConversion(2000d, getUOM(Unit.POUND_MASS));
			break;

		default:
			break;
		}

		return uom;
	}

	private UnitOfMeasure createBRUnit(Unit unit) throws Exception {

		UnitOfMeasure uom = null;

		switch (unit) {
		case BR_GALLON:
			// gallon
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_GALLON,
					DomainLocalizer.instance().getUnitString("br_gallon.name"),
					DomainLocalizer.instance().getUnitString("br_gallon.symbol"),
					DomainLocalizer.instance().getUnitString("br_gallon.desc"));
			uom.setConversion(277.4194327916215, getUOM(Unit.CUBIC_INCH));
			break;

		case BR_BUSHEL:
			// bushel
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_BUSHEL,
					DomainLocalizer.instance().getUnitString("br_bu.name"),
					DomainLocalizer.instance().getUnitString("br_bu.symbol"),
					DomainLocalizer.instance().getUnitString("br_bu.desc"));
			uom.setConversion(8d, getUOM(Unit.BR_GALLON));
			break;

		case BR_FLUID_OUNCE:
			// fluid ounce
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_FLUID_OUNCE,
					DomainLocalizer.instance().getUnitString("br_fl_oz.name"),
					DomainLocalizer.instance().getUnitString("br_fl_oz.symbol"),
					DomainLocalizer.instance().getUnitString("br_fl_oz.desc"));
			uom.setConversion(0.00625, getUOM(Unit.BR_GALLON));
			break;

		case BR_CUP:
			// cup
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_CUP, DomainLocalizer.instance().getUnitString("br_cup.name"),
					DomainLocalizer.instance().getUnitString("br_cup.symbol"),
					DomainLocalizer.instance().getUnitString("br_cup.desc"));
			uom.setConversion(8d, getUOM(Unit.BR_FLUID_OUNCE));
			break;

		case BR_PINT:
			// pint
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_PINT,
					DomainLocalizer.instance().getUnitString("br_pint.name"),
					DomainLocalizer.instance().getUnitString("br_pint.symbol"),
					DomainLocalizer.instance().getUnitString("br_pint.desc"));
			uom.setConversion(20d, getUOM(Unit.BR_FLUID_OUNCE));
			break;

		case BR_QUART:
			// quart
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_QUART,
					DomainLocalizer.instance().getUnitString("br_quart.name"),
					DomainLocalizer.instance().getUnitString("br_quart.symbol"),
					DomainLocalizer.instance().getUnitString("br_quart.desc"));
			uom.setConversion(40d, getUOM(Unit.BR_FLUID_OUNCE));
			break;

		case BR_TABLESPOON:
			// tablespoon
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_TABLESPOON,
					DomainLocalizer.instance().getUnitString("br_tbsp.name"),
					DomainLocalizer.instance().getUnitString("br_tbsp.symbol"),
					DomainLocalizer.instance().getUnitString("br_tbsp.desc"));
			uom.setConversion(0.625, getUOM(Unit.BR_FLUID_OUNCE));
			break;

		case BR_TEASPOON:
			// teaspoon
			uom = createScalarUOM(UnitType.VOLUME, Unit.BR_TEASPOON,
					DomainLocalizer.instance().getUnitString("br_tsp.name"),
					DomainLocalizer.instance().getUnitString("br_tsp.symbol"),
					DomainLocalizer.instance().getUnitString("br_tsp.desc"));
			uom.setConversion(5d / 24d, getUOM(Unit.BR_FLUID_OUNCE));
			break;

		case BR_TON:
			// ton
			uom = createScalarUOM(UnitType.MASS, Unit.BR_TON, DomainLocalizer.instance().getUnitString("br_ton.name"),
					DomainLocalizer.instance().getUnitString("br_ton.symbol"),
					DomainLocalizer.instance().getUnitString("br_ton.desc"));
			uom.setConversion(2240d, getUOM(Unit.POUND_MASS));
			break;

		default:
			break;
		}

		return uom;
	}

	private UnitOfMeasure createFinancialUnit(Unit unit) throws Exception {
		UnitOfMeasure uom = null;

		switch (unit) {

		case US_DOLLAR:
			uom = createScalarUOM(UnitType.CURRENCY, Unit.US_DOLLAR,
					DomainLocalizer.instance().getUnitString("us_dollar.name"),
					DomainLocalizer.instance().getUnitString("us_dollar.symbol"),
					DomainLocalizer.instance().getUnitString("us_dollar.desc"));
			break;

		case EURO:
			uom = createScalarUOM(UnitType.CURRENCY, Unit.EURO, DomainLocalizer.instance().getUnitString("euro.name"),
					DomainLocalizer.instance().getUnitString("euro.symbol"),
					DomainLocalizer.instance().getUnitString("euro.desc"));
			break;

		case YUAN:
			uom = createScalarUOM(UnitType.CURRENCY, Unit.YUAN, DomainLocalizer.instance().getUnitString("yuan.name"),
					DomainLocalizer.instance().getUnitString("yuan.symbol"),
					DomainLocalizer.instance().getUnitString("yuan.desc"));
			break;

		default:
			break;
		}

		return uom;
	}

	/**
	 * Get the unit of measure with this unique enumerated type
	 * 
	 * @param unit {@link Unit}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception Exception
	 */
	public UnitOfMeasure getUOM(Unit unit) throws Exception {
		UnitOfMeasure uom = cacheManager.getUOM(unit);

		if (uom == null) {
			uom = createUOM(unit);
		}
		return uom;
	}

	/**
	 * Get the fundamental unit of measure of time
	 * 
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception Exception
	 */
	public UnitOfMeasure getSecond() throws Exception {
		return getUOM(Unit.SECOND);
	}

	/**
	 * Get the unit of measure for a minute (60 seconds)
	 * 
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getMinute() throws Exception {
		return getUOM(Unit.MINUTE);
	}

	/**
	 * Get the unit of measure for an hour (60 minutes)
	 * 
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getHour() throws Exception {
		return getUOM(Unit.HOUR);
	}

	/**
	 * Get the unit of measure for one day (24 hours)
	 * 
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getDay() throws Exception {
		return getUOM(Unit.DAY);
	}

	/**
	 * Get the unit of measure for unity 'one'
	 * 
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getOne() throws Exception {
		return getUOM(Unit.ONE);
	}

	/**
	 * Get the unit of measure with this unique symbol
	 * 
	 * @param symbol Symbol
	 * @return {@link UnitOfMeasure}
	 */
	public UnitOfMeasure getUOM(String symbol) {
		return cacheManager.getUOM(symbol);
	}

	/**
	 * Get the unit of measure with this base symbol
	 * 
	 * @param symbol Base symbol
	 * @return {@link UnitOfMeasure}
	 */
	public UnitOfMeasure getBaseUOM(String symbol) {
		return cacheManager.getBaseUOM(symbol);
	}

	/**
	 * Remove all cached units of measure
	 */
	public void clearCache() {
		cacheManager.clearCache();
	}

	/**
	 * Get all units currently cached by this measurement system
	 * 
	 * @return List of {@link UnitOfMeasure}
	 */
	public List<UnitOfMeasure> getRegisteredUnits() {
		Collection<UnitOfMeasure> units = cacheManager.getCachedUnits();
		List<UnitOfMeasure> list = new ArrayList<>(units);

		Collections.sort(list, new Comparator<UnitOfMeasure>() {
			public int compare(UnitOfMeasure unit1, UnitOfMeasure unit2) {
				return unit1.getSymbol().compareTo(unit2.getSymbol());
			}
		});

		return list;
	}

	/**
	 * Get the units of measure cached by their symbol
	 * 
	 * @return Symbol cache
	 */
	public Map<String, UnitOfMeasure> getSymbolCache() {
		return cacheManager.getSymbolCache();
	}

	/**
	 * Get the units of measure cached by their base symbol
	 * 
	 * @return Base symbol cache
	 */
	public Map<String, UnitOfMeasure> getBaseSymbolCache() {
		return cacheManager.getBaseSymbolCache();
	}

	/**
	 * Get the units of measure cached by their {@link Unit} enumeration
	 * 
	 * @return Enumeration cache
	 */
	public Map<Unit, UnitOfMeasure> getEnumerationCache() {
		return cacheManager.getEnumerationCache();
	}

	/**
	 * Remove a unit from the cache
	 * 
	 * @param uom {@link UnitOfMeasure} to remove
	 * @throws Exception Exception
	 */
	public synchronized void unregisterUnit(UnitOfMeasure uom) throws Exception {
		if (uom == null) {
			return;
		}
		cacheManager.unregisterUnit(uom);
	}

	/**
	 * Cache this unit of measure
	 * 
	 * @param uom {@link UnitOfMeasure} to cache
	 * @throws Exception Exception
	 */
	public void registerUnit(UnitOfMeasure uom) throws Exception {
		cacheManager.registerUnit(uom);
	}

	private UnitOfMeasure createUOM(UnitType type, String name, String symbol, String description) throws Exception {

		if (symbol == null || symbol.length() == 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("symbol.cannot.be.null"));
		}

		if (type == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("unit.type.cannot.be.null"));
		}

		UnitOfMeasure uom = cacheManager.getUOM(symbol);

		if (uom == null) {
			// create a new one
			uom = new UnitOfMeasure(type, name, symbol, description);
			uom.setAbscissaUnit(uom);
		}
		return uom;
	}

	private UnitOfMeasure createScalarUOM(UnitType type, Unit id, String name, String symbol, String description)
			throws Exception {

		UnitOfMeasure uom = createUOM(type, name, symbol, description);
		uom.setEnumeration(id);
		registerUnit(uom);

		return uom;
	}

	/**
	 * Create a unit of measure that is not a power, product or quotient
	 * 
	 * @param type        {@link UnitType}
	 * @param name        Name of unit of measure
	 * @param symbol      Symbol (must be unique)
	 * @param description Description of unit of measure
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createScalarUOM(UnitType type, String name, String symbol, String description)
			throws Exception {
		return createScalarUOM(type, null, name, symbol, description);
	}

	/**
	 * Create a unit of measure that is a unit divided by another unit
	 * 
	 * @param type        {@link UnitType}
	 * @param id          {@link Unit}
	 * @param name        Name of unit of measure
	 * @param symbol      Symbol (must be unique)
	 * @param description Description of unit of measure
	 * @param dividend    {@link UnitOfMeasure}
	 * @param divisor     {@link UnitOfMeasure}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createQuotientUOM(UnitType type, Unit id, String name, String symbol, String description,
			UnitOfMeasure dividend, UnitOfMeasure divisor) throws Exception {

		UnitOfMeasure uom = createUOM(type, name, symbol, description);
		uom.setQuotientUnits(dividend, divisor);
		uom.setEnumeration(id);
		registerUnit(uom);
		return uom;
	}

	/**
	 * Create a unit of measure that is a unit divided by another unit
	 * 
	 * @param type        {@link UnitType}
	 * @param name        Name of unit of measure
	 * @param symbol      Symbol (must be unique)
	 * @param description Description of unit of measure
	 * @param dividend    {@link UnitOfMeasure}
	 * @param divisor     {@link UnitOfMeasure}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createQuotientUOM(UnitType type, String name, String symbol, String description,
			UnitOfMeasure dividend, UnitOfMeasure divisor) throws Exception {
		return this.createQuotientUOM(type, null, name, symbol, description, dividend, divisor);
	}

	/**
	 * Create a unit of measure that is a unit divided by another unit
	 * 
	 * @param dividend {@link UnitOfMeasure}
	 * @param divisor  {@link UnitOfMeasure}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createQuotientUOM(UnitOfMeasure dividend, UnitOfMeasure divisor) throws Exception {
		if (dividend == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("dividend.cannot.be.null"));
		}

		if (divisor == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("divisor.cannot.be.null"));
		}

		String symbol = UnitOfMeasure.generateQuotientSymbol(dividend, divisor);
		return createQuotientUOM(UnitType.UNCLASSIFIED, null, null, symbol, null, dividend, divisor);
	}

	/**
	 * Create a unit of measure that is the product of two other units of measure
	 * 
	 * @param type         {@link UnitType}
	 * @param id           {@link Unit}
	 * @param name         Name of unit of measure
	 * @param symbol       Symbol (must be unique)
	 * @param description  Description of unit of measure
	 * @param multiplier   {@link UnitOfMeasure} multiplier
	 * @param multiplicand {@link UnitOfMeasure} multiplicand
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createProductUOM(UnitType type, Unit id, String name, String symbol, String description,
			UnitOfMeasure multiplier, UnitOfMeasure multiplicand) throws Exception {

		UnitOfMeasure uom = createUOM(type, name, symbol, description);
		uom.setProductUnits(multiplier, multiplicand);
		uom.setEnumeration(id);
		registerUnit(uom);
		return uom;
	}

	/**
	 * Create a unit of measure that is the product of two other units of measure
	 * 
	 * @param type         {@link UnitType}
	 * @param name         Name of unit of measure
	 * @param symbol       Symbol (must be unique)
	 * @param description  Description of unit of measure
	 * @param multiplier   {@link UnitOfMeasure} multiplier
	 * @param multiplicand {@link UnitOfMeasure} multiplicand
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createProductUOM(UnitType type, String name, String symbol, String description,
			UnitOfMeasure multiplier, UnitOfMeasure multiplicand) throws Exception {
		return createProductUOM(type, null, name, symbol, description, multiplier, multiplicand);
	}

	/**
	 * Create a unit of measure that is the product of two other units of measure
	 * 
	 * @param multiplier   {@link UnitOfMeasure} multiplier
	 * @param multiplicand {@link UnitOfMeasure} multiplicand
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createProductUOM(UnitOfMeasure multiplier, UnitOfMeasure multiplicand) throws Exception {
		if (multiplier == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("multiplier.cannot.be.null"));
		}

		if (multiplicand == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("multiplicand.cannot.be.null"));
		}

		String symbol = UnitOfMeasure.generateProductSymbol(multiplier, multiplicand);
		return createProductUOM(UnitType.UNCLASSIFIED, null, null, symbol, null, multiplier, multiplicand);
	}

	/**
	 * Create a unit of measure with a base raised to an integral power
	 * 
	 * @param type        {@link UnitType}
	 * @param id          {@link Unit}
	 * @param name        Name of unit of measure
	 * @param symbol      Symbol (must be unique)
	 * @param description Description of unit of measure
	 * @param base        {@link UnitOfMeasure}
	 * @param exponent    Exponent
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createPowerUOM(UnitType type, Unit id, String name, String symbol, String description,
			UnitOfMeasure base, int exponent) throws Exception {

		UnitOfMeasure uom = createUOM(type, name, symbol, description);
		uom.setPowerUnit(base, exponent);
		uom.setEnumeration(id);
		registerUnit(uom);
		return uom;
	}

	/**
	 * Create a unit of measure with a base raised to an integral exponent
	 * 
	 * @param type        {@link UnitType}
	 * @param name        Name of unit of measure
	 * @param symbol      Symbol (must be unique)
	 * @param description Description of unit of measure
	 * @param base        {@link UnitOfMeasure}
	 * @param exponent    Exponent
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createPowerUOM(UnitType type, String name, String symbol, String description,
			UnitOfMeasure base, int exponent) throws Exception {
		return createPowerUOM(type, null, name, symbol, description, base, exponent);
	}

	/**
	 * Create a unit of measure with a base raised to an integral exponent
	 * 
	 * @param base     {@link UnitOfMeasure}
	 * @param exponent Exponent
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure createPowerUOM(UnitOfMeasure base, int exponent) throws Exception {
		if (base == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("base.cannot.be.null"));
		}

		String symbol = UnitOfMeasure.generatePowerSymbol(base, exponent);
		return createPowerUOM(UnitType.UNCLASSIFIED, null, null, symbol, null, base, exponent);
	}

	/**
	 * Create or fetch a unit of measure linearly scaled by the {@link Prefix}
	 * against the target unit of measure.
	 * 
	 * @param prefix    {@link Prefix} Scaling prefix with the scaling factor, e.g.
	 *                  1000
	 * @param targetUOM abscissa {@link UnitOfMeasure}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getUOM(Prefix prefix, UnitOfMeasure targetUOM) throws Exception {
		String symbol = prefix.getSymbol() + targetUOM.getSymbol();

		UnitOfMeasure scaled = getUOM(symbol);

		// if not found, create it
		if (scaled == null) {
			// generate a name and description
			String name = prefix.getName() + targetUOM.getName();
			String description = prefix.getFactor() + " " + targetUOM.getName();

			// scaling factor
			double scalingFactor = targetUOM.getScalingFactor() * prefix.getFactor();

			// create the unit of measure and set conversion
			scaled = createScalarUOM(targetUOM.getUnitType(), null, name, symbol, description);
			scaled.setConversion(scalingFactor, targetUOM.getAbscissaUnit());
		}
		return scaled;
	}

	/**
	 * Create or fetch a unit of measure linearly scaled by the {@link Prefix}
	 * against the target unit of measure.
	 * 
	 * @param prefix {@link Prefix} Scaling prefix with the scaling factor, e.g.
	 *               1000
	 * @param unit   {@link Unit}
	 * @return {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public UnitOfMeasure getUOM(Prefix prefix, Unit unit) throws Exception {
		return getUOM(prefix, MeasurementSystem.instance().getUOM(unit));
	}

	/**
	 * Get all the units of measure of the specified type
	 * 
	 * @param type {@link UnitType}
	 * @return List of {@link UnitOfMeasure}
	 * @throws Exception Exception
	 */
	public List<UnitOfMeasure> getUnitsOfMeasure(UnitType type) throws Exception {
		List<UnitOfMeasure> units = new ArrayList<>();

		switch (type) {
		case LENGTH:
			// SI
			units.add(getUOM(Unit.METRE));
			units.add(getUOM(Unit.ANGSTROM));
			units.add(getUOM(Unit.PARSEC));
			units.add(getUOM(Unit.ASTRONOMICAL_UNIT));

			// customary
			units.add(getUOM(Unit.FOOT));
			units.add(getUOM(Unit.INCH));
			units.add(getUOM(Unit.MIL));
			units.add(getUOM(Unit.POINT));
			units.add(getUOM(Unit.YARD));
			units.add(getUOM(Unit.MILE));
			units.add(getUOM(Unit.NAUTICAL_MILE));
			units.add(getUOM(Unit.FATHOM));
			break;

		case MASS:
			// SI
			units.add(getUOM(Unit.KILOGRAM));
			units.add(getUOM(Unit.TONNE));
			units.add(getUOM(Unit.CARAT));

			// customary
			units.add(getUOM(Unit.POUND_MASS));
			units.add(getUOM(Unit.OUNCE));
			units.add(getUOM(Unit.TROY_OUNCE));
			units.add(getUOM(Unit.SLUG));
			units.add(getUOM(Unit.GRAIN));

			// US
			units.add(getUOM(Unit.US_TON));

			// British
			units.add(getUOM(Unit.BR_TON));
			break;

		case TIME:
			units.add(getUOM(Unit.SECOND));
			units.add(getUOM(Unit.MINUTE));
			units.add(getUOM(Unit.HOUR));
			units.add(getUOM(Unit.DAY));
			units.add(getUOM(Unit.WEEK));
			units.add(getUOM(Unit.JULIAN_YEAR));

			break;

		case ACCELERATION:
			units.add(getUOM(Unit.METRE_PER_SEC_SQUARED));
			units.add(getUOM(Unit.FEET_PER_SEC_SQUARED));
			break;

		case AREA:
			// customary
			units.add(getUOM(Unit.SQUARE_INCH));
			units.add(getUOM(Unit.SQUARE_FOOT));
			units.add(getUOM(Unit.SQUARE_YARD));
			units.add(getUOM(Unit.ACRE));

			// SI
			units.add(getUOM(Unit.SQUARE_METRE));
			units.add(getUOM(Unit.HECTARE));

			break;

		case CATALYTIC_ACTIVITY:
			units.add(getUOM(Unit.KATAL));
			units.add(getUOM(Unit.UNIT));
			break;

		case COMPUTER_SCIENCE:
			units.add(getUOM(Unit.BIT));
			units.add(getUOM(Unit.BYTE));
			break;

		case DENSITY:
			units.add(getUOM(Unit.KILOGRAM_PER_CU_METRE));
			break;

		case DYNAMIC_VISCOSITY:
			units.add(getUOM(Unit.PASCAL_SECOND));
			break;

		case ELECTRIC_CAPACITANCE:
			units.add(getUOM(Unit.FARAD));
			break;

		case ELECTRIC_CHARGE:
			units.add(getUOM(Unit.COULOMB));
			break;

		case ELECTRIC_CONDUCTANCE:
			units.add(getUOM(Unit.SIEMENS));
			break;

		case ELECTRIC_CURRENT:
			units.add(getUOM(Unit.AMPERE));
			break;

		case ELECTRIC_FIELD_STRENGTH:
			units.add(getUOM(Unit.AMPERE_PER_METRE));
			break;

		case ELECTRIC_INDUCTANCE:
			units.add(getUOM(Unit.HENRY));
			break;

		case ELECTRIC_PERMITTIVITY:
			units.add(getUOM(Unit.FARAD_PER_METRE));
			break;

		case ELECTRIC_RESISTANCE:
			units.add(getUOM(Unit.OHM));
			break;

		case ELECTROMOTIVE_FORCE:
			units.add(getUOM(Unit.VOLT));
			break;

		case ENERGY:
			// customary
			units.add(getUOM(Unit.BTU));
			units.add(getUOM(Unit.FOOT_POUND_FORCE));

			// SI
			units.add(getUOM(Unit.CALORIE));
			units.add(getUOM(Unit.NEWTON_METRE));
			units.add(getUOM(Unit.JOULE));
			units.add(getUOM(Unit.WATT_HOUR));
			units.add(getUOM(Unit.ELECTRON_VOLT));
			break;

		case CURRENCY:
			units.add(getUOM(Unit.US_DOLLAR));
			units.add(getUOM(Unit.EURO));
			units.add(getUOM(Unit.YUAN));
			break;

		case FORCE:
			// customary
			units.add(getUOM(Unit.POUND_FORCE));

			// SI
			units.add(getUOM(Unit.NEWTON));
			break;

		case FREQUENCY:
			units.add(getUOM(Unit.REV_PER_MIN));
			units.add(getUOM(Unit.HERTZ));
			units.add(getUOM(Unit.RAD_PER_SEC));
			break;

		case ILLUMINANCE:
			units.add(getUOM(Unit.LUX));
			break;

		case INTENSITY:
			units.add(getUOM(Unit.DECIBEL));
			break;

		case IRRADIANCE:
			units.add(getUOM(Unit.WATTS_PER_SQ_METRE));
			break;

		case KINEMATIC_VISCOSITY:
			units.add(getUOM(Unit.SQUARE_METRE_PER_SEC));
			break;

		case LUMINOSITY:
			units.add(getUOM(Unit.CANDELA));
			break;

		case LUMINOUS_FLUX:
			units.add(getUOM(Unit.LUMEN));
			break;

		case MAGNETIC_FLUX:
			units.add(getUOM(Unit.WEBER));
			break;

		case MAGNETIC_FLUX_DENSITY:
			units.add(getUOM(Unit.TESLA));
			break;

		case MASS_FLOW:
			units.add(getUOM(Unit.KILOGRAM_PER_SEC));
			break;

		case MOLAR_CONCENTRATION:
			units.add(getUOM(Unit.MOLARITY));
			break;

		case PLANE_ANGLE:
			units.add(getUOM(Unit.DEGREE));
			units.add(getUOM(Unit.RADIAN));
			units.add(getUOM(Unit.ARC_SECOND));
			break;

		case POWER:
			units.add(getUOM(Unit.HP));
			units.add(getUOM(Unit.WATT));
			break;

		case PRESSURE:
			// customary
			units.add(getUOM(Unit.PSI));
			units.add(getUOM(Unit.IN_HG));

			// SI
			units.add(getUOM(Unit.PASCAL));
			units.add(getUOM(Unit.ATMOSPHERE));
			units.add(getUOM(Unit.BAR));
			break;

		case RADIATION_DOSE_ABSORBED:
			units.add(getUOM(Unit.GRAY));
			break;

		case RADIATION_DOSE_EFFECTIVE:
			units.add(getUOM(Unit.SIEVERT));
			break;

		case RADIATION_DOSE_RATE:
			units.add(getUOM(Unit.SIEVERTS_PER_HOUR));
			break;

		case RADIOACTIVITY:
			units.add(getUOM(Unit.BECQUEREL));
			break;

		case RECIPROCAL_LENGTH:
			units.add(getUOM(Unit.DIOPTER));
			break;

		case SOLID_ANGLE:
			units.add(getUOM(Unit.STERADIAN));
			break;

		case SUBSTANCE_AMOUNT:
			units.add(getUOM(Unit.MOLE));
			units.add(getUOM(Unit.EQUIVALENT));
			units.add(getUOM(Unit.INTERNATIONAL_UNIT));
			break;

		case TEMPERATURE:
			// customary
			units.add(getUOM(Unit.RANKINE));
			units.add(getUOM(Unit.FAHRENHEIT));

			// SI
			units.add(getUOM(Unit.KELVIN));
			units.add(getUOM(Unit.CELSIUS));
			break;

		case TIME_SQUARED:
			units.add(getUOM(Unit.SQUARE_SECOND));
			break;

		case UNCLASSIFIED:
			break;

		case UNITY:
			units.add(getUOM(Unit.ONE));
			units.add(getUOM(Unit.PERCENT));
			break;

		case VELOCITY:
			// customary
			units.add(getUOM(Unit.FEET_PER_SEC));
			units.add(getUOM(Unit.MILES_PER_HOUR));
			units.add(getUOM(Unit.KNOT));

			// SI
			units.add(getUOM(Unit.METRE_PER_SEC));
			break;

		case VOLUME:
			// British
			units.add(getUOM(Unit.BR_BUSHEL));
			units.add(getUOM(Unit.BR_CUP));
			units.add(getUOM(Unit.BR_FLUID_OUNCE));
			units.add(getUOM(Unit.BR_GALLON));
			units.add(getUOM(Unit.BR_PINT));
			units.add(getUOM(Unit.BR_QUART));
			units.add(getUOM(Unit.BR_TABLESPOON));
			units.add(getUOM(Unit.BR_TEASPOON));

			// customary
			units.add(getUOM(Unit.CUBIC_FOOT));
			units.add(getUOM(Unit.CUBIC_YARD));
			units.add(getUOM(Unit.CUBIC_INCH));
			units.add(getUOM(Unit.CORD));

			// SI
			units.add(getUOM(Unit.CUBIC_METRE));
			units.add(getUOM(Unit.LITRE));

			// US
			units.add(getUOM(Unit.US_BARREL));
			units.add(getUOM(Unit.US_BUSHEL));
			units.add(getUOM(Unit.US_CUP));
			units.add(getUOM(Unit.US_FLUID_OUNCE));
			units.add(getUOM(Unit.US_GALLON));
			units.add(getUOM(Unit.US_PINT));
			units.add(getUOM(Unit.US_QUART));
			units.add(getUOM(Unit.US_TABLESPOON));
			units.add(getUOM(Unit.US_TEASPOON));
			break;

		case VOLUMETRIC_FLOW:
			units.add(getUOM(Unit.CUBIC_METRE_PER_SEC));
			units.add(getUOM(Unit.CUBIC_FEET_PER_SEC));
			break;

		default:
			break;
		}

		return units;
	}

	public Map<UnitType, Integer> getTypeMap(UnitType unitType) {
		// check cache
		Map<UnitType, Integer> cachedMap = cacheManager.getUnitTypeCache().get(unitType);

		if (cachedMap != null) {
			return cachedMap;
		}

		// create map
		cachedMap = new ConcurrentHashMap<>();
		cacheManager.getUnitTypeCache().put(unitType, cachedMap);

		// base types have empty maps
		switch (unitType) {
		case UNITY:
			break;
		case LENGTH:
			break;
		case MASS:
			break;
		case TIME:
			break;
		case ELECTRIC_CURRENT:
			break;
		case TEMPERATURE:
			break;
		case SUBSTANCE_AMOUNT:
			break;
		case LUMINOSITY:
			break;
		case AREA:
			cachedMap.put(UnitType.LENGTH, 2);
			break;
		case VOLUME:
			cachedMap.put(UnitType.LENGTH, 3);
			break;
		case DENSITY:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, -3);
			break;
		case VELOCITY:
			cachedMap.put(UnitType.LENGTH, 1);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case VOLUMETRIC_FLOW:
			cachedMap.put(UnitType.LENGTH, 3);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case MASS_FLOW:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case FREQUENCY:
			cachedMap.put(UnitType.TIME, -1);
			break;
		case ACCELERATION:
			cachedMap.put(UnitType.LENGTH, 1);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case FORCE:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 1);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case PRESSURE:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, -1);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case ENERGY:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case POWER:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -3);
			break;
		case ELECTRIC_CHARGE:
			cachedMap.put(UnitType.ELECTRIC_CURRENT, 1);
			cachedMap.put(UnitType.TIME, 1);
			break;
		case ELECTROMOTIVE_FORCE:
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, -1);
			cachedMap.put(UnitType.TIME, -3);
			break;
		case ELECTRIC_RESISTANCE:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, -3);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, 2);
			cachedMap.put(UnitType.TIME, 4);
			break;
		case ELECTRIC_CAPACITANCE:
			cachedMap.put(UnitType.MASS, -1);
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, -2);
			cachedMap.put(UnitType.TIME, -3);
			break;
		case ELECTRIC_PERMITTIVITY:
			cachedMap.put(UnitType.MASS, -1);
			cachedMap.put(UnitType.LENGTH, -3);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, 2);
			cachedMap.put(UnitType.TIME, 4);
			break;
		case ELECTRIC_FIELD_STRENGTH:
			cachedMap.put(UnitType.ELECTRIC_CURRENT, 1);
			cachedMap.put(UnitType.LENGTH, -1);
			break;
		case MAGNETIC_FLUX:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, -1);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case MAGNETIC_FLUX_DENSITY:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, -1);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case ELECTRIC_INDUCTANCE:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, -2);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case ELECTRIC_CONDUCTANCE:
			cachedMap.put(UnitType.MASS, -1);
			cachedMap.put(UnitType.LENGTH, -2);
			cachedMap.put(UnitType.ELECTRIC_CURRENT, 2);
			cachedMap.put(UnitType.TIME, 3);
			break;
		case LUMINOUS_FLUX:
			cachedMap.put(UnitType.LUMINOSITY, 1);
			break;
		case ILLUMINANCE:
			cachedMap.put(UnitType.LUMINOSITY, 1);
			cachedMap.put(UnitType.LENGTH, -2);
			break;
		case RADIATION_DOSE_ABSORBED:
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case RADIATION_DOSE_EFFECTIVE:
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -2);
			break;
		case RADIATION_DOSE_RATE:
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -3);
			break;
		case RADIOACTIVITY:
			cachedMap.put(UnitType.TIME, -1);
			break;
		case CATALYTIC_ACTIVITY:
			cachedMap.put(UnitType.SUBSTANCE_AMOUNT, 1);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case DYNAMIC_VISCOSITY:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.LENGTH, 1);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case KINEMATIC_VISCOSITY:
			cachedMap.put(UnitType.LENGTH, 2);
			cachedMap.put(UnitType.TIME, -1);
			break;
		case RECIPROCAL_LENGTH:
			cachedMap.put(UnitType.LENGTH, -1);
			break;
		case PLANE_ANGLE:
			break;
		case SOLID_ANGLE:
			break;
		case INTENSITY:
			break;
		case COMPUTER_SCIENCE:
			break;
		case TIME_SQUARED:
			cachedMap.put(UnitType.TIME, 2);
			break;
		case MOLAR_CONCENTRATION:
			cachedMap.put(UnitType.SUBSTANCE_AMOUNT, 1);
			cachedMap.put(UnitType.LENGTH, -3);
			break;
		case IRRADIANCE:
			cachedMap.put(UnitType.MASS, 1);
			cachedMap.put(UnitType.TIME, -3);
			break;
		case CURRENCY:
			break;
		case UNCLASSIFIED:
			break;
		default:
			break;
		}
		return cachedMap;
	}

	private class CacheManager {
		// registry by unit symbol
		private final Map<String, UnitOfMeasure> symbolRegistry = new ConcurrentHashMap<>();

		// registry by base symbol
		private final Map<String, UnitOfMeasure> baseRegistry = new ConcurrentHashMap<>();

		// registry for units by enumeration
		private final Map<Unit, UnitOfMeasure> unitRegistry = new ConcurrentHashMap<>();

		// registry for base UOM map by unit type
		private final Map<UnitType, Map<UnitType, Integer>> unitTypeRegistry = new ConcurrentHashMap<>();

		private UnitOfMeasure getUOM(Unit unit) {
			return unitRegistry.get(unit);
		}

		private UnitOfMeasure getUOM(String symbol) {
			return symbolRegistry.get(symbol);
		}

		private UnitOfMeasure getBaseUOM(String baseSymbol) {
			return baseRegistry.get(baseSymbol);
		}

		private void clearCache() {
			symbolRegistry.clear();
			baseRegistry.clear();
			unitRegistry.clear();
		}

		private Collection<UnitOfMeasure> getCachedUnits() {
			return symbolRegistry.values();
		}

		private Map<String, UnitOfMeasure> getSymbolCache() {
			return symbolRegistry;
		}

		private Map<String, UnitOfMeasure> getBaseSymbolCache() {
			return baseRegistry;
		}

		private Map<Unit, UnitOfMeasure> getEnumerationCache() {
			return unitRegistry;
		}

		private Map<UnitType, Map<UnitType, Integer>> getUnitTypeCache() {
			return unitTypeRegistry;
		}

		private void unregisterUnit(UnitOfMeasure uom) throws Exception {
			if (uom.getEnumeration() != null) {
				unitRegistry.remove(uom.getEnumeration());
			}

			// remove by symbol and base symbol
			symbolRegistry.remove(uom.getSymbol());
			baseRegistry.remove(uom.getBaseSymbol());
		}

		private void registerUnit(UnitOfMeasure uom) throws Exception {
			String key = uom.getSymbol();

			// get first by symbol
			UnitOfMeasure current = symbolRegistry.get(key);

			if (current != null) {
				// already cached
				return;
			}

			// cache it
			symbolRegistry.put(key, uom);

			// next by unit enumeration
			Unit id = uom.getEnumeration();

			if (id != null) {
				unitRegistry.put(id, uom);
			}

			// finally by base symbol
			key = uom.getBaseSymbol();

			if (baseRegistry.get(key) == null) {
				baseRegistry.put(key, uom);
			}
		}
	}
}
