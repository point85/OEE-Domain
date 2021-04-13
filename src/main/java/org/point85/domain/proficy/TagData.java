package org.point85.domain.proficy;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * TagData contains the data for tag samples
 *
 */
public class TagData {
	@SerializedName(value = "TagName")
	private String tagName;

	@SerializedName(value = "ErrorCode")
	private Integer errorCode;

	@SerializedName(value = "DataType")
	private String dataType;

	@SerializedName(value = "Samples")
	private List<TagSample> samples;

	public TagData(String tagName) {
		this.tagName = tagName;
		this.samples = new ArrayList<>();
	}

	public String getTagName() {
		return tagName;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public List<TagSample> getSamples() {
		return samples;
	}

	public ErrorCode getEnumeratedError() {
		return ErrorCode.fromInt(errorCode);
	}

	public String getDataType() {
		return dataType;
	}

	public TagDataType getEnumeratedType() {
		return TagDataType.fromString(dataType);
	}
}
