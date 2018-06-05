package org.point85.domain.http;

public class EquipmentEventResponseDto {
	private static final String OK_STATUS = "OK";
	private static final String ERROR_STATUS = "ERROR";

	private String status = OK_STATUS;
	private String errorText;

	public EquipmentEventResponseDto() {
		this.errorText = OK_STATUS;
	}

	public EquipmentEventResponseDto(String errorText) {
		this.errorText = errorText;
	}

	public String getErrorText() {
		return errorText;
	}

	public void setErrorText(String errorText) {
		this.errorText = errorText;
		setStatus(ERROR_STATUS);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isOK() {
		return status.equals(OK_STATUS) ? true : false;
	}

	public boolean isError() {
		return !isOK();
	}

}
