package org.point85.domain.modbus;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * Enumeration of the types of Modbus registers
 *
 */
public enum ModbusRegisterType {
	COIL, DISCRETE, HOLDING_REGISTER, INPUT_REGISTER;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case COIL:
			key = "coil.type";
			break;

		case DISCRETE:
			key = "discrete.type";
			break;

		case HOLDING_REGISTER:
			key = "holding.type";
			break;

		case INPUT_REGISTER:
			key = "input.type";
			break;

		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}

	public String toDatabaseString() {
		String value = null;

		switch (this) {
		case COIL:
			value = "C";
			break;

		case DISCRETE:
			value = "D";
			break;

		case HOLDING_REGISTER:
			value = "H";
			break;

		case INPUT_REGISTER:
			value = "I";
			break;

		default:
			break;
		}
		return value;
	}

	public static ModbusRegisterType fromDatabaseString(String value) {
		ModbusRegisterType type = null;

		if (value.equals("C")) {
			type = ModbusRegisterType.COIL;
		} else if (value.equals("D")) {
			type = ModbusRegisterType.DISCRETE;
		} else if (value.equals("H")) {
			type = ModbusRegisterType.HOLDING_REGISTER;
		} else if (value.equals("I")) {
			type = ModbusRegisterType.INPUT_REGISTER;
		}
		return type;
	}
}
