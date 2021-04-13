package org.point85.domain.proficy;

/**
 * An enumeration of tag data quality
 *
 */
public enum TagQuality {
	Bad(0),Uncertain(1),NA(2),Good(3);
	
	private int qualityValue;

	private TagQuality(int value) {
		this.qualityValue = value;
	}

	public int getQuality() {
		return qualityValue;
	}
	
	/**
	 * Return the quality from the integer enumeration
	 * 
	 * @param value integer value
	 * @return TagQuality
	 */
	public static TagQuality fromInt(int value) {
		TagQuality quality = null;

		switch (value) {
		case 0:
			quality = Bad;
			break;
		case 1:
			quality = Uncertain;
			break;
		case 2:
			quality = NA;
			break;
		case 3:
			quality = Good;
			break;
		default:
			break;
		}

		return quality;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
