package org.point85.domain.http;

public interface HttpEventListener {
	// predefined event source
	void onHttpEquipmentEvent(EquipmentEventRequestDto dto) throws Exception;
}
