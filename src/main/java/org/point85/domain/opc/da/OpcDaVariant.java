package org.point85.domain.opc.da;

import java.util.Date;

import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JICurrency;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIUnsignedByte;
import org.jinterop.dcom.core.JIUnsignedInteger;
import org.jinterop.dcom.core.JIUnsignedShort;
import org.jinterop.dcom.core.JIVariant;

/**
 *
 * @author Kent Randall
 */
public class OpcDaVariant {

	private final JIVariant jiVariant;

	public OpcDaVariant(JIVariant variant) {
		this.jiVariant = variant;
	}

	public OpcDaVariant(String value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(byte value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(int value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(short value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(long value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(boolean value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(float value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(double value) {
		this.jiVariant = new JIVariant(value);
	}

	public OpcDaVariant(Date value) {
		this.jiVariant = new JIVariant(value);
	}

	public JIVariant getJIVariant() {
		return this.jiVariant;
	}

	public String getTypeAsString() {
		String type = "unknown";

		try {
			type = OpcDaVariant.getDisplayType(jiVariant.getType());
		} catch (Exception e) {
		}
		return type;
	}

	public static String getDisplayType(int dataType) {

		String typeString = "unknown";

		switch (dataType) {
		case JIVariant.VT_BOOL:
			typeString = "boolean";
			break;

		case JIVariant.VT_I1:
			typeString = "signed 1-byte int";
			break;

		case JIVariant.VT_I2:
			typeString = "signed 2-byte int";
			break;

		case JIVariant.VT_INT:
		case JIVariant.VT_I4:
			typeString = "signed 4-byte int";
			break;

		case JIVariant.VT_I8:
			typeString = "signed 8-byte int";
			break;

		case JIVariant.VT_UI1:
			typeString = "unsigned 1-byte int";
			break;

		case JIVariant.VT_UI2:
			typeString = "unsigned 2-byte int";
			break;

		case JIVariant.VT_UINT:
		case JIVariant.VT_UI4:
			typeString = "unsigned 4-byte int";
			break;

		case JIVariant.VT_R4:
		case JIVariant.VT_DECIMAL:
			typeString = "4-byte single float";
			break;

		case JIVariant.VT_R8:
			typeString = "8-byte double float";
			break;

		case JIVariant.VT_BSTR:
			typeString = "string";
			break;

		case JIVariant.VT_DATE:
			typeString = "date";
			break;

		default:
			break;
		}
		return typeString;

	}

	public boolean isArray() throws Exception {
		return jiVariant.isArray();
	}

	public String getValueAsString() throws Exception {
		if (!isArray()) {
			return getScalarValueAsString();
		} else {
			return getArrayValueAsString();
		}
	}

	public String getArrayValueAsString() throws Exception {
		String valueString = "";

		if (!isArray()) {
			return valueString;
		}

		JIArray jiArray = jiVariant.getObjectAsArray();

		Object[] values = (Object[]) jiArray.getArrayInstance();

		boolean isStringArray = false;
		if (values instanceof JIString[]) {
			isStringArray = true;
		}
		valueString += "Array of size " + values.length + "\n";

		valueString += "[";
		for (int i = 0; i < values.length; i++) {
			if (i > 0 && i <= (values.length - 1)) {
				valueString += ", ";
			}

			// convert to string
			if (!isStringArray) {
				if (values instanceof JIUnsignedByte[]) {
					valueString += ((JIUnsignedByte) values[i]).getValue().toString();
				} else if (values instanceof JIUnsignedShort[]) {
					valueString += ((JIUnsignedShort) values[i]).getValue().toString();
				} else if (values instanceof JIUnsignedInteger[]) {
					valueString += ((JIUnsignedInteger) values[i]).getValue().toString();
				} else {
					valueString += values[i];
				}
			} else {
				valueString += ((JIString) values[i]).getString();
			}
		}
		valueString += "]";

		return valueString;
	}

	public String getScalarValueAsString() throws Exception {
		String valueString = "";

		switch (jiVariant.getType()) {
		case JIVariant.VT_BOOL:
			valueString = Boolean.toString(jiVariant.getObjectAsBoolean());
			break;

		case JIVariant.VT_I1:
			char c = jiVariant.getObjectAsChar();
			StringBuilder sb = new StringBuilder();
			sb.append(c);
			byte[] bytes = sb.toString().getBytes();

			StringBuilder builder = new StringBuilder();
			for (byte b : bytes) {
				builder.append("0x" + String.format("%02x", b));
			}
			valueString = builder.toString();
			break;

		case JIVariant.VT_I2:
			valueString = Short.toString(jiVariant.getObjectAsShort());
			break;

		case JIVariant.VT_INT:
		case JIVariant.VT_I4:
			valueString = Integer.toString(jiVariant.getObjectAsInt());
			break;

		case JIVariant.VT_I8:
			valueString = Long.toString(jiVariant.getObjectAsLong());
			break;

		case JIVariant.VT_UI1:
			JIUnsignedByte ub = (JIUnsignedByte) jiVariant.getObject();
			valueString = ub.getValue().toString();
			break;

		case JIVariant.VT_UI2:
			JIUnsignedShort us = (JIUnsignedShort) jiVariant.getObject();
			valueString = us.getValue().toString();
			break;

		case JIVariant.VT_UINT:
		case JIVariant.VT_UI4:
			JIUnsignedInteger ui = (JIUnsignedInteger) jiVariant.getObject();
			valueString = ui.getValue().toString();
			break;

		case JIVariant.VT_R4:
		case JIVariant.VT_DECIMAL:
			valueString = Float.toString(jiVariant.getObjectAsFloat());
			break;

		case JIVariant.VT_R8:
			valueString = Double.toString(jiVariant.getObjectAsDouble());
			break;

		case JIVariant.VT_BSTR:
			valueString = jiVariant.getObjectAsString2();
			break;

		case JIVariant.VT_DATE:
			valueString = jiVariant.getObjectAsDate().toString();
			break;

		case JIVariant.VT_CY:
			JICurrency currency = (JICurrency) jiVariant.getObject();
			valueString = currency.getUnits() + "." + currency.getFractionalUnits();
			break;

		case JIVariant.VT_ERROR:
			valueString = String.format("%08X", jiVariant.getObjectAsSCODE());
			break;

		default:
			Object o = jiVariant.getObject();
			valueString = o.toString();
			break;
		}

		return valueString;
	}

	public Number getValueAsNumber() {
		Number numberValue = null;

		try {
			switch (getJIVariant().getType()) {
			case JIVariant.VT_I1:
			case JIVariant.VT_UI1:
				numberValue = new Byte((byte) getJIVariant().getObjectAsChar());
				break;

			case JIVariant.VT_I2:
			case JIVariant.VT_UI2:
				numberValue = new Short(getJIVariant().getObjectAsShort());
				break;

			case JIVariant.VT_INT:
			case JIVariant.VT_UINT:
			case JIVariant.VT_UI4:
			case JIVariant.VT_I4:
				numberValue = new Integer(getJIVariant().getObjectAsInt());
				break;

			case JIVariant.VT_I8:
				numberValue = new Long(getJIVariant().getObjectAsLong());
				break;

			case JIVariant.VT_R4:
			case JIVariant.VT_DECIMAL:
				numberValue = new Float(getJIVariant().getObjectAsFloat());
				break;

			case JIVariant.VT_R8:
				numberValue = new Double(getJIVariant().getObjectAsDouble());
				break;

			case JIVariant.VT_BOOL:
				byte boolValue = 0;
				if (getJIVariant().getObjectAsBoolean()) {
					boolValue = 1;
				}
				numberValue = new Byte(boolValue);
				break;

			case JIVariant.VT_BSTR:
			case JIVariant.VT_DATE:
			default:
				break;
			}
		} catch (Exception e) {
		}
		return numberValue;
	}

	public OpcDaVariantType getDataType() throws Exception {
		OpcDaVariantType type = OpcDaVariantType.UNKNOWN;

		switch (getJIVariant().getType()) {
		case JIVariant.VT_I1:
		case JIVariant.VT_UI1:
			type = OpcDaVariantType.I1;
			break;

		case JIVariant.VT_I2:
		case JIVariant.VT_UI2:
			type = OpcDaVariantType.I2;
			break;

		case JIVariant.VT_INT:
		case JIVariant.VT_UINT:
		case JIVariant.VT_UI4:
		case JIVariant.VT_I4:
			type = OpcDaVariantType.I4;
			break;

		case JIVariant.VT_I8:
			type = OpcDaVariantType.I8;
			break;

		case JIVariant.VT_R4:
		case JIVariant.VT_DECIMAL:
			type = OpcDaVariantType.R4;
			break;

		case JIVariant.VT_R8:
			type = OpcDaVariantType.R8;
			break;

		case JIVariant.VT_BOOL:
			type = OpcDaVariantType.BOOLEAN;
			break;

		case JIVariant.VT_BSTR:
			type = OpcDaVariantType.STRING;
			break;

		case JIVariant.VT_DATE:
			type = OpcDaVariantType.DATE;
			break;

		default:
			break;
		}

		return type;
	}

	public boolean isNumeric() {
		return getValueAsNumber() != null ? true : false;
	}

	@Override
	public String toString() {
		String value = "";
		try {
			value = getValueAsString();
		} catch (Exception e) {
		}
		return value;
	}
}
