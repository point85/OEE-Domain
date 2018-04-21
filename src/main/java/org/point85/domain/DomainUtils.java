package org.point85.domain;

import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

public class DomainUtils {

	// format a Duration
	public static String formatDuration(Duration duration) {
		if (duration == null) {
			return "";
		} else {
			// remove the "PT" prefix
			long seconds = duration.getSeconds();

			if (duration.getNano() > 500000000l) {
				seconds += 1;
			}
			Duration rounded = Duration.ofSeconds(seconds);
			return rounded.toString().substring(2);
		}
	}

	public static String[] parseDomainAndUser(String user) {
		String[] info = new String[2];
		if (user == null) {
			return info;
		}

		String delimeters = "[/" + "\\Q\\\\E" + "]+";
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
		return (zdt != null) ? zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
	}

	public static String offsetDateTimeToString(OffsetDateTime odt) {
		return (odt != null) ? odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
	}

	public static OffsetDateTime offsetDateTimeFromString(String timestamp) throws Exception {
		if (timestamp == null) {
			return OffsetDateTime.now();
		}

		String timeString = timestamp;

		// look for T separator
		if (timeString.indexOf('T') == -1) {
			// missing T
			if (timestamp.indexOf(' ') != -1) {
				timeString = timeString.replace(' ', 'T');
			}
		}
		return OffsetDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	// create a UTC ZonedDateTime from the DateTime
	public static synchronized ZonedDateTime utcTimeFromDateTime(DateTime dateTime) {
		long epochMillis = dateTime.getJavaTime();
		Instant instant = Instant.ofEpochMilli(epochMillis);
		ZonedDateTime time = ZonedDateTime.ofInstant(instant, ZoneId.of("Z"));
		return time;
	}

	// create a local ZonedDateTime from the DateTime
	public static synchronized OffsetDateTime localTimeFromDateTime(DateTime dateTime) {
		ZonedDateTime utc = utcTimeFromDateTime(dateTime);
		ZonedDateTime time = utc.withZoneSameInstant(ZoneId.systemDefault());
		return OffsetDateTime.from(time);
	}

	public static OffsetDateTime fromLocalDateTime(LocalDateTime ldt) {
		OffsetDateTime odt = null;
		if (ldt != null) {
			ZoneOffset offset = OffsetDateTime.now().getOffset();
			odt = OffsetDateTime.of(ldt, offset);
		}
		return odt;
	}

	// removed formatting from decimal string
	public static String removeThousandsSeparator(String formattedString) {
		if (formattedString == null) {
			return null;
		}
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

		StringBuffer sb = new StringBuffer();
		sb.append(decimalFormatSymbols.getGroupingSeparator());
		String separator = sb.toString();

		String[] thousands = formattedString.split(separator);

		sb = new StringBuffer();

		for (String thousand : thousands) {
			sb.append(thousand);
		}
		return sb.toString();
	}
}
