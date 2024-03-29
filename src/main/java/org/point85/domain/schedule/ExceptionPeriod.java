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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.point85.domain.DomainUtils;
import org.point85.domain.dto.ExceptionPeriodDto;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.TimeLossConverter;

/**
 * Class NonWorkingPeriod represents named non-working, non-recurring periods.
 * For example holidays and scheduled outages such as for preventive
 * maintenance.
 * 
 * @author Kent Randall
 *
 */
@Entity
@Table(name = "NON_WORKING_PERIOD")
@AttributeOverride(name = "primaryKey", column = @Column(name = "PERIOD_KEY"))
public class ExceptionPeriod extends Named implements Comparable<ExceptionPeriod> {
	// owning work schedule
	@ManyToOne
	@JoinColumn(name = "WS_KEY")
	private WorkSchedule workSchedule;

	// starting date and time of day
	@Column(name = "START_DATE_TIME")
	private LocalDateTime startDateTime;

	// duration of period
	@Column(name = "DURATION")
	private Duration duration;

	// loss category (can be value adding)
	@Column(name = "LOSS")
	@Convert(converter = TimeLossConverter.class)
	private TimeLoss timeLoss;

	/**
	 * Default constructor
	 */
	public ExceptionPeriod() {
		super();
	}

	ExceptionPeriod(String name, String description, LocalDateTime startDateTime, Duration duration, TimeLoss loss) {
		super(name, description);
		this.startDateTime = startDateTime;
		this.duration = duration;
		this.timeLoss = loss;
	}

	public ExceptionPeriod(ExceptionPeriodDto dto) {
		super(dto.getName(), dto.getDescription());

		this.startDateTime = DomainUtils.localDateTimeFromString(dto.getStartDateTime(),
				DomainUtils.LOCAL_DATE_TIME_8601);
		this.duration = Duration.ofSeconds(dto.getDuration());
		this.timeLoss = dto.getTimeLoss() != null ? TimeLoss.valueOf(dto.getTimeLoss()) : null;
	}

	/**
	 * Get period start date and time
	 * 
	 * @return Start date and time
	 */
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	/**
	 * Set period start date and time
	 * 
	 * @param startDateTime Period start
	 * @throws Exception exception
	 */
	public void setStartDateTime(LocalDateTime startDateTime) throws Exception {
		if (startDateTime == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("start.not.defined"));
		}

		this.startDateTime = startDateTime;
	}

	/**
	 * Get period end date and time
	 * 
	 * @return Period end
	 * @throws Exception exception
	 */
	public LocalDateTime getEndDateTime() throws Exception {
		return startDateTime.plus(duration);
	}

	/**
	 * Get period duration
	 * 
	 * @return Duration
	 */
	public Duration getDuration() {
		return duration;
	}

	/**
	 * Set duration
	 * 
	 * @param duration Duration
	 * @throws Exception exception
	 */
	public void setDuration(Duration duration) throws Exception {
		if (duration == null || duration.getSeconds() == 0) {
			throw new Exception(DomainLocalizer.instance().getErrorString("duration.not.defined"));
		}

		this.duration = duration;
	}

	/**
	 * Compare this non-working period to another such period by start date and time
	 * of day
	 * 
	 * @param other {@link ExceptionPeriod}
	 * @return negative if less than, 0 if equal and positive if greater than
	 */
	@Override
	public int compareTo(ExceptionPeriod other) {
		return getStartDateTime().compareTo(other.getStartDateTime());
	}

	/**
	 * Get the work schedule that owns this non-working period
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
	 * Check to see if this day is contained in the non-working period
	 * 
	 * @param day Date to check
	 * @return True if in the non-working period
	 * @throws Exception Exception
	 */
	public boolean isInPeriod(LocalDate day) throws Exception {
		boolean isInPeriod = false;

		Duration span = Duration.between(getStartDateTime(), getEndDateTime());

		if (span.compareTo(Duration.ofHours(24l)) >= 0) {
			LocalDate periodStart = getStartDateTime().toLocalDate();
			LocalDate periodEnd = getEndDateTime().toLocalDate();

			if (day.compareTo(periodStart) >= 0 && day.compareTo(periodEnd) <= 0) {
				isInPeriod = true;
			}
		}

		return isInPeriod;
	}

	public TimeLoss getLossCategory() {
		return this.timeLoss;
	}

	public void setLossCategory(TimeLoss loss) {
		this.timeLoss = loss;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExceptionPeriod) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getWorkSchedule());
	}

	/**
	 * Check to see if this is a working period designated as a no loss category
	 * 
	 * @return True if a working period, else false
	 */
	public boolean isWorkingPeriod() {
		return timeLoss.equals(TimeLoss.NO_LOSS);
	}

	/**
	 * Build a string representation of this non-working period
	 */
	@Override
	public String toString() {
		String text = "";

		try {
			text = super.toString() + ", Start: " + getStartDateTime() + " (" + getDuration() + ")" + ", End: "
					+ getEndDateTime();
		} catch (Exception e) {
			// ignore
		}
		return text;
	}
}
