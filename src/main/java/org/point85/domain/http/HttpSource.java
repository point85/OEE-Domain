package org.point85.domain.http;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.HTTP_VALUE)

//@NamedQueries({
		//@NamedQuery(name = HttpSource.HTTP_SRC_BY_TYPE, query = "SELECT source FROM HttpSource source WHERE source.sourceType = '"
				//+ DataSourceType.HTTP_VALUE + "'"), })

public class HttpSource extends DataSource {
	// queries
	//public static final String HTTP_SRC_BY_TYPE = "HTTP.ByType";

	public HttpSource() {
		super();
		setDataSourceType(DataSourceType.HTTP);
	}

	public HttpSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.HTTP);
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
