package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.schedule.ExceptionPeriod;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;

public class WorkScheduleDto extends NamedObjectDto {
	private List<TeamDto> teams = new ArrayList<>();

	private List<ShiftDto> shifts = new ArrayList<>();

	private List<ExceptionPeriodDto> exceptionPeriods = new ArrayList<>();

	private List<RotationDto> rotations = new ArrayList<>();

	public WorkScheduleDto(WorkSchedule schedule) {
		super(schedule);

		// teams
		for (Team team : schedule.getTeams()) {
			teams.add(new TeamDto(team));
		}

		// shifts
		for (Shift shift : schedule.getShifts()) {
			shifts.add(new ShiftDto(shift));
		}

		// exception periods
		for (ExceptionPeriod period : schedule.getExceptionPeriods()) {
			exceptionPeriods.add(new ExceptionPeriodDto(period));
		}

		// rotations
		for (Rotation rotation : schedule.getRotations()) {
			rotations.add(new RotationDto(rotation));
		}
	}

	public List<TeamDto> getTeams() {
		return teams;
	}

	public List<ShiftDto> getShifts() {
		return shifts;
	}

	public List<ExceptionPeriodDto> getExceptionPeriods() {
		return exceptionPeriods;
	}

	public List<RotationDto> getRotations() {
		return rotations;
	}

	public void setRotations(List<RotationDto> rotations) {
		this.rotations = rotations;
	}

}
