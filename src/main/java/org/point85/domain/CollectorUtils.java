package org.point85.domain;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CollectorUtils {

	public static String[] parseDomainAndUser(String user) {

		if (user == null) {
			return null;
		}

		String delimeters = "[/" + "\\Q\\\\E" + "]+";
		String[] info = new String[2];
		String[] tokens = user.split(delimeters);

		info[0] = "localhost";
		info[1] = user;

		if (tokens.length == 2) {
			info[0] = tokens[0];
			info[1] = tokens[1];
		}

		return info;
	}

	public static String zonedDateTimeToString(ZonedDateTime zdt) {
		return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static String offsetDateTimeToString(OffsetDateTime odt) {
		return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static OffsetDateTime offsetDateTimeFromString(String timestamp) throws Exception {
		if (timestamp == null) {
			return OffsetDateTime.now();
		}

		// look for T separator
		if (timestamp.indexOf('T') == -1) {
			// missing T
			int space = timestamp.indexOf(' ');

			if (space != -1) {
				timestamp.split(" ");
			}
		}
		return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
