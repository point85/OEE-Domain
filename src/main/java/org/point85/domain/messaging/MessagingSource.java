package org.point85.domain.messaging;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.MESSAGING_VALUE)

public class MessagingSource extends CollectorDataSource {

	public MessagingSource() {
		super();
		setDataSourceType(DataSourceType.MESSAGING);
	}

	public MessagingSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.MESSAGING);
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
