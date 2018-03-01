package org.point85.domain.http;

import java.time.OffsetDateTime;

public interface HttpEventListener {
	void onHttpEquipmentEvent(String sourceId, String dataValue, OffsetDateTime timestamp);
}
