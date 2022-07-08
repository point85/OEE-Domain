package org.point85.domain.dto;

import org.point85.domain.rmq.RmqSource;

public class RmqSourceDto extends CollectorDataSourceDto {
	public RmqSourceDto(RmqSource source) {
		super(source);
	}
}
