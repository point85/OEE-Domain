package org.point85.domain.cron;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.CronSourceDto;

/**
 * 
 * The cron event source
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.CRON_VALUE)
public class CronEventSource extends CollectorDataSource {
	// overloaded for cron expression
	@Column(name = "END_PATH")
	private String cronExpression;

	public CronEventSource() {
		super();
		setDataSourceType(DataSourceType.CRON);
	}

	/**
	 * Constructor
	 * 
	 * @param name        Source name
	 * @param description Source description
	 */
	public CronEventSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.CRON);
	}

	public CronEventSource(CronSourceDto dto) {
		super(dto);

		setCronExpression(dto.getCronExpression());
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
		return cronExpression;
	}

	public void setCronExpression(String expression) {
		cronExpression = expression;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CronEventSource) {
			return super.equals(obj) && cronExpression.equals(((CronEventSource) obj).getCronExpression());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getCronExpression());
	}
}
