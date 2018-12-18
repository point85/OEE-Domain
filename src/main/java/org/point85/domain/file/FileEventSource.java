package org.point85.domain.file;

import java.io.File;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.FILE_VALUE)

public class FileEventSource extends CollectorDataSource {
	public FileEventSource() {
		super();
		setDataSourceType(DataSourceType.FILE);
	}

	public FileEventSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.FILE);
	}

	@Override
	public String getId() {
		return getHost();
	}

	@Override
	public void setId(String id) {
		setHost(id);
	}

	public String getNetworkPath(String sourceId) {
		return getHost() + File.separatorChar + sourceId;
	}
}
