package org.point85.domain;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.openscada.opc.dcom.common.FILETIME;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.UnitOfMeasure;

public class DomainUtils {
	// folder with configuration files
	public static final String CONFIG_DIR = "config_dir";

	// pattern for OffsetDateTime conversion
	private static final String OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS ZZZZZ";

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

	public static String offsetDateTimeToString(OffsetDateTime odt) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(OFFSET_DATE_TIME_PATTERN);
		return (odt != null) ? odt.format(dtf) : null;
	}

	public static OffsetDateTime offsetDateTimeFromString(String formatted) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(OFFSET_DATE_TIME_PATTERN);
		return OffsetDateTime.parse(formatted, dtf);
	}

	// create a UTC OffsetDateTime from the DateTime
	public static synchronized OffsetDateTime utcTimeFromDateTime(DateTime dateTime) {
		long epochMillis = dateTime.getJavaTime();
		Instant instant = Instant.ofEpochMilli(epochMillis);
		OffsetDateTime time = OffsetDateTime.ofInstant(instant, ZoneId.of("Z"));
		return time;
	}

	// create a local OffsetDateTime from the DateTime
	public static synchronized OffsetDateTime localTimeFromDateTime(DateTime dateTime) {
		long epochMillis = dateTime.getJavaTime();
		Instant instant = Instant.ofEpochMilli(epochMillis);
		OffsetDateTime time = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		return time;
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

		StringBuilder sb = new StringBuilder();
		sb.append(decimalFormatSymbols.getGroupingSeparator());
		String separator = sb.toString();

		String[] thousands = formattedString.split(separator);

		sb = new StringBuilder();

		for (String thousand : thousands) {
			sb.append(thousand);
		}
		return sb.toString();
	}

	public static OffsetDateTime fromFiletime(FILETIME filetime) {
		Calendar cal = filetime.asCalendar();
		Instant instant = Instant.ofEpochMilli(cal.getTime().getTime());
		OffsetDateTime odt = OffsetDateTime.ofInstant(instant, cal.getTimeZone().toZoneId());
		return odt;
	}

	// encode the string in base64
	public static String encode(String toEncode) {
		String encoded = null;

		if (toEncode != null) {
			byte[] bytes = toEncode.getBytes(StandardCharsets.UTF_8);
			encoded = Base64.getEncoder().withoutPadding().encodeToString(bytes);
		}
		return encoded;
	}

	// decode a base64 encoded string
	public static String decode(String toDecode) {
		String decoded = null;

		if (toDecode != null) {
			byte[] bytes = toDecode.getBytes(StandardCharsets.UTF_8);
			byte[] decodedBytes = Base64.getDecoder().decode(bytes);
			decoded = new String(decodedBytes);
		}
		return decoded;
	}

	public static UnitOfMeasure getUomBySymbol(String symbol) throws Exception {
		if (symbol == null) {
			throw new Exception("The unit of measure symbol cannot be null.");
		}

		// try cache
		UnitOfMeasure uom = MeasurementSystem.instance().getUOM(symbol);

		if (uom == null) {
			// not cached
			uom = PersistenceService.instance().fetchUomBySymbol(symbol);
		}

		if (uom == null) {
			throw new Exception("Unit of measure with symbol " + symbol + " does not exist.");
		}
		return uom;
	}
}
