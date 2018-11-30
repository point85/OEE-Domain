package org.point85.domain.persistence;

import javax.persistence.AttributeConverter;

import org.point85.domain.db.DatabaseEventStatus;

public class DatabaseEventStatusConverter implements AttributeConverter<DatabaseEventStatus, String> {
	@Override
	public String convertToDatabaseColumn(DatabaseEventStatus status) {
		String value = null;

		if (status == null) {
			return value;
		}

		switch (status) {
		case PASS:
			value = DatabaseEventStatus.PASS_VALUE;
			break;

		case FAIL:
			value = DatabaseEventStatus.FAIL_VALUE;
			break;

		case READY:
			value = DatabaseEventStatus.READY_VALUE;
			break;

		default:
			break;
		}
		return value;
	}

	@Override
	public DatabaseEventStatus convertToEntityAttribute(String value) {
		DatabaseEventStatus status = null;

		if (value == null) {
			return status;
		}

		switch (value) {
		case DatabaseEventStatus.PASS_VALUE:
			status = DatabaseEventStatus.PASS;
			break;

		case DatabaseEventStatus.READY_VALUE:
			status = DatabaseEventStatus.READY;
			break;

		case DatabaseEventStatus.FAIL_VALUE:
			status = DatabaseEventStatus.FAIL;
			break;

		default:
			break;
		}
		return status;
	}
}
