package org.point85.domain.proficy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * A serialized String array
 *
 */
public class TagStringArray {
	@SerializedName("Values")
	private List<String> values;

	public List<String> getValues() {
		return values;
	}
}
