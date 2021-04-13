package org.point85.domain.proficy;

import java.time.Instant;

/**
 * Tag data type enumeration
 *
 */
public enum TagDataType {
	ihDataTypeUndefined(0, "DataTypeUndefined"), ihScaled(1, "Scaled"), ihFloat(2, "Float"),
	ihDoubleFloat(3, "DoubleFloat"), ihInteger(4, "Integer"), ihDoubleInteger(5, "DoubleInteger"),
	ihFixedString(6, "FixedString"), ihVariableString(7, "VariableString"), ihBlob(8, "Blob"), ihTime(9, "Time"),
	ihInt64(10, "Int64"), ihUInt64(11, "UInt64"), ihUInt32(12, "UInt32"), ihUInt16(13, "UInt16("), ihByte(14, "Byte"),
	ihBool(15, "Bool"), ihMultiField(16, "MultiField"), ihArray(17, "Array");

	private int intValue;

	private String stringValue;

	private TagDataType(int intValue, String stringValue) {
		this.intValue = intValue;
		this.stringValue = stringValue;
	}

	/**
	 * Return the tag data type from the integer enumeration
	 * 
	 * @param value integer value
	 * @return TagDataType
	 */
	public static TagDataType fromInt(int value) {
		TagDataType type = null;

		switch (value) {
		case 0:
			type = ihDataTypeUndefined;
			break;
		case 1:
			type = ihScaled;
			break;
		case 2:
			type = ihFloat;
			break;
		case 3:
			type = ihDoubleFloat;
			break;
		case 4:
			type = ihInteger;
			break;
		case 5:
			type = ihDoubleInteger;
			break;
		case 6:
			type = ihFixedString;
			break;
		case 7:
			type = ihVariableString;
			break;
		case 8:
			type = ihBlob;
			break;
		case 9:
			type = ihTime;
			break;
		case 10:
			type = ihInt64;
			break;
		case 11:
			type = ihUInt64;
			break;
		case 12:
			type = ihUInt32;
			break;
		case 13:
			type = ihUInt16;
			break;
		case 14:
			type = ihByte;
			break;
		case 15:
			type = ihBool;
			break;
		case 16:
			type = ihMultiField;
			break;
		case 17:
			type = ihArray;
			break;
		default:
			break;
		}

		return type;
	}

	/**
	 * Return the tag data type from the string value
	 * 
	 * @param value String value
	 * @return TagDataType
	 */
	public static TagDataType fromString(String value) {
		TagDataType type = null;

		switch (value) {
		case "DataTypeUndefined":
			type = ihDataTypeUndefined;
			break;
		case "Scaled":
			type = ihScaled;
			break;
		case "Float":
			type = ihFloat;
			break;
		case "DoubleFloat":
			type = ihDoubleFloat;
			break;
		case "Integer":
			type = ihInteger;
			break;
		case "DoubleInteger":
			type = ihDoubleInteger;
			break;
		case "FixedString":
			type = ihFixedString;
			break;
		case "VariableString":
			type = ihVariableString;
			break;
		case "Blob":
			type = ihBlob;
			break;
		case "Time":
			type = ihTime;
			break;
		case "Int64":
			type = ihInt64;
			break;
		case "UInt64":
			type = ihUInt64;
			break;
		case "UInt32":
			type = ihUInt32;
			break;
		case "UInt16":
			type = ihUInt16;
			break;
		case "Byte":
			type = ihByte;
			break;
		case "Bool":
			type = ihBool;
			break;
		case "MultiField":
			type = ihMultiField;
			break;
		case "Array":
			type = ihArray;
			break;
		default:
			break;
		}

		return type;
	}

	/**
	 * Get the Java class for this data type
	 * 
	 * @return Class of data type
	 */
	public Class<?> getJavaType() {
		Class<?> type = null;

		switch (this) {
		case ihFloat:
			type = Float.class;
			break;

		case ihDoubleFloat:
			type = Double.class;
			break;

		case ihInteger:
		case ihUInt16:
			type = Integer.class;
			break;

		case ihDoubleInteger:
		case ihInt64:
		case ihUInt32:
			type = Long.class;
			break;

		case ihFixedString:
		case ihVariableString:
			type = String.class;
			break;

		case ihTime:
			type = Instant.class;
			break;

		case ihByte:
			type = Byte.class;
			break;

		case ihBool:
			type = Boolean.class;
			break;

		default:
			break;
		}
		return type;
	}

	public int getIntValue() {
		return intValue;
	}

	public String getStringValue() {
		return stringValue;
	}

	public boolean isArray() {
		return intValue == 17;
	}

	@Override
	public String toString() {
		return stringValue;
	}
}
