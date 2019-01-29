package org.point85.domain.mqtt;

public enum QualityOfService {
	AT_MOST_ONCE(0), AT_LEAST_ONCE(1), EXACTLY_ONCE(2);

	private int qos;

	private QualityOfService(int qos) {
		this.qos = qos;
	}

	public int getQos() {
		return qos;
	}

}
