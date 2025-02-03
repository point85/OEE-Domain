package org.point85.domain.oee;

import org.point85.domain.i18n.DomainLocalizer;

public enum OeeComponent {
	PERFORMANCE, AVAILABILITY, QUALITY, NON_WORKING, NORMAL;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case PERFORMANCE:
			key = "performance";
			break;
		case AVAILABILITY:
			key = "availability";
			break;
		case QUALITY:
			key = "quality";
			break;
		case NON_WORKING:
			key = "nonworking";
			break;
		case NORMAL:
			key = "normal";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
