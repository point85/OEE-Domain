package org.point85.domain.dto;

import org.point85.domain.DomainUtils;
import org.point85.domain.schedule.Team;

public class TeamDto extends NamedObjectDto {
	// LocalDate start rotation
	private String rotationStart;

	// rotation
	private String rotation;

	public TeamDto(Team team) {
		super(team);
		this.rotationStart = DomainUtils.localDateToString(team.getRotationStart(), DomainUtils.LOCAL_DATE_8601);
		this.rotation = team.getRotation().getName();
	}

	public String getRotationStart() {
		return rotationStart;
	}

	public void setRotationStart(String rotationStart) {
		this.rotationStart = rotationStart;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

}
