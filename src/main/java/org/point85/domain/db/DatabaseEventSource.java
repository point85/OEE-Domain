package org.point85.domain.db;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.DatabaseSourceDto;

@Entity
@DiscriminatorValue(DataSourceType.DATABASE_VALUE)

public class DatabaseEventSource extends CollectorDataSource {

	public DatabaseEventSource() {
		super();
		setDataSourceType(DataSourceType.DATABASE);
	}

	public DatabaseEventSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.DATABASE);
	}

	public DatabaseEventSource(DatabaseSourceDto dto) {
		super(dto);
	}

	@Override
	public String getId() {
		return getHost();
	}

	@Override
	public void setId(String id) {
		setHost(id);
	}
}
