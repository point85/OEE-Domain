package org.point85.domain.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) for data source HTTP response
 */
public class DataSourceResponseDto {
	private List<DataSourceDto> dataSourceList;

	public DataSourceResponseDto(List<DataSourceDto> dataSourceList) {
		this.dataSourceList = dataSourceList;
	}

	public List<DataSourceDto> getDataSourceList() {
		return dataSourceList;
	}

	public void setDataSourceList(List<DataSourceDto> dataSourceList) {
		this.dataSourceList = dataSourceList;
	}

}
