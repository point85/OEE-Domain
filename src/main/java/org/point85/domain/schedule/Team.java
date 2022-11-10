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
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.DomainUtils;
import org.point85.domain.dto.TeamDto;
import org.point85.domain.i18n.DomainLocalizer;

/**
 * Class Team is a named group of individuals who rotate through a shift
 * schedule.
 * 
 * @author Kent Randall
 *
 */
@Entity
@Table(name = "TEAM")
@AttributeOverride(name = "primaryKey", column = @Column(name = "TEAM_KEY"))
public class Team extends Named implements Comparable<Team> {
	// owning work schedule
	@ManyToOne
	@JoinColumn(name = "WS_KEY")
	private WorkSchedule workSchedule;

	// reference date for starting the rotations
	@Column(name = "ROTATION_START")
	private LocalDate rotationStart;

	// shift rotation days
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ROTATION_KEY")
	private Rotation rotation;

	/**
	 * Default constructor
	 */
	public Team() {
		super();
	}

	Team(String name, String description, Rotation rotation, LocalDate rotationStart) {
		super(name, description);
		this.rotation = rotation;
		this.rotationStart = rotationStart;
	}

	public Team(TeamDto dto) {
		super(dto.getName(), dto.getDescription());
		this.rotation = new Rotation(dto.getRotation(), null);
		this.rotationStart = DomainUtils.localDateFromString(dto.getRotationStart(), DomainUtils.LOCAL_DATE_8601);
	}

	/**
	 * Get rotation start
	 * 
	 * @return Rotation start date
	 */
	public LocalDate getRotationStart() {
		return rotationStart;
	}

	/**
	 * Get rotation start
	 * 
	 * @param rotationStart Starting date of rotation
	 */
	public void setRotationStart(LocalDate rotationStart) {
		this.rotationStart = rotationStart;
	}

	private long getDayFrom() {
		return rotationStart.toEpochDay();
	}

	/**
	 * Get the shift rotation for this team
	 * 
	 * @return {@link Rotation}
	 */
	public Rotation getRotation() {
		return rotation;
	}

	/**
	 * Set the shift rotation for this team
	 * 
	 * @param rotation {@link Rotation}
	 */
	public void setRotation(Rotation rotation) {
		this.rotation = rotation;
	}

	/**
	 * Get the duration of the shift rotation
	 * 
	 * @return Duration
	 * @throws Exception exception
	 */
	public Duration getRotationDuration() throws Exception {
		return getRotation().getDuration();
	}

	/**
	 * Get the shift rotation's working time as a percentage of the rotation
	 * duration
	 * 
	 * @return Percentage
	 * @throws Exception exception
	 */
	public float getPercentageWorked() throws Exception {
		return ((float) getRotation().getWorkingTime().getSeconds()) / ((float) getRotationDuration().getSeconds())
				* 100.0f;
	}

	/**
	 * Get the average number of hours worked each week by this team
	 * 
	 * @return Duration of hours worked per week
	 */
	public Duration getHoursWorkedPerWeek() {
		if (getRotation() == null) {
			return Duration.ZERO;
		}

		float days = getRotation().getDuration().toDays();
		float secPerWeek = getRotation().getWorkingTime().getSeconds() * (7.0f / days);
		return Duration.ofSeconds((long) secPerWeek);
	}

	/**
	 * Get the day number in the rotation for this local date
	 * 
	 * @param date LocalDate
	 * @return day number in the rotation, starting at 1
	 * @throws Exception exception
	 */
	public int getDayInRotation(LocalDate date) throws Exception {
		// calculate total number of days from start of rotation
		long dayTo = date.toEpochDay();
		long deltaDays = dayTo - getDayFrom();

		if (deltaDays < 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("end.earlier.than.start", rotationStart, date,
					getName()));
		}

		return (int) (deltaDays % getRotation().getDuration().toDays()) + 1;
	}

	/**
	 * Get the {@link ShiftInstance} for the specified day
	 * 
	 * @param day Day with a shift instance
	 * @return {@link ShiftInstance}
	 * @throws Exception exception
	 */
	public ShiftInstance getShiftInstanceForDay(LocalDate day) throws Exception {
		ShiftInstance instance = null;

		Rotation shiftRotation = getRotation();

		if (shiftRotation.getDuration().equals(Duration.ZERO)) {
			// no instance for that day
			return instance;
		}

		int dayInRotation = getDayInRotation(day);

		// shift or off shift
		TimePeriod period = shiftRotation.getPeriods().get(dayInRotation - 1);

		if (period.isWorkingPeriod()) {
			LocalDateTime startDateTime = LocalDateTime.of(day, period.getStart());
			instance = new ShiftInstance((Shift) period, startDateTime, this);
		}

		return instance;
	}

	/**
	 * Check to see if this day is a day off
	 * 
	 * @param day Date to check
	 * @return True if a day off
	 * @throws Exception Exception
	 */
	public boolean isDayOff(LocalDate day) throws Exception {

		boolean dayOff = false;

		Rotation shiftRotation = getRotation();
		int dayInRotation = getDayInRotation(day);

		// shift or off shift
		TimePeriod period = shiftRotation.getPeriods().get(dayInRotation - 1);

		if (!period.isWorkingPeriod()) {
			dayOff = true;
		}

		return dayOff;

	}

	/**
	 * Calculate the schedule working time between the specified dates and times
	 * 
	 * @param from Starting date and time of day
	 * @param to   Ending date and time of day
	 * @return Duration of working time
	 * @throws Exception exception
	 */
	public Duration calculateWorkingTime(LocalDateTime from, LocalDateTime to) throws Exception {
		if (from.isAfter(to)) {
			throw new Exception(
					DomainLocalizer.instance().getErrorString("end.earlier.than.start", to, from, getName()));
		}

		Duration sum = Duration.ZERO;

		LocalDate thisDate = from.toLocalDate();
		LocalTime thisTime = from.toLocalTime();
		LocalDate toDate = to.toLocalDate();
		LocalTime toTime = to.toLocalTime();
		int dayCount = getRotation().getDayCount();

		// get the working shift from yesterday
		Shift lastShift = null;

		LocalDate yesterday = thisDate.plusDays(-1);
		ShiftInstance yesterdayInstance = getShiftInstanceForDay(yesterday);

		if (yesterdayInstance != null) {
			lastShift = yesterdayInstance.getShift();
		}

		// step through each day until done
		while (thisDate.compareTo(toDate) < 1) {
			if (lastShift != null && lastShift.spansMidnight()) {
				// check for days in the middle of the time period
				boolean lastDay = thisDate.compareTo(toDate) == 0;

				if (!lastDay || (lastDay && !toTime.equals(LocalTime.MIDNIGHT))) {
					// add time after midnight in this day
					int afterMidnightSecond = lastShift.getEnd().toSecondOfDay();
					int fromSecond = thisTime.toSecondOfDay();

					if (afterMidnightSecond > fromSecond) {
						sum = sum.plusSeconds((long) afterMidnightSecond - (long) fromSecond);
					}
				}
			}

			// today's shift
			ShiftInstance instance = getShiftInstanceForDay(thisDate);

			Duration duration = null;

			if (instance != null) {
				lastShift = instance.getShift();
				// check for last date
				if (thisDate.compareTo(toDate) == 0) {
					duration = lastShift.calculateWorkingTime(thisTime, toTime, true);
				} else {
					duration = lastShift.calculateWorkingTime(thisTime, LocalTime.MAX, true);
				}
				sum = sum.plus(duration);
			} else {
				lastShift = null;
			}

			int n = 1;
			if (getDayInRotation(thisDate) == dayCount) {
				// move ahead by the rotation count if possible
				LocalDate rotationEndDate = thisDate.plusDays(dayCount);

				if (rotationEndDate.compareTo(toDate) < 0) {
					n = dayCount;
					sum = sum.plus(getRotation().getWorkingTime());
				}
			}

			// move ahead n days starting at midnight
			thisDate = thisDate.plusDays(n);
			thisTime = LocalTime.MIDNIGHT;
		} // end day loop

		return sum;
	}

	/**
	 * Get the work schedule that owns this team
	 * 
	 * @return {@link WorkSchedule}
	 */
	public WorkSchedule getWorkSchedule() {
		return workSchedule;
	}

	public void setWorkSchedule(WorkSchedule workSchedule) {
		this.workSchedule = workSchedule;
	}

	/**
	 * Compare one team to another
	 */
	@Override
	public int compareTo(Team other) {
		return this.getName().compareTo(other.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Team) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getWorkSchedule());
	}

	/**
	 * Build a string value for this team
	 */
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);

		String text = "";
		try {
			text = super.toString() + ", Rotation start: " + getRotationStart() + ", " + getRotation()
					+ ", Percentage worked: " + df.format(getPercentageWorked()) + "%"
					+ ", Average hours worked per week: " + getHoursWorkedPerWeek();

		} catch (Exception e) {
			// ignore
		}

		return text;
	}
}
