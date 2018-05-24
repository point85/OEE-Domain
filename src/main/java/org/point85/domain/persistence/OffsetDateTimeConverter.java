package org.point85.domain.persistence;

import java.time.OffsetDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.DomainUtils;

@Converter
public class OffsetDateTimeConverter implements AttributeConverter<OffsetDateTime, String> {

	@Override
	public String convertToDatabaseColumn(OffsetDateTime time) {
		String converted = null;
		if (time != null) {
			converted = DomainUtils.offsetDateTimeToString(time);
		}
		return converted;
	}

	@Override
	public OffsetDateTime convertToEntityAttribute(String timestamp) {
		OffsetDateTime odt = null;
		if (timestamp != null) {
			odt = DomainUtils.offsetDateTimeFromString(timestamp);
		}
		return odt;
	}
}
