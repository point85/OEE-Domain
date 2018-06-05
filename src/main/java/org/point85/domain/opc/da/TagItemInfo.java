package org.point85.domain.opc.da;

import org.point85.domain.collector.CollectorDataSource;

public class TagItemInfo {
	// path to tag leaf node
	private final String pathName;
	
	// requested update period
	private int updatePeriod = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;

	public TagItemInfo(String pathName) {
		this.pathName = pathName;
	}

	public String getPathName() {
		return pathName;
	}

	@Override
	public String toString() {
		return getPathName();
	}

	public int getUpdatePeriod() {
		return updatePeriod;
	}

	public void setUpdatePeriod(int updatePeriod) {
		this.updatePeriod = updatePeriod;
	}
}
