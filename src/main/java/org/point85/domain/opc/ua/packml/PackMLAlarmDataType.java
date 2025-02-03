package org.point85.domain.opc.ua.packml;

import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.serialization.UaDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaEncoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaStructure;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.GenericDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;

import com.google.common.base.Objects;

/**
 * The PackMLAlarmDataType provides the PackML tag alarm structure.
 */
public class PackMLAlarmDataType implements UaStructure {
	// data type node id
	public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLAlarmDataType));

	// encoding node id
	public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLAlarmDataType_Encoding_DefaultBinary));

	// alarm fields
	private static final String ID_FIELD = "ID";
	private static final String VALUE_FIELD = "Value";
	private static final String MESSAGE_FIELD = "Message";
	private static final String CATEGORY_FIELD = "Category";
	private static final String DATETIME_FIELD = "DateTime";
	private static final String ACK_DATETIME_FIELD = "AckDateTime";
	private static final String TRIGGER_FIELD = "Trigger";

	// A unique number assigned to each type of alarm, stop or warning.
	private Integer id;

	// An alarm, stop or warning message number associated to the ID to allow for
	// user specific detail or to break down the Alarm.ID to greater detail
	private Integer value;

	// The actual text of the alarm, stop or warning for those machines capable of
	// providing string information
	private String message;

	// A user defined value which indicates what type of alarm, stop or warning has
	// occurred. E.g. electrical, mechanical, process limit, …
	private Integer category;

	// The date and time that the alarm, stop or warning occurred, in ISO 8601
	// format (year, month, day, hour, minute, second, msec), as an array of 32 bit
	// integers. Any unused date time elements should be set to zero
	private DateTime dateTime;

	// The date and time that the alarm, stop or warning has been acknowledged, in
	// ISO 8601 format (year, month, day, hour, minute, second, msec), as an array
	// of 32 bit integers. Any unused date time elements should be set to zero
	private DateTime ackDateTime;

	// This variable is true when the alarm is active
	private Boolean trigger;

	public PackMLAlarmDataType() {
		this(null, null, null, null, null, null, null);
	}

	public PackMLAlarmDataType(Integer id, Integer value, String message, Integer category, DateTime dateTime,
			DateTime ackDateTime, Boolean trigger) {
		this.id = id;
		this.value = value;
		this.message = message;
		this.category = category;
		this.dateTime = dateTime;
		this.ackDateTime = ackDateTime;
		this.trigger = trigger;
	}

	@Override
	public ExpandedNodeId getTypeId() {
		return TYPE_ID;
	}

	@Override
	public ExpandedNodeId getBinaryEncodingId() {
		return BINARY_ENCODING_ID;
	}

	@Override
	public ExpandedNodeId getXmlEncodingId() {
		// XML encoding not supported
		return ExpandedNodeId.NULL_VALUE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PackMLAlarmDataType that = (PackMLAlarmDataType) o;
		return value == that.value && Objects.equal(message, that.message) && Objects.equal(id, that.id)
				&& dateTime == that.dateTime;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value, id, message, dateTime);
	}

	public Integer getId() {
		return id;
	}

	public Integer getValue() {
		return value;
	}

	public String getMessage() {
		return message;
	}

	public Integer getCategory() {
		return category;
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public DateTime getAckDateTime() {
		return ackDateTime;
	}

	public Boolean getTrigger() {
		return trigger;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID\t").append(getId());
		sb.append('\n').append("Value\t").append(getValue());
		sb.append('\n').append("Message\t").append(getMessage());
		sb.append('\n').append("Category\t").append(getCategory());
		sb.append('\n').append("DateTime\t").append(getDateTime().toString());
		sb.append('\n').append("AckDateTime\t").append(getAckDateTime().toString());
		sb.append('\n').append("Trigger\t").append(getTrigger());
		return sb.toString();
	}

	/**
	 * Binary encoding/decoding codec
	 */
	public static class Codec extends GenericDataTypeCodec<PackMLAlarmDataType> {
		@Override
		public Class<PackMLAlarmDataType> getType() {
			return PackMLAlarmDataType.class;
		}

		@Override
		public PackMLAlarmDataType decode(SerializationContext context, UaDecoder decoder)
				throws UaSerializationException {

			Integer id = decoder.readInt32(ID_FIELD);
			Integer value = decoder.readInt32(VALUE_FIELD);
			String message = decoder.readString(MESSAGE_FIELD);
			Integer category = decoder.readInt32(CATEGORY_FIELD);
			DateTime dateTime = decoder.readDateTime(DATETIME_FIELD);
			DateTime ackDateTime = decoder.readDateTime(ACK_DATETIME_FIELD);
			Boolean trigger = decoder.readBoolean(TRIGGER_FIELD);
			return new PackMLAlarmDataType(id, value, message, category, dateTime, ackDateTime, trigger);
		}

		@Override
		public void encode(SerializationContext context, UaEncoder encoder, PackMLAlarmDataType value)
				throws UaSerializationException {

			encoder.writeInt32(ID_FIELD, value.id);
			encoder.writeInt32(VALUE_FIELD, value.value);
			encoder.writeString(MESSAGE_FIELD, value.message);
			encoder.writeInt32(CATEGORY_FIELD, value.category);
			encoder.writeDateTime(DATETIME_FIELD, value.dateTime);
			encoder.writeDateTime(ACK_DATETIME_FIELD, value.ackDateTime);
			encoder.writeBoolean(TRIGGER_FIELD, value.trigger);
		}
	}
}
