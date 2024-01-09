package org.point85.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.common.FILETIME;
import org.point85.domain.i18n.DomainLocalizer;

public final class DomainUtils {
	// ISO 8601 datetime format, yyyy-mm-ddThh:mm:ss.nnnnnn+|-hh:mm
	public static final String OFFSET_DATE_TIME_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";

	// ISO 8601 datetime format, yyyy-mm-ddThh:mm:ss.nnn
	public static final String LOCAL_DATE_TIME_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	// ISO 8601 date format, yyyy-mm-dd
	public static final String LOCAL_DATE_8601 = "yyyy-MM-dd";

	// ISO 8601 datetime UTC format, yyyy-mm-ddThh:mm:ss.nnnZ
	public static final String UTC_DATE_TIME_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	// pattern for OffsetDateTime display
	public static final String OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS ZZZZZ";

	// SSL key and truststore folder
	public static final String SECURITY_DIR = "config/security/";

	private DomainUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static String getVersionInfo() {
		return DomainLocalizer.instance().getLangString("version") + " 3.10.1, "
				+ LocalDate.of(2024, 1, 10).format(DateTimeFormatter.ISO_DATE);
	}

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

	public static String offsetDateTimeToString(OffsetDateTime odt, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (odt != null) ? odt.format(dtf) : null;
	}

	public static OffsetDateTime offsetDateTimeFromString(String iso8601, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (iso8601 != null) ? OffsetDateTime.parse(iso8601.trim(), dtf) : null;
	}

	public static String localDateTimeToString(LocalDateTime ldt, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (ldt != null) ? ldt.format(dtf) : null;
	}

	public static LocalDateTime localDateTimeFromString(String iso8601, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (iso8601 != null) ? LocalDateTime.parse(iso8601.trim(), dtf) : null;
	}

	public static String localDateToString(LocalDate ld, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (ld != null) ? ld.format(dtf) : null;
	}

	public static LocalDate localDateFromString(String iso8601, String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		return (iso8601 != null) ? LocalDate.parse(iso8601.trim(), dtf) : null;
	}

	// create a UTC OffsetDateTime from the DateTime
	public static OffsetDateTime utcTimeFromDateTime(DateTime dateTime) {
		long epochMillis = dateTime.getJavaTime();
		Instant instant = Instant.ofEpochMilli(epochMillis);
		return OffsetDateTime.ofInstant(instant, ZoneId.of("Z"));
	}

	// create a local OffsetDateTime from the DateTime
	public static OffsetDateTime localTimeFromDateTime(DateTime dateTime) {
		long epochMillis = dateTime.getJavaTime();
		Instant instant = Instant.ofEpochMilli(epochMillis);
		return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
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
		return OffsetDateTime.ofInstant(instant, cal.getTimeZone().toZoneId());
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

	public static String formatException(Exception e) {
		StringBuilder sb = new StringBuilder();

		sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage() != null ? e.getMessage() : "");

		if (e.getCause() != null) {
			sb.append("\n\tCause: ").append(e.getCause());
		}

		if (e instanceof MqttException) {
			MqttException me = (MqttException) e;
			sb.append("\n\tReason: ").append(me.getReasonCode());
		} else if (e instanceof JIException) {
			JIException jie = (JIException) e;
			sb.append("\n\tCode: ").append(jie.getErrorCode());
		}

		return sb.toString();
	}

	public static String getJVMInfo() {
		return System.getProperty("java.version") + ", " + System.getProperty("java.vm.name") + ", "
				+ System.getProperty("java.runtime.version");
	}

	public static int getJVMVersion() {
		int version = 8;
		String[] tokens = System.getProperty("java.version").split("\\.");

		if (!tokens[0].equals("1")) {
			version = Integer.parseInt(tokens[0]);
		}

		return version;
	}

	public static void gzip(String content, String fileName) throws Exception {
		try (FileOutputStream outputStream = new FileOutputStream(fileName);
				GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
			byte[] data = content.getBytes();
			gzipOutputStream.write(data);
		}
	}

	public static String gunzip(byte[] data) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		GZIPInputStream in = new GZIPInputStream(bis);
		byte[] buffer = new byte[1024];
		int len = 0;

		while ((len = in.read(buffer)) >= 0) {
			bos.write(buffer, 0, len);
		}

		in.close();
		bos.close();

		return new String(bos.toByteArray());
	}

	public static String byteArrayToHex(byte[] byteArray) {
		StringBuilder sb = new StringBuilder();
		for (byte b : byteArray) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}
