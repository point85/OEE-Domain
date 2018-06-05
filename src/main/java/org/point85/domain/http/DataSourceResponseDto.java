package org.point85.domain.http;

import java.util.List;

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
