package org.point85.domain.modbus;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * Enumeration of the data types supported by the ModbusMaster
 *
 */
public enum ModbusDataType {
	DISCRETE, BYTE_LOW, BYTE_HIGH, INT16, UINT16, INT32, UINT32, INT64, SINGLE, DOUBLE, STRING;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case BYTE_LOW:
			key = "byte.low.type";
			break;
		case BYTE_HIGH:
			key = "byte.high.type";
			break;
		case DOUBLE:
			key = "double.type";
			break;
		case INT16:
			key = "int16.type";
			break;
		case INT32:
			key = "int32.type";
			break;
		case INT64:
			key = "int64.type";
			break;
		case SINGLE:
			key = "single.type";
			break;
		case UINT16:
			key = "uint16.type";
			break;
		case UINT32:
			key = "uint32.type";
			break;
		case DISCRETE:
			key = "discrete.type";
			break;
		case STRING:
			key = "string.type";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
