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

import org.point85.domain.i18n.DomainLocalizer;

/**
 * UnitType is an enumeration of unit of measure types. Only units of measure
 * with the same type can be converted.
 * 
 * @author Kent Randall
 *
 */
public enum UnitType {
	// dimension-less "1"
	UNITY,

	// fundamental
	LENGTH, MASS, TIME, ELECTRIC_CURRENT, TEMPERATURE, SUBSTANCE_AMOUNT, LUMINOSITY,

	// other physical
	AREA, VOLUME, DENSITY, VELOCITY, VOLUMETRIC_FLOW, MASS_FLOW, FREQUENCY, ACCELERATION, FORCE, PRESSURE, ENERGY, POWER, ELECTRIC_CHARGE, 
	ELECTROMOTIVE_FORCE, ELECTRIC_RESISTANCE, ELECTRIC_CAPACITANCE, ELECTRIC_PERMITTIVITY, ELECTRIC_FIELD_STRENGTH,
	MAGNETIC_FLUX, MAGNETIC_FLUX_DENSITY, ELECTRIC_INDUCTANCE, ELECTRIC_CONDUCTANCE, 
	LUMINOUS_FLUX, ILLUMINANCE, RADIATION_DOSE_ABSORBED, RADIATION_DOSE_EFFECTIVE, RADIATION_DOSE_RATE, RADIOACTIVITY, CATALYTIC_ACTIVITY, DYNAMIC_VISCOSITY, 
	KINEMATIC_VISCOSITY, RECIPROCAL_LENGTH, PLANE_ANGLE, SOLID_ANGLE, INTENSITY, COMPUTER_SCIENCE, TIME_SQUARED, MOLAR_CONCENTRATION, IRRADIANCE,
	
	// currency
	CURRENCY,
	
	// unclassified.  Reserved for use when creating custom units of measure.
	UNCLASSIFIED;
	
	@Override
	public String toString() {
		String key = null;
		
		switch (this) {
		case ACCELERATION:
			key = "ut.acceleration";
			break;
		case AREA:
			key = "ut.area";
			break;
		case CATALYTIC_ACTIVITY:
			key = "ut.cat.activity";
			break;
		case COMPUTER_SCIENCE:
			key = "ut.comp.sci";
			break;
		case CURRENCY:
			key = "ut.currency";
			break;
		case DENSITY:
			key = "ut.density";
			break;
		case DYNAMIC_VISCOSITY:
			key = "ut.dyn.vis";
			break;
		case ELECTRIC_CAPACITANCE:
			key = "ut.elec.cap";
			break;
		case ELECTRIC_CHARGE:
			key = "ut.elec.charge";
			break;
		case ELECTRIC_CONDUCTANCE:
			key = "ut.elec.cond";
			break;
		case ELECTRIC_CURRENT:
			key = "ut.elec.cur";
			break;
		case ELECTRIC_FIELD_STRENGTH:
			key = "ut.elec.field";
			break;
		case ELECTRIC_INDUCTANCE:
			key = "ut.elec.ind";
			break;
		case ELECTRIC_PERMITTIVITY:
			key = "ut.elec.perm";
			break;
		case ELECTRIC_RESISTANCE:
			key = "ut.elec.res";
			break;
		case ELECTROMOTIVE_FORCE:
			key = "ut.emf";
			break;
		case ENERGY:
			key = "ut.energy";
			break;
		case FORCE:
			key = "ut.force";
			break;
		case FREQUENCY:
			key = "ut.freq";
			break;
		case ILLUMINANCE:
			key = "ut.illum";
			break;
		case INTENSITY:
			key = "ut.intent";
			break;
		case IRRADIANCE:
			key = "ut.irrad";
			break;
		case KINEMATIC_VISCOSITY:
			key = "ut.kin.vis";
			break;
		case LENGTH:
			key = "ut.length";
			break;
		case LUMINOSITY:
			key = "ut.lumin";
			break;
		case LUMINOUS_FLUX:
			key = "ut.lumin.flux";
			break;
		case MAGNETIC_FLUX:
			key = "ut.mag.flux";
			break;
		case MAGNETIC_FLUX_DENSITY:
			key = "ut.mag.flux.den";
			break;
		case MASS:
			key = "ut.mass";
			break;
		case MASS_FLOW:
			key = "ut.mass.flow";
			break;
		case MOLAR_CONCENTRATION:
			key = "ut.mol.conc";
			break;
		case PLANE_ANGLE:
			key = "ut.plane.angle";
			break;
		case POWER:
			key = "ut.power";
			break;
		case PRESSURE:
			key = "ut.pressure";
			break;
		case RADIATION_DOSE_ABSORBED:
			key = "ut.rad.dose.abs";
			break;
		case RADIATION_DOSE_EFFECTIVE:
			key = "ut.rad.dose.eff";
			break;
		case RADIATION_DOSE_RATE:
			key = "ut.rad.dose.rate";
			break;
		case RADIOACTIVITY:
			key = "ut.radio";
			break;
		case RECIPROCAL_LENGTH:
			key = "ut.recip.len";
			break;
		case SOLID_ANGLE:
			key = "ut.solid.angle";
			break;
		case SUBSTANCE_AMOUNT:
			key = "ut.sub.amount";
			break;
		case TEMPERATURE:
			key = "ut.temperature";
			break;
		case TIME:
			key = "ut.time";
			break;
		case TIME_SQUARED:
			key = "ut.sq.time";
			break;
		case UNCLASSIFIED:
			key = "ut.unclassified";
			break;
		case UNITY:
			key = "ut.unity";
			break;
		case VELOCITY:
			key = "ut.velocity";
			break;
		case VOLUME:
			key = "ut.volume";
			break;
		case VOLUMETRIC_FLOW:
			key = "ut.vol.flow";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getUnitString(key);
	}
}
