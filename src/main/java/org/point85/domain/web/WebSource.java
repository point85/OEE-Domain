package org.point85.domain.web;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.WEB_VALUE)

public class WebSource extends CollectorDataSource {
	public WebSource() {
		super();
		setDataSourceType(DataSourceType.WEB);
	}

	public WebSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.WEB);
	}

	@Override
	public String getId() {
		return getHost() + ":" + getPort();
	}

	@Override
	public void setId(String id) {
		String[] tokens = id.split(":");
		if (tokens.length == 2) {
			setHost(tokens[0]);
			setPort(Integer.valueOf(tokens[1]));
		}
	}
}
