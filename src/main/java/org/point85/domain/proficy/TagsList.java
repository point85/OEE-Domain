package org.point85.domain.proficy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * A serialized list of properties of tags
 *
 */
public class TagsList {
	@SerializedName(value = "TotalCount")
	private Integer totalCount;

	@SerializedName(value = "Page")
	private Integer page;

	@SerializedName(value = "PageSize")
	private Integer pageSize;

	@SerializedName(value = "Tags")
	private List<TagDetail> tags;

	public Integer getTotalCount() {
		return totalCount;
	}

	public Integer getPage() {
		return page;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public List<TagDetail> getTagDetails() {
		return tags;
	}
}
