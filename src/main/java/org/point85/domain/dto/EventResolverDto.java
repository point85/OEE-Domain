package org.point85.domain.dto;

import org.point85.domain.script.EventResolver;

public class EventResolverDto {
	// CollectorDataSource
	private String dataSource;

	private String sourceId;

	private String functionScript;

	private Integer updatePeriod;

	private String dataType;

	// DataCollector
	private String collector;

	// OeeEventType
	private String type;

	public EventResolverDto(EventResolver resolver) {
		this.dataSource = resolver.getDataSource() != null ? resolver.getDataSource().getName() : null;
		this.sourceId = resolver.getSourceId();
		this.functionScript = resolver.getScript();
		this.updatePeriod = resolver.getUpdatePeriod();
		this.dataType = resolver.getDataType();
		this.type = resolver.getType() != null ? resolver.getType().name() : null;
		this.collector = resolver.getCollector() != null ? resolver.getCollector().getName() : null;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getScript() {
		return functionScript;
	}

	public void setScript(String functionScript) {
		this.functionScript = functionScript;
	}

	public Integer getUpdatePeriod() {
		return updatePeriod;
	}

	public void setUpdatePeriod(Integer updatePeriod) {
		this.updatePeriod = updatePeriod;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getCollector() {
		return collector;
	}

	public void setCollector(String collector) {
		this.collector = collector;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
