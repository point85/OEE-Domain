package org.point85.domain.web;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.WEB_VALUE)

/*
@NamedQueries({
		@NamedQuery(name = WebSource.WEB_SRC_BY_NAME, query = "SELECT source FROM WebSource source WHERE source.name = :name"),
		@NamedQuery(name = WebSource.WEB_SRC_BY_TYPE, query = "SELECT source FROM WebSource source WHERE source.sourceType = '"
				+ DataSourceType.WEB_VALUE + "'"), })
				*/

public class WebSource extends DataSource {
	// queries
	//public static final String WEB_SRC_BY_NAME = "WEB.ByName";
	//public static final String WEB_SRC_BY_TYPE = "WEB.ByType";

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
