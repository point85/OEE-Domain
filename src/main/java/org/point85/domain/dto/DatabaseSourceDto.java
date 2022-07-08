package org.point85.domain.dto;

import org.point85.domain.db.DatabaseEventSource;

public class DatabaseSourceDto extends CollectorDataSourceDto {
	public DatabaseSourceDto(DatabaseEventSource source) {
		super(source);
	}
}
