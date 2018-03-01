package org.point85.domain.messaging;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.MESSAGING_VALUE)
/*
@NamedQueries({
		@NamedQuery(name = MessagingSource.MSG_SRC_BY_NAME, query = "SELECT source FROM MessagingSource source WHERE source.name = :name"),
		@NamedQuery(name = MessagingSource.MSG_SRC_BY_TYPE, query = "SELECT source FROM MessagingSource source WHERE source.sourceType = '"
				+ DataSourceType.MESSAGING_VALUE + "'"), })
				*/

public class MessagingSource extends DataSource {
	// queries
	//public static final String MSG_SRC_BY_NAME = "MSG.ByName";
	//public static final String MSG_SRC_BY_TYPE = "MSG.ByType";

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
