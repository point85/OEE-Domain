package org.point85.domain.http;

import org.point85.domain.dto.EquipmentEventRequestDto;

public interface HttpEventListener {
	// predefined event source
	void onHttpEquipmentEvent(EquipmentEventRequestDto dto) throws Exception;
}
