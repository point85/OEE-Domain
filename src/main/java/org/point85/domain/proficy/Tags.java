package org.point85.domain.proficy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Serialized list of tag names
 *
 */
public class Tags extends TagError {
	@SerializedName(value = "Tags")
	private List<String> tagNames;

	public List<String> getTags() {
		return tagNames;
	}
}
