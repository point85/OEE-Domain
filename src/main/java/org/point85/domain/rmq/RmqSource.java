package org.point85.domain.rmq;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.RmqSourceDto;

@Entity
@DiscriminatorValue(DataSourceType.RMQ_VALUE)
public class RmqSource extends CollectorDataSource {

	public RmqSource() {
		super();
		setDataSourceType(DataSourceType.RMQ);
	}

	public RmqSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.RMQ);
	}

	public RmqSource(RmqSourceDto dto) {
		super(dto);
		setDataSourceType(DataSourceType.RMQ);
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}
}
