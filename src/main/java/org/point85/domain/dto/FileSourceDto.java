package org.point85.domain.dto;

import org.point85.domain.file.FileEventSource;

public class FileSourceDto extends CollectorDataSourceDto {
	public FileSourceDto(FileEventSource source) {
		super(source);
	}
}
