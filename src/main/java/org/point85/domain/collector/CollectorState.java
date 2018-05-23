package org.point85.domain.collector;

public enum CollectorState {
	DEV, READY, RUNNING;

	public static final String DEV_VALUE = "DEV";
	public static final String READY_VALUE = "READY";
	public static final String RUNNING_VALUE = "RUNNING";

	public boolean isValidTransition(CollectorState toState) {
		boolean answer = false;

		switch (this) {
		case DEV:
			if (toState.equals(CollectorState.READY) || toState.equals(CollectorState.DEV)) {
				answer = true;
			}
			break;
		case READY:
			if (toState.equals(CollectorState.RUNNING) || toState.equals(CollectorState.DEV)
					|| toState.equals(CollectorState.READY)) {
				answer = true;
			}
			break;
		case RUNNING:
			if (toState.equals(CollectorState.READY) || toState.equals(CollectorState.RUNNING)) {
				answer = true;
			}
			break;
		default:
			break;

		}
		return answer;
	}
}
