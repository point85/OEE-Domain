package org.point85.domain.opc.da;

import java.util.ArrayList;
import java.util.List;

import org.point85.domain.collector.CollectorDataSource;

public class TagGroupInfo {
	// name
	private String groupName;

	// group update period
	private int updatePeriod = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;

	// list of tag information
	private final List<TagItemInfo> tagItems = new ArrayList<>();

	public TagGroupInfo(String name) {
		this.groupName = name;
	}

	public List<TagItemInfo> getTagItems() {
		return this.tagItems;
	}

	public void addTagItem(TagItemInfo tagItem) {
		this.tagItems.add(tagItem);
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getUpdatePeriod() {
		return updatePeriod;
	}

	public void setUpdatePeriod(int updatePeriod) {
		this.updatePeriod = updatePeriod;
	}

	@Override
	public String toString() {
		return getGroupName();
	}
}
