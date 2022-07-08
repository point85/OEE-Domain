package org.point85.domain.jms;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.JmsSourceDto;

@Entity
@DiscriminatorValue(DataSourceType.JMS_VALUE)
public class JmsSource extends CollectorDataSource {
	public JmsSource() {
		super();
		setDataSourceType(DataSourceType.JMS);
	}

	public JmsSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.JMS);
	}

	public JmsSource(JmsSourceDto dto) {
		super(dto);
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
