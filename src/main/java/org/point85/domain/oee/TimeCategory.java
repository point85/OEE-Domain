package org.point85.domain.oee;

public enum TimeCategory {
	TOTAL, REQUIRED_OPERATIONS, AVAILABLE, SCHEDULED_PRODUCTION, PRODUCTION, REPORTED_PRODUCTION, NET_PRODUCTION, EFFICIENT_NET_PRODUCTION, EFFECTIVE_NET_PRODUCTION, VALUE_ADDING;

	@Override
	public String toString() {
		String value = "Undefined";

		switch (this) {
		case TOTAL:
			value = "Total";
			break;
		case REQUIRED_OPERATIONS:
			value = "Required Operations";
			break;
		case AVAILABLE:
			value = "Available";
			break;
		case SCHEDULED_PRODUCTION:
			value = "Scheduled Production";
			break;
		case PRODUCTION:
			value = "Production";
			break;
		case REPORTED_PRODUCTION:
			value = "Reported Production";
			break;
		case NET_PRODUCTION:
			value = "Net Production";
			break;
		case EFFICIENT_NET_PRODUCTION:
			value = "Efficient Production";
			break;
		case EFFECTIVE_NET_PRODUCTION:
			value = "Effective Production";
			break;
		case VALUE_ADDING:
			value = "Value Adding";
			break;
		default:
			break;
		}

		return value;
	}
}
