package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.collector.CollectorState;

@Converter
public class CollectorStateConverter implements AttributeConverter<CollectorState, String> {
	@Override
	public String convertToDatabaseColumn(CollectorState attribute) {
		String value = null;

		if (attribute == null) {
			return value;
		}

		switch (attribute) {
		case DEV:
			value = CollectorState.DEV_VALUE;
			break;
		case READY:
			value = CollectorState.READY_VALUE;
			break;
		case RUNNING:
			value = CollectorState.RUNNING_VALUE;
			break;
		default:
			break;
		}

		return value;
	}

	@Override
	public CollectorState convertToEntityAttribute(String value) {
		CollectorState state = null;

		if (value == null) {
			return state;
		}

		switch (value) {
		case CollectorState.DEV_VALUE:
			state = CollectorState.DEV;
			break;
		case CollectorState.READY_VALUE:
			state = CollectorState.READY;
			break;
		case CollectorState.RUNNING_VALUE:
			state = CollectorState.RUNNING;
			break;
		default:
			break;
		}

		return state;
	}
}
