package org.point85.domain.http;

public class EquipmentStatusResponseDto {
	// material being run
	private MaterialDto material;

	// job
	private String job;

	// last availability reason
	private ReasonDto reason;

	// production units of measure
	private String runRateUOM;
	private String rejectUOM;

	public MaterialDto getMaterial() {
		return material;
	}

	public void setMaterial(MaterialDto materialDto) {
		this.material = materialDto;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public ReasonDto getReason() {
		return reason;
	}

	public void setReason(ReasonDto reason) {
		this.reason = reason;
	}

	public String getRunRateUOM() {
		return runRateUOM;
	}

	public void setRunRateUOM(String runRateUOM) {
		this.runRateUOM = runRateUOM;
	}

	public String getRejectUOM() {
		return rejectUOM;
	}

	public void setRejectUOM(String rejectUOM) {
		this.rejectUOM = rejectUOM;
	}
}
