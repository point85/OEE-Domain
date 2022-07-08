package org.point85.domain.dto;

import org.point85.domain.db.DatabaseEventSource;

public class DatabaseEventSourceDto extends CollectorDataSourceDto {
	public DatabaseEventSourceDto(DatabaseEventSource source) {
		super(source);
	}
}
