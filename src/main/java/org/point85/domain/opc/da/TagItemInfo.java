package org.point85.domain.opc.da;

public class TagItemInfo {
	//public static final int DEFAULT_UPDATE_PERIOD = 1000;

	// path to tag leaf node
	private String pathName;
	// requested update period
	private int updatePeriod;

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
