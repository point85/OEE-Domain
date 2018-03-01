/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.domain.opc.da;

/**
 *
 * @author krandal
 */
public enum OpcDaVariantType {

	BYTE, BOOLEAN, I1, I2, I4, I8, R4, R8, STRING, DATE, UNKNOWN;

	public Class<?> getJavaClass() {
		Class<?> clazz = Object.class;

		switch (this) {
		case BOOLEAN:
			clazz = Boolean.class;
			break;
		case BYTE:
		case I1:
			clazz = Byte.class;
			break;
		case DATE:
			clazz = java.util.Date.class;
			break;
		case I2:
			clazz = Short.class;
			break;
		case I4:
			clazz = Integer.class;
			break;
		case I8:
			clazz = Long.class;
			break;
		case R4:
			clazz = Float.class;
			break;
		case R8:
			clazz = Double.class;
			break;
		case STRING:
			clazz = String.class;
			break;
		case UNKNOWN:
		default:
			break;
		}
		return clazz;
	}
}
