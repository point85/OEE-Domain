package org.point85.domain.proficy;

import com.google.gson.annotations.SerializedName;

/**
 * Serialized properties about a Proficy error
 *
 */
public class TagError {
	@SerializedName(value = "ErrorCode")
	private Integer errorCode;

	@SerializedName(value = "ErrorMessage")
	private String errorMessage;

	public Integer getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public ErrorCode getEnumeratedError() {
		return ErrorCode.fromInt(errorCode);
	}
}
