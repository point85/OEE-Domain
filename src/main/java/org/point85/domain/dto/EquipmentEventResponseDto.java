package org.point85.domain.dto;

/**
 * Data Transfer Object (DTO) for an equipment event HTTP response
 */
public class EquipmentEventResponseDto {
	private static final String OK_STATUS = "OK";
	private static final String ERROR_STATUS = "ERROR";

	private String status = OK_STATUS;
	private String errorText;
	private EquipmentEventRequestDto requestDto;

	public EquipmentEventResponseDto() {
		this.errorText = OK_STATUS;
	}

	public EquipmentEventResponseDto(String errorText) {
		this.errorText = errorText;
	}
	
	public EquipmentEventResponseDto(EquipmentEventRequestDto requestDto) {
		this.setRequestDto(requestDto);
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
		return status.equals(OK_STATUS);
	}

	public boolean isError() {
		return !isOK();
	}

	public EquipmentEventRequestDto getRequestDto() {
		return requestDto;
	}

	public void setRequestDto(EquipmentEventRequestDto requestDto) {
		this.requestDto = requestDto;
	}

}
