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
import java.time.LocalTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.point85.domain.dto.BreakDto;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.TimeLossConverter;

/**
 * Class Break is defined as a working period of time for operators during a shift,
 * for example lunch. For OEE calculations, a time loss can be associated with a
 * break if the equipment is not scheduled for production during this period. 
 * 
 * @author Kent Randall
 *
 */
@Entity
@Table(name = "BREAK_PERIOD")
@AttributeOverride(name = "primaryKey", column = @Column(name = "BREAK_KEY"))
public class Break extends TimePeriod implements Comparable<Break> {
	// owning shift
	@ManyToOne
	@JoinColumn(name = "SHIFT_KEY")
	private Shift shift;

	// loss category (can be value adding)
	@Column(name = "LOSS")
	@Convert(converter = TimeLossConverter.class)
	private TimeLoss timeLoss = TimeLoss.NO_LOSS;

	public Break() {
		// for persistence
	}

	/**
	 * Construct a period of time for a break
	 * 
	 * @param name        Name of break
	 * @param description Description of break
	 * @param start       Starting time of day
	 * @param duration    Duration of break
	 * @throws Exception exception
	 */
	public Break(String name, String description, LocalTime start, Duration duration) throws Exception {
		super(name, description, start, duration);
	}

	/**
	 * Construct a Break from its data transfer object
	 * 
	 * @param dto {@link BreakDto}
	 * @throws Exception Exception
	 */
	public Break(BreakDto dto) throws Exception {
		this.setName(dto.getName());
		this.setDescription(dto.getDescription());
		this.setStart(LocalTime.ofSecondOfDay(dto.getStartTime()));
		this.setDuration(Duration.ofSeconds(dto.getDuration()));
		this.timeLoss = dto.getTimeLoss() != null ? TimeLoss.valueOf(dto.getTimeLoss()) : null;
	}

	@Override
	public boolean isWorkingPeriod() {
		// operator is working a shift, thus a working period
		return true;
	}

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public TimeLoss getLossCategory() {
		return this.timeLoss;
	}

	public void setLossCategory(TimeLoss loss) {
		this.timeLoss = loss;
	}

	/**
	 * Compare this break period to another such period by name and time of day
	 * 
	 * @param other {@link Break}
	 * @return negative if less than, 0 if equal and positive if greater than
	 */
	@Override
	public int compareTo(Break other) {
		int value = 0;
		if (this.getName().equals(other.getName()) && this.getStart().equals(other.getStart())) {
			value = 0;
		} else {
			value = this.getName().compareTo(other.getName());
		}
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Break) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getShift());
	}
}
