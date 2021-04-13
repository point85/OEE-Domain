package org.point85.domain.proficy;

/**
 * Enumerated value for the tag collection type
 *
 */
public enum TagCollectionType {
	ihUnsolicited(1), ihPolled(2);

	private int intValue;

	private TagCollectionType(int type) {
		this.intValue = type;
	}

	/**
	 * Return the collection type from the integer enumeration
	 * 
	 * @param value integer value
	 * @return TagCollectionType
	 */
	public static TagCollectionType fromInt(int value) {
		TagCollectionType collectionType = null;

		switch (value) {
		case 1:
			collectionType = ihUnsolicited;
			break;
		case 2:
			collectionType = ihPolled;
			break;
		default:
			break;
		}

		return collectionType;
	}

	public int getIntValue() {
		return intValue;
	}
}
