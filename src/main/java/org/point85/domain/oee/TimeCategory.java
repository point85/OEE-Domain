package org.point85.domain.oee;

import org.point85.domain.i18n.DomainLocalizer;

public enum TimeCategory {
	TOTAL, REQUIRED_OPERATIONS, AVAILABLE, SCHEDULED_PRODUCTION, PRODUCTION, REPORTED_PRODUCTION, NET_PRODUCTION,
	EFFICIENT_NET_PRODUCTION, EFFECTIVE_NET_PRODUCTION, VALUE_ADDING;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case TOTAL:
			key = "total.cat";
			break;
		case REQUIRED_OPERATIONS:
			key = "required.cat";
			break;
		case AVAILABLE:
			key = "available.cat";
			break;
		case SCHEDULED_PRODUCTION:
			key = "scheduled.cat";
			break;
		case PRODUCTION:
			key = "production.cat";
			break;
		case REPORTED_PRODUCTION:
			key = "reported.cat";
			break;
		case NET_PRODUCTION:
			key = "net.cat";
			break;
		case EFFICIENT_NET_PRODUCTION:
			key = "efficient.cat";
			break;
		case EFFECTIVE_NET_PRODUCTION:
			key = "effective.cat";
			break;
		case VALUE_ADDING:
			key = "value.cat";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
