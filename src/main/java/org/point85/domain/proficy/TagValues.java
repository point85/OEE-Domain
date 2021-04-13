package org.point85.domain.proficy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * A serialized list of tag data
 *
 */
public class TagValues extends TagError {
	@SerializedName(value = "Data")
	private List<TagData> tagData;

	public List<TagData> getTagData() {
		return tagData;
	}
}
