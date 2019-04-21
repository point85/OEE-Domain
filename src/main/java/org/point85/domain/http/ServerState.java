package org.point85.domain.http;

import org.point85.domain.i18n.DomainLocalizer;

public enum ServerState {
	// server state
	STARTED, STOPPED;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case STARTED:
			key = "started.state";
			break;
		case STOPPED:
			key = "stopped.state";
			break;
		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
