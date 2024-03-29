/*
MIT License

Copyright (c) 2016 Kent Randall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.point85.domain.schedule;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.point85.domain.dto.BreakDto;
import org.point85.domain.dto.ExceptionPeriodDto;
import org.point85.domain.dto.RotationDto;
import org.point85.domain.dto.ShiftDto;
import org.point85.domain.dto.TeamDto;
import org.point85.domain.dto.WorkScheduleDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.plant.NamedObject;

/**
 * Class WorkSchedule represents a named group of teams who collectively work
 * one or more shifts with off-shift periods. A work schedule can have periods
 * of non-working time.
 * 
 * @author Kent Randall
 *
 */
@Entity
@Table(name = "WORK_SCHEDULE")
@AttributeOverride(name = "primaryKey", column = @Column(name = "WS_KEY"))

public class WorkSchedule extends NamedObject {
	// cached UTC time zone for working time calculations
	private static final ZoneId ZONE_ID = ZoneId.of("Z");

	// list of teams
	@OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<Team> teams = new ArrayList<>();

	// list of shifts
	@OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<Shift> shifts = new ArrayList<>();

	// holidays and planned downtime. Unscheduled overtime is also included.
	@OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<ExceptionPeriod> exceptionPeriods = new ArrayList<>();

	// list of rotations
	@OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<Rotation> rotations = new ArrayList<>();

	public WorkSchedule() {
		super();
	}

	/**
	 * Construct a work schedule
	 * 
	 * @param name        Schedule name
	 * @param description Schedule description
	 * @throws Exception exception
	 */
	public WorkSchedule(String name, String description) throws Exception {
		super(name, description);
	}

	public WorkSchedule(WorkScheduleDto dto) throws Exception {
		super(dto.getName(), dto.getDescription());

		// teams
		for (TeamDto teamDto : dto.getTeams()) {
			Team team = new Team(teamDto);

			getTeams().add(team);
			team.setWorkSchedule(this);
		}

		// shifts
		for (ShiftDto shiftDto : dto.getShifts()) {
			Shift shift = new Shift(shiftDto);

			getShifts().add(shift);
			shift.setWorkSchedule(this);

			// breaks
			if (shiftDto.getBreaks() != null) {
				for (BreakDto breakDto : shiftDto.getBreaks()) {
					Break period = new Break(breakDto);
					period.setShift(shift);
					shift.addBreak(period);
				}
			}
		}

		// exception periods
		for (ExceptionPeriodDto periodDto : dto.getExceptionPeriods()) {
			ExceptionPeriod period = new ExceptionPeriod(periodDto);

			getExceptionPeriods().add(period);
			period.setWorkSchedule(this);
		}

		// rotations
		for (RotationDto rotationDto : dto.getRotations()) {
			Rotation rotation = new Rotation(rotationDto);

			getRotations().add(rotation);
			rotation.setWorkSchedule(this);
		}
	}

	/**
	 * Remove this team from the schedule
	 * 
	 * @param team {@link Team}
	 */
	public void deleteTeam(Team team) {
		if (teams.contains(team)) {
			teams.remove(team);
		}
	}

	/**
	 * Get all teams
	 * 
	 * @return List of {@link Team}
	 */
	public List<Team> getTeams() {
		return this.teams;
	}

	/**
	 * Remove a non-working or overtime period from the schedule
	 * 
	 * @param period {@link ExceptionPeriod}
	 */
	public void deleteExceptionPeriod(ExceptionPeriod period) {
		if (this.exceptionPeriods.contains(period)) {
			this.exceptionPeriods.remove(period);
		}
	}

	/**
	 * Get non-working periods in the schedule, i.e. where the loss category is not
	 * NO LOSS
	 * 
	 * @return List of {@link ExceptionPeriod}
	 */
	public List<ExceptionPeriod> getNonWorkingPeriods() {
		List<ExceptionPeriod> periods = new ArrayList<>();

		for (ExceptionPeriod period : exceptionPeriods) {
			if (!period.isWorkingPeriod()) {
				periods.add(period);
			}
		}
		return periods;
	}

	/**
	 * Get periods in the schedule where the loss category is NO LOSS
	 * 
	 * @return List of {@link ExceptionPeriod}
	 */
	public List<ExceptionPeriod> getOvertimePeriods() {
		List<ExceptionPeriod> periods = new ArrayList<>();

		for (ExceptionPeriod period : exceptionPeriods) {
			if (period.isWorkingPeriod()) {
				periods.add(period);
			}
		}
		return periods;
	}

	/**
	 * Get all exception periods in the schedule
	 * 
	 * @return List of {@link ExceptionPeriod}
	 */
	public List<ExceptionPeriod> getExceptionPeriods() {
		return this.exceptionPeriods;
	}

	/**
	 * Get the list of shift instances for the specified date that start in that
	 * date
	 * 
	 * @param day LocalDate
	 * @return List of {@link ShiftInstance}
	 * @throws Exception exception
	 */
	public List<ShiftInstance> getShiftInstancesForDay(LocalDate day) throws Exception {
		List<ShiftInstance> workingShifts = new ArrayList<>();

		// for each team see if there is a working shift
		for (Team team : teams) {
			ShiftInstance instance = team.getShiftInstanceForDay(day);

			if (instance == null) {
				continue;
			}

			// check to see if this is a non-working day
			boolean addShift = true;

			LocalDate startDate = instance.getStartTime().toLocalDate();

			for (ExceptionPeriod nonWorkingPeriod : getNonWorkingPeriods()) {
				if (nonWorkingPeriod.isInPeriod(startDate)) {
					addShift = false;
					break;
				}
			}

			if (addShift) {
				workingShifts.add(instance);
			}
		}

		Collections.sort(workingShifts);

		return workingShifts;
	}

	/**
	 * Get the list of shift instances for the specified date that start in that
	 * date or cross over from midnight the previous day
	 * 
	 * @param day LocalDate
	 * @return List of {@link ShiftInstance}
	 * @throws Exception exception
	 */
	public List<ShiftInstance> getAllShiftInstancesForDay(LocalDate day) throws Exception {
		// starting in this day
		List<ShiftInstance> workingShifts = getShiftInstancesForDay(day);

		// now check previous day
		LocalDate yesterday = day.minusDays(1);

		for (ShiftInstance instance : getShiftInstancesForDay(yesterday)) {
			if (instance.getEndTime().toLocalDate().equals(day)) {
				// shift ends in this day
				workingShifts.add(instance);
			}
		}

		Collections.sort(workingShifts);

		return workingShifts;
	}

	/**
	 * Get the list of shift instances for the specified date and time of day
	 * 
	 * @param dateTime Date and time of day
	 * @return List of {@link ShiftInstance}
	 * @throws Exception exception
	 */
	public List<ShiftInstance> getShiftInstancesForTime(LocalDateTime dateTime) throws Exception {
		List<ShiftInstance> workingShifts = new ArrayList<>();

		// shifts from this date and yesterday
		List<ShiftInstance> candidateShifts = getAllShiftInstancesForDay(dateTime.toLocalDate());

		for (ShiftInstance instance : candidateShifts) {
			if (instance.isInShiftInstance(dateTime)) {
				workingShifts.add(instance);
			}
		}

		Collections.sort(workingShifts);

		return workingShifts;
	}

	/**
	 * Create a team
	 * 
	 * @param name          Name of team
	 * @param description   Team description
	 * @param rotation      Shift rotation
	 * @param rotationStart Start of rotation
	 * @return {@link Team}
	 * @throws Exception exception
	 */
	public Team createTeam(String name, String description, Rotation rotation, LocalDate rotationStart)
			throws Exception {
		Team team = new Team(name, description, rotation, rotationStart);

		if (teams.contains(team)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("team.already.exists", name));
		}

		teams.add(team);
		team.setWorkSchedule(this);
		return team;
	}

	/**
	 * Create a rotation
	 * 
	 * @param name        Name of rotation
	 * @param description Description of rotation
	 * @return {@link Rotation}
	 * @throws Exception exception
	 */
	public Rotation createRotation(String name, String description) throws Exception {
		Rotation rotation = new Rotation(name, description);

		if (rotations.contains(rotation)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("rotation.already.exists", name));
		}

		rotations.add(rotation);
		rotation.setWorkSchedule(this);
		return rotation;
	}

	/**
	 * Create a shift
	 * 
	 * @param name        Name of shift
	 * @param description Description of shift
	 * @param start       Shift start time of day
	 * @param duration    Shift duration
	 * @return {@link Shift}
	 * @throws Exception exception
	 */
	public Shift createShift(String name, String description, LocalTime start, Duration duration) throws Exception {
		Shift shift = new Shift(name, description, start, duration);

		if (shifts.contains(shift)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("shift.already.exists", name));
		}
		shifts.add(shift);
		shift.setWorkSchedule(this);
		return shift;
	}

	/**
	 * Delete this shift
	 * 
	 * @param shift {@link Shift} to delete
	 * @throws Exception exception
	 */
	public void deleteShift(Shift shift) throws Exception {
		if (!shifts.contains(shift)) {
			return;
		}

		// can't be in use
		for (Shift inUseShift : shifts) {
			for (Team team : teams) {
				Rotation rotation = team.getRotation();

				for (TimePeriod period : rotation.getPeriods()) {
					if (period.equals(inUseShift)) {
						throw new Exception(DomainLocalizer.instance().getErrorString("shift.in.use", shift.getName()));
					}
				}
			}
		}

		// remove breaks
		shift.getBreaks().clear();

		shifts.remove(shift);
	}

	/**
	 * Create a non-working or overtime period of time
	 * 
	 * @param name          Name of period
	 * @param description   Description of period
	 * @param startDateTime Starting date and time of day
	 * @param duration      Duration of period
	 * @param loss          Time loss category
	 * @return {@link ExceptionPeriod}
	 * @throws Exception exception
	 */
	public ExceptionPeriod createExceptionPeriod(String name, String description, LocalDateTime startDateTime,
			Duration duration, TimeLoss loss) throws Exception {
		ExceptionPeriod period = new ExceptionPeriod(name, description, startDateTime, duration, loss);

		if (exceptionPeriods.contains(period)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("nonworking.period.already.exists", name));
		}
		period.setWorkSchedule(this);
		exceptionPeriods.add(period);

		Collections.sort(exceptionPeriods);

		return period;
	}

	/**
	 * Get total duration of rotation across all teams.
	 * 
	 * @return Duration of rotation
	 * @throws Exception Exception
	 */
	public Duration getRotationDuration() throws Exception {
		Duration sum = Duration.ZERO;

		for (Team team : teams) {
			sum = sum.plus(team.getRotationDuration());
		}
		return sum;
	}

	/**
	 * Get the total working time for all team rotations
	 * 
	 * @return Team rotation working time
	 */
	public Duration getRotationWorkingTime() {
		Duration sum = Duration.ZERO;

		for (Team team : teams) {
			sum = sum.plus(team.getRotation().getWorkingTime());
		}
		return sum;
	}

	/**
	 * Calculate the scheduled working time between the specified dates and times of
	 * day. Non-working periods are removed.
	 * 
	 * @param from Starting date and time
	 * @param to   Ending date and time
	 * @return Working time duration
	 * @throws Exception exception
	 */
	public Duration calculateWorkingTime(LocalDateTime from, LocalDateTime to) throws Exception {
		Duration sum = Duration.ZERO;

		// now add up scheduled time by team
		for (Team team : getTeams()) {
			sum = sum.plus(team.calculateWorkingTime(from, to));
		}

		// remove the non-working time from exception periods, e.g. holidays
		Duration nonWorking = calculateNonWorkingTime(from, to);
		sum = sum.minus(nonWorking);

		// add overtime from exception periods
		Duration overTime = calculateOvertime(from, to);
		sum = sum.plus(overTime);

		// clip if negative
		if (sum.isNegative()) {
			sum = Duration.ZERO;
		}
		return sum;
	}

	/**
	 * Calculate the non-working time between the specified dates and times of day.
	 * 
	 * @param from Starting date and time
	 * @param to   Ending date and time
	 * @return Non-working time duration
	 * @throws Exception exception
	 */
	public Duration calculateNonWorkingTime(LocalDateTime from, LocalDateTime to) throws Exception {
		return calculateExceptionTime(from, to, true);
	}

	/**
	 * Calculate the overtime between the specified dates and times of day.
	 * 
	 * @param from Starting date and time
	 * @param to   Ending date and time
	 * @return Overtime duration
	 * @throws Exception exception
	 */
	public Duration calculateOvertime(LocalDateTime from, LocalDateTime to) throws Exception {
		return calculateExceptionTime(from, to, false);
	}

	private Duration calculateExceptionTime(LocalDateTime from, LocalDateTime to, boolean isNonWorking)
			throws Exception {
		Duration sum = Duration.ZERO;

		long fromSeconds = from.atZone(ZONE_ID).toEpochSecond();
		long toSeconds = to.atZone(ZONE_ID).toEpochSecond();

		List<ExceptionPeriod> periods = isNonWorking ? getNonWorkingPeriods() : getOvertimePeriods();

		for (ExceptionPeriod period : periods) {
			LocalDateTime start = period.getStartDateTime();
			long startSeconds = start.atZone(ZONE_ID).toEpochSecond();

			LocalDateTime end = period.getEndDateTime();
			long endSeconds = end.atZone(ZONE_ID).toEpochSecond();

			if (fromSeconds >= endSeconds) {
				// look at next period
				continue;
			}

			if (toSeconds <= startSeconds) {
				// done with periods
				break;
			}

			// found a period, check edge conditions
			if (fromSeconds > startSeconds) {
				startSeconds = fromSeconds;
			}

			if (toSeconds < endSeconds) {
				endSeconds = toSeconds;
			}

			sum = sum.plusSeconds(endSeconds - startSeconds);

			if (toSeconds <= endSeconds) {
				break;
			}
		}

		return sum;
	}

	/**
	 * Get the list of shifts in this schedule
	 * 
	 * @return List of {@link Shift}
	 */
	public List<Shift> getShifts() {
		return shifts;
	}

	/**
	 * Get the list of rotations in this schedule
	 * 
	 * @return List of {@link Rotation}
	 */
	public List<Rotation> getRotations() {
		return rotations;
	}

	/**
	 * Print shift instances
	 * 
	 * @param start Starting date
	 * @param end   Ending date
	 * @throws Exception exception
	 */
	public void printShiftInstances(LocalDate start, LocalDate end) throws Exception {
		if (start.isAfter(end)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("print.end.earlier.than.start", start, end));
		}

		long days = end.toEpochDay() - start.toEpochDay() + 1;

		LocalDate day = start;

		System.out.println(DomainLocalizer.instance().getLangString("shifts.working"));
		for (long i = 0; i < days; i++) {
			System.out.println(
					"[" + (i + 1) + "] " + DomainLocalizer.instance().getLangString("shifts.day") + ": " + day);

			List<ShiftInstance> instances = getShiftInstancesForDay(day);

			if (instances.isEmpty()) {
				System.out.println("   " + DomainLocalizer.instance().getLangString("shifts.non.working"));
			} else {
				int count = 1;
				for (ShiftInstance instance : instances) {
					System.out.println("   (" + count + ")" + instance);
					count++;
				}
			}
			day = day.plusDays(1);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WorkSchedule) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getDescription());
	}

	/**
	 * Build a string value for the work schedule
	 * 
	 * @return String
	 */
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);

		String text = "Schedule: " + super.toString();
		try {
			text += "\nRotation duration: " + getRotationDuration() + ", Scheduled working time: "
					+ getRotationWorkingTime();

			// shifts
			text += "\nShifts: ";
			int count = 1;
			for (Shift shift : getShifts()) {
				text += "\n   (" + count + ") " + shift;
				count++;
			}

			// teams
			text += "\nTeams: ";
			count = 1;
			float teamPercent = 0.0f;
			for (Team team : this.getTeams()) {
				text += "\n   (" + count + ") " + team;
				teamPercent += team.getPercentageWorked();
				count++;
			}
			text += "\nTotal team coverage: " + df.format(teamPercent) + "%";

			// non-working periods
			List<ExceptionPeriod> periods = getNonWorkingPeriods();

			if (!periods.isEmpty()) {
				text += "\nNon-working periods:";

				Duration totalMinutes = Duration.ZERO;

				count = 1;
				for (ExceptionPeriod period : periods) {
					totalMinutes = totalMinutes.plusMinutes(period.getDuration().toMinutes());
					text += "\n   (" + count + ") " + period;
					count++;
				}
				text += "\nTotal non-working time: " + totalMinutes;
			}

			// overtime periods
			periods = getOvertimePeriods();

			if (!periods.isEmpty()) {
				text += "\nOvertime periods:";

				Duration totalMinutes = Duration.ZERO;

				count = 1;
				for (ExceptionPeriod period : periods) {
					totalMinutes = totalMinutes.plusMinutes(period.getDuration().toMinutes());
					text += "\n   (" + count + ") " + period;
					count++;
				}
				text += "\nTotal overtime: " + totalMinutes;
			}
		} catch (Exception e) {
			// ignore
		}

		return text;
	}
}
