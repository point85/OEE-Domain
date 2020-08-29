package org.point85.domain.modbus;

import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.util.BitVector;

/**
 * This class has utility methods for handling Modbus data.
 *
 */
public class ModbusUtils {
	private ModbusUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Convert the 4 two-byte words to a Long signed integer
	 * 
	 * @param word1            First word
	 * @param word2            Second word
	 * @param word3            Third word
	 * @param word4            Fourth word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Long integer
	 */
	public static Long toLong(byte[] word1, byte[] word2, byte[] word3, byte[] word4, boolean reverseEndianess) {
		byte[] bytes = new byte[8];

		if (reverseEndianess) {
			bytes[0] = word4[1];
			bytes[1] = word4[0];
			bytes[2] = word3[1];
			bytes[3] = word3[0];
			bytes[4] = word2[1];
			bytes[5] = word2[0];
			bytes[6] = word1[1];
			bytes[7] = word1[0];
		} else {
			bytes[0] = word1[0];
			bytes[1] = word1[1];
			bytes[2] = word2[0];
			bytes[3] = word2[1];
			bytes[4] = word3[0];
			bytes[5] = word3[1];
			bytes[6] = word4[0];
			bytes[7] = word4[1];
		}
		return Long.valueOf(ByteBuffer.wrap(bytes).getLong());
	}

	/**
	 * Convert the 2 two-byte words to a Long unsigned integer
	 * 
	 * @param word1            First word
	 * @param word2            Second word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Long integer
	 */
	public static Long toUnsignedInteger(byte[] word1, byte[] word2, boolean reverseEndianess) {
		byte[] bytes = new byte[8];

		if (reverseEndianess) {
			bytes[4] = word2[1];
			bytes[5] = word2[0];
			bytes[6] = word1[1];
			bytes[7] = word1[0];
		} else {
			bytes[4] = word1[0];
			bytes[5] = word1[1];
			bytes[6] = word2[0];
			bytes[7] = word2[1];
		}
		return Long.valueOf(ByteBuffer.wrap(bytes).getLong());
	}

	/**
	 * Convert the 2 two-byte words to a signed Integer
	 * 
	 * @param word1            First word
	 * @param word2            Second word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Integer integer
	 */
	public static Integer toInteger(byte[] word1, byte[] word2, boolean reverseEndianess) {
		byte[] bytes = new byte[4];

		if (reverseEndianess) {
			bytes[0] = word2[1];
			bytes[1] = word2[0];
			bytes[2] = word1[1];
			bytes[3] = word1[0];
		} else {
			bytes[0] = word1[0];
			bytes[1] = word1[1];
			bytes[2] = word2[0];
			bytes[3] = word2[1];
		}
		return Integer.valueOf(ByteBuffer.wrap(bytes).getInt());
	}

	/**
	 * Convert the two-byte word to a unsigned Integer
	 * 
	 * @param word             Word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Integer integer
	 */
	public static Integer toUnsignedShort(byte[] word, boolean reverseEndianess) {
		byte[] bytes = new byte[4];

		if (reverseEndianess) {
			bytes[2] = word[1];
			bytes[3] = word[0];
		} else {
			bytes[2] = word[0];
			bytes[3] = word[1];
		}
		return Integer.valueOf(ByteBuffer.wrap(bytes).getInt());
	}

	/**
	 * Convert the two-byte word to a signed Short
	 * 
	 * @param word             Word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Short integer
	 */
	public static Short toShort(byte[] word, boolean reverseEndianess) {
		short value = 0;

		if (reverseEndianess) {
			byte[] bytes = new byte[2];
			bytes[0] = word[1];
			bytes[1] = word[0];
			value = ByteBuffer.wrap(bytes).getShort();
		} else {
			value = ByteBuffer.wrap(word).getShort();
		}
		return Short.valueOf(value);
	}

	/**
	 * Convert the 2-byte word into a byte array
	 * 
	 * @param word             Word
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return byte array
	 */
	public static byte[] toByteArray(byte[] word, boolean reverseEndianess) {
		byte[] bytes = new byte[2];

		if (reverseEndianess) {
			bytes[0] = word[0];
			bytes[1] = word[1];
		} else {
			bytes[0] = word[1];
			bytes[1] = word[0];
		}
		return bytes;
	}

	/**
	 * Convert the 4-byte single precision float to a byte array
	 * 
	 * @param value Single precision float
	 * @return byte array
	 */
	public static byte[] toByteArray(float value) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putFloat(value);
		return bytes;
	}

	/**
	 * Convert the 4-byte integer to a byte array
	 * 
	 * @param value int
	 * @return byte array
	 */
	public static byte[] toByteArray(int value) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(value);
		return bytes;
	}

	/**
	 * Convert the 2 two-byte words to a single precision floating point number
	 * 
	 * @param word1            Word1
	 * @param word2            Word2
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Float
	 */
	public static Float toFloat(byte[] word1, byte[] word2, boolean reverseEndianess) {
		byte[] bytes = new byte[4];

		if (reverseEndianess) {
			bytes[0] = word2[1];
			bytes[1] = word2[0];
			bytes[2] = word1[1];
			bytes[3] = word1[0];
		} else {
			bytes[0] = word1[0];
			bytes[1] = word1[1];
			bytes[2] = word2[0];
			bytes[3] = word2[1];
		}
		return Float.valueOf(ByteBuffer.wrap(bytes).getFloat());
	}

	/**
	 * Convert the 4 two-byte words to a double precision floating point number
	 * 
	 * @param word1            Word1
	 * @param word2            Word2
	 * @param word3            Word3
	 * @param word4            Word4
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return Double
	 */
	public static Double toDouble(byte[] word1, byte[] word2, byte[] word3, byte[] word4, boolean reverseEndianess) {
		byte[] bytes = new byte[8];

		if (reverseEndianess) {
			bytes[0] = word4[1];
			bytes[1] = word4[0];
			bytes[2] = word3[1];
			bytes[3] = word3[0];
			bytes[4] = word2[1];
			bytes[5] = word2[0];
			bytes[6] = word1[1];
			bytes[7] = word1[0];
		} else {
			bytes[0] = word1[0];
			bytes[1] = word1[1];
			bytes[2] = word2[0];
			bytes[3] = word2[1];
			bytes[4] = word3[0];
			bytes[5] = word3[1];
			bytes[6] = word4[0];
			bytes[7] = word4[1];
		}
		return Double.valueOf(ByteBuffer.wrap(bytes).getDouble());
	}

	/**
	 * Convert the array of two-byte words into a UTF8 string
	 * 
	 * @param data             Array of words
	 * @param charCount        Number of characters in the string
	 * @param reverseEndianess If true, swap the high and low order bytes in the
	 *                         word
	 * @return String
	 */
	public static String toUTF8String(byte[][] data, int charCount, boolean reverseEndianess) {
		int wordCount = (charCount + 1) / 2;

		byte[] stringBytes = new byte[charCount + 1];

		int j = 0;
		for (int i = 0; i < wordCount; i++) {
			byte[] word = ModbusUtils.toByteArray(data[i], reverseEndianess);

			stringBytes[j++] = word[1];
			stringBytes[j++] = word[0];
		}
		return new String(stringBytes);
	}

	/**
	 * Convert a short to a byte array
	 * 
	 * @param value Short
	 * @return byte array
	 */
	public static byte[] fromShort(Short value) {
		ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
		buffer.putShort(value);
		return buffer.array();
	}

	/**
	 * Convert an integer to a byte array
	 * 
	 * @param value Integer
	 * @return byte array
	 */
	public static byte[] fromInteger(Integer value) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(value);
		return buffer.array();
	}

	/**
	 * Convert a long to a byte array
	 * 
	 * @param value Long
	 * @return byte array
	 */
	public static byte[] fromLong(Long value) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(value);
		return buffer.array();
	}

	/**
	 * Convert a float to a byte array
	 * 
	 * @param value Float
	 * @return byte array
	 */
	public static byte[] fromFloat(Float value) {
		ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
		buffer.putFloat(value);
		return buffer.array();
	}

	/**
	 * Convert a double to a byte array
	 * 
	 * @param value Double
	 * @return byte array
	 */
	public static byte[] fromDouble(Double value) {
		ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
		buffer.putDouble(value);
		return buffer.array();
	}

	/**
	 * Construct a variant from its string
	 * 
	 * @param type  {@link ModbusDataType}
	 * @param value String value
	 * @return typed {@link ModbusVariant}
	 */
	public static ModbusVariant toVariant(ModbusDataType type, String value) {
		ModbusVariant dataValue = null;

		switch (type) {
		case BYTE_HIGH:
		case BYTE_LOW:
			dataValue = new ModbusVariant(type, (byte) Integer.parseInt(value, 16));
			break;

		case DISCRETE:
			dataValue = new ModbusVariant(Boolean.valueOf(value));
			break;

		case DOUBLE:
			dataValue = new ModbusVariant(type, Double.valueOf(value));
			break;

		case INT16:
			dataValue = new ModbusVariant(type, Short.valueOf(value));
			break;

		case UINT16:
		case INT32:
			dataValue = new ModbusVariant(type, Integer.valueOf(value));
			break;

		case UINT32:
		case INT64:
			dataValue = new ModbusVariant(type, Long.valueOf(value));
			break;

		case SINGLE:
			dataValue = new ModbusVariant(type, Float.valueOf(value));
			break;

		case STRING:
			dataValue = new ModbusVariant(value);
			break;

		default:
			break;
		}
		return dataValue;
	}

	/**
	 * Convert the BitVector to a boolean array
	 * 
	 * @param bv BitVector
	 * @return boolean array
	 */
	public static boolean[] bitVectorToBooleans(BitVector bv) {
		boolean[] bits = new boolean[bv.size()];

		for (int i = 0; i < bv.size(); i++) {
			bits[i] = bv.getBit(i);
		}
		return bits;
	}
}
