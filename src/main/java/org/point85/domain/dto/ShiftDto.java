package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.schedule.Break;
import org.point85.domain.schedule.Shift;

public class ShiftDto extends TimePeriodDto {
	private List<BreakDto> breaks = new ArrayList<>();

	public ShiftDto(Shift shift) {
		super(shift);

		for (Break period : shift.getBreaks()) {
			breaks.add(new BreakDto(period));
		}
	}

	public List<BreakDto> getBreaks() {
		return this.breaks;
	}
}
