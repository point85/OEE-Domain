package org.point85.domain.modbus;

import java.util.Objects;

/**
 * This class holds the java value for Modbus native data
 *
 */
public class ModbusVariant {
	private ModbusDataType dataType;
	private Byte byteValue;
	private Number numberValue;
	private Boolean booleanValue;
	private String stringValue;

	public ModbusVariant(ModbusDataType type, Byte value) {
		this.dataType = type;
		this.byteValue = value;
	}

	public ModbusVariant(ModbusDataType type, Number value) {
		this.dataType = type;
		this.numberValue = value;
	}

	public ModbusVariant(Boolean value) {
		this.dataType = ModbusDataType.DISCRETE;
		this.booleanValue = value;
	}

	public ModbusVariant(String value) {
		this.dataType = ModbusDataType.STRING;
		this.stringValue = value;
	}

	public Byte getByte() {
		return byteValue;
	}

	public Number getNumber() {
		return numberValue;
	}

	public Boolean getBoolean() {
		return booleanValue;
	}

	public ModbusDataType getDataType() {
		return dataType;
	}

	public String getString() {
		return stringValue;
	}

	/**
	 * Check to see if the numerical variant is greater than zero
	 * 
	 * @return true if greater than 0
	 */
	public boolean isPositiveNumber() {
		boolean value = false;

		switch (dataType) {
		case DOUBLE:
			value = numberValue.doubleValue() > 0.0d ? true : false;
			break;

		case INT16:
			value = numberValue.shortValue() > 0 ? true : false;
			break;

		case UINT16:
		case INT32:
			value = numberValue.intValue() > 0 ? true : false;
			break;

		case UINT32:
		case INT64:
			value = numberValue.longValue() > 0 ? true : false;
			break;
		case SINGLE:
			value = numberValue.floatValue() > 0.0f ? true : false;
			break;

		case BYTE_HIGH:
		case BYTE_LOW:
		case DISCRETE:
		case STRING:
		default:
			break;
		}
		return value;
	}

	/**
	 * Get the hash code
	 * 
	 * @return hash code
	 */
	public int hashCode() {
		return Objects.hash(booleanValue, byteValue, numberValue, stringValue);
	}

	/**
	 * Compare this unit of measure to another
	 * 
	 * @return true if equal
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ModbusVariant)) {
			return false;
		}
		ModbusVariant variant = (ModbusVariant) other;

		if (!dataType.equals(variant.dataType)) {
			return false;
		}

		if (booleanValue != null && variant.booleanValue != null && booleanValue.equals(variant.booleanValue)) {
			return true;
		}

		if (byteValue != null && variant.byteValue != null && byteValue.equals(variant.byteValue)) {
			return true;
		}

		if (numberValue != null && variant.numberValue != null && numberValue.equals(variant.numberValue)) {
			return true;
		}

		if (stringValue != null && variant.stringValue != null && stringValue.equals(variant.stringValue)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String value = null;

		if (numberValue != null) {
			value = numberValue.toString();
		} else if (booleanValue != null) {
			value = booleanValue.toString();
		} else if (byteValue != null) {
			value = String.format("%d", byteValue);
		} else if (stringValue != null) {
			value = stringValue;
		}
		return value;
	}
}
