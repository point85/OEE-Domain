package org.point85.domain.http;

public interface HttpEventListener {
	void onHttpEquipmentEvent(String sourceId, String dataValue, String timestamp, String reason);
}
