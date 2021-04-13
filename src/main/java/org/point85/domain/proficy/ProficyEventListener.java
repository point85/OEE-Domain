package org.point85.domain.proficy;

/**
 * Listener for Proficy tag value change
 *
 */
public interface ProficyEventListener {
	/**
	 * Callback for a tag value change
	 * 
	 * @param tagData {@link TagData}
	 */
	void onProficyEvent(TagData tagData);
}
