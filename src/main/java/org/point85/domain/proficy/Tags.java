package org.point85.domain.proficy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Tags {
	@SerializedName(value = "ErrorCode")
	private Integer errorCode;

	@SerializedName(value = "ErrorMessage")
	private String errorMessage;

	@SerializedName(value = "Tags")
	private List<String> tags;

	public String getErrorMessage() {
		return errorMessage;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public List<String> getTags() {
		return tags;
	}

}
