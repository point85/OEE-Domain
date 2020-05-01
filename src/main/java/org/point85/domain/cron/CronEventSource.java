package org.point85.domain.cron;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

/**
 * 
 * The cron event source
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.CRON_VALUE)
public class CronEventSource extends CollectorDataSource {
	public CronEventSource() {
		super();
		setDataSourceType(DataSourceType.CRON);
	}

	/**
	 * Constructor
	 * @param name Source name
	 * @param description Source description
	 */
	public CronEventSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.CRON);
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	public String getCronExpression() {
		return this.getEndpointPath();
	}

	public void setCronExpression(String expression) {
		setEndpointPath(expression);
	}
}
