package org.point85.domain.proficy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Serialized tag sample data
 *
 */
public class TagSample {
	@SerializedName(value = "TimeStamp")
	private String timeStamp;

	@SerializedName(value = "Value")
	private String value;

	@SerializedName(value = "Quality")
	private Integer quality;

	/**
	 * Create a tag sample from a value
	 * 
	 * @param dataValue Java object data value
	 */
	public TagSample(Object dataValue) {
		timeStamp = Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
		value = dataValue.toString();
		quality = TagQuality.Good.getQuality();
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public String getValue() {
		return value;
	}

	public Integer getQuality() {
		return quality;
	}

	public boolean isArray() {
		return value.contains("[");
	}

	/**
	 * Get the Java Instant for the UTC time stamp
	 * 
	 * @return Instant in time
	 */
	public Instant getTimeStampInstant() {
		return (timeStamp != null) ? Instant.parse(timeStamp) : null;
	}

	/**
	 * Get the OffsetDateTime of the tag's time stamp in the local time zone
	 * 
	 * @return OffsetDateTime time stamp
	 */
	public OffsetDateTime getTimeStampTime() {
		Instant timestamp = getTimeStampInstant();

		// UTC time in local zone
		return (timestamp != null) ? OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault()) : null;
	}

	/**
	 * Get the Java value for this tag data type
	 * 
	 * @param type {@link TagDataType}
	 * @return Java Object
	 */
	public Object getTypedValue(TagDataType type) {
		return !type.isArray() ? TagSample.parseType(getValue(), type) : null;
	}

	private static Object parseType(String tagValue, TagDataType type) {
		Object typedValue = null;
		Class<?> clazz = type.getJavaType();

		if (clazz != null) {
			if (clazz.equals(String.class)) {
				typedValue = tagValue;
			} else if (clazz.equals(Byte.class)) {
				typedValue = Byte.valueOf(tagValue);
			} else if (clazz.equals(Short.class)) {
				typedValue = Short.valueOf(tagValue);
			} else if (clazz.equals(Integer.class)) {
				typedValue = Integer.valueOf(tagValue);
			} else if (clazz.equals(Long.class)) {
				typedValue = Long.valueOf(tagValue);
			} else if (clazz.equals(Float.class)) {
				typedValue = Float.valueOf(tagValue);
			} else if (clazz.equals(Double.class)) {
				typedValue = Double.valueOf(tagValue);
			} else if (clazz.equals(Boolean.class)) {
				typedValue = Boolean.valueOf(tagValue);
			} else if (clazz.equals(Instant.class)) {
				typedValue = Instant.parse(tagValue);
			}
		}
		return typedValue;
	}

	/**
	 * A Java typed list from an array tag
	 * 
	 * @param type {@link TagDataType}
	 * @return List of Object
	 */
	public List<Object> getTypedList(TagDataType type) {
		List<Object> values = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		sb.append('{').append('"').append("Values").append('"').append(':').append(value).append('}');
		String json = sb.toString();

		Gson gson = new Gson();
		TagStringArray tagStringArray = gson.fromJson(json, TagStringArray.class);

		for (String stringValue : tagStringArray.getValues()) {
			values.add(TagSample.parseType(stringValue, type));
		}
		return values;
	}

	public TagQuality getEnumeratedQuality() {
		return TagQuality.fromInt(quality);
	}
}
