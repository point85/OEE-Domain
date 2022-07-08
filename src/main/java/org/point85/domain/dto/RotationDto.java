package org.point85.domain.dto;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.RotationSegment;

public class RotationDto extends NamedObjectDto {
	// rotation segments
	private final List<RotationSegmentDto> rotationSegments = new ArrayList<>();

	public RotationDto(Rotation rotation) {
		super(rotation);
		
		for (RotationSegment segment : rotation.getRotationSegments()) {
			this.rotationSegments.add(new RotationSegmentDto(segment));	
		}
		
	}

	public List<RotationSegmentDto> getRotationSegments() {
		return rotationSegments;
	}

}
