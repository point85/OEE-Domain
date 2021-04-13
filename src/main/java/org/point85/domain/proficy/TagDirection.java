package org.point85.domain.proficy;

/**
 * An enumeration of the chronological search direction for tag values
 *
 */
public enum TagDirection {
	Forward(0), Backward(1);

	private int directionValue;

	private TagDirection(int direction) {
		this.directionValue = direction;
	}

	public int getDirection() {
		return directionValue;
	}

	/**
	 * Return the direction from the integer enumeration
	 * 
	 * @param direction Forward or backward
	 * @return TagDirection
	 */
	public static TagDirection fromInt(int direction) {
		TagDirection value = null;

		switch (direction) {
		case 0:
			value = Forward;
			break;
		case 1:
			value = Backward;
			break;
		default:
			break;
		}
		return value;
	}

	@Override
	public String toString() {
		return name();
	}
}
