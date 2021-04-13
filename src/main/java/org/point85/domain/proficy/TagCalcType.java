package org.point85.domain.proficy;

/**
 * Enumerated value for the tag calculation type
 *
 */
public enum TagCalcType {
	ihRawTag(0), ihAnalyticTag(1), ihPythonExprTag(2);

	private int intValue;

	private TagCalcType(int type) {
		this.intValue = type;
	}

	/**
	 * Return the calculation type from the integer enumeration
	 * 
	 * @param value integer value
	 * @return TagCalcType
	 */
	public static TagCalcType fromInt(int value) {
		TagCalcType calcType = null;

		switch (value) {
		case 0:
			calcType = ihRawTag;
			break;
		case 1:
			calcType = ihAnalyticTag;
			break;
		case 2:
			calcType = ihPythonExprTag;
			break;
		default:
			break;
		}

		return calcType;
	}

	public int getIntValue() {
		return intValue;
	}

}
