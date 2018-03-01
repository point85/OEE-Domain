package org.point85.domain.persistence;

import java.time.OffsetDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.point85.domain.DomainUtils;

@Converter
public class OffsetDateTimeConverter implements AttributeConverter<OffsetDateTime, String> {

	@Override
	public String convertToDatabaseColumn(OffsetDateTime time) {
		return DomainUtils.offsetDateTimeToString(time);
	}

	@Override
	public OffsetDateTime convertToEntityAttribute(String timestamp) {
		String toConvert = timestamp;
		if (timestamp.indexOf('T') == -1) {
			// missing T
			String[] tokens = timestamp.split(" ");

			if (tokens.length > 0) {
				StringBuffer sb = new StringBuffer();

				// date + T + time + offset
				sb.append(tokens[0]).append('T').append(tokens[1]);
				if (tokens.length == 3) {
					sb.append(tokens[2]);
				}
				toConvert = sb.toString();
			}
		}
		OffsetDateTime odt = null;

		try {
			odt = DomainUtils.offsetDateTimeFromString(toConvert);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return odt;
	}
}
