package org.point85.domain.opc.ua.packml;

import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.serialization.UaDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaEncoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaStructure;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.GenericDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;

import com.google.common.base.Objects;

/**
 * The PackMLCountDataType is used to generate summary information about the
 * system. The information depending on the use might be related to produced
 * product, defective materials or any other information that needs to be
 * tracked.
 */
public class PackMLCountDataType implements UaStructure {
	// data type node id
	public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLCountDataType));

	// encoding node id
	public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLCountDataType_Encoding_DefaultBinary));

	// count fields
	private static final String ID_FIELD = "ID";
	private static final String NAME_FIELD = "Name";
	private static final String UNIT_FIELD = "Unit";
	private static final String COUNT_FIELD = "Count";
	private static final String ACCCOUNT_FIELD = "AccCount";

	// A user defined value that represents the consumed (processed or defective)
	// material. Typically this is an SKU number or a user material master number.
	private Integer id;

	// A string description of the material.
	private String name;

	// OPC UA engineering unit information for the count.
	private EUInformation unit;

	// The amount of consumed (processed or defective) material on the current
	// production job.
	private Integer count;

	// The cumulative count value of the material produced (or consumed). This
	// counter gives the user a non-resetting counter that may be used for OEE
	// calculations
	private Integer accCount;

	public PackMLCountDataType() {
		this(null, null, null, null, null);
	}

	/**
	 * Constructor to populate all fields
	 * 
	 * @param id       A user defined value that represents the consumed (processed
	 *                 or defective) material. Typically this is an SKU number or a
	 *                 user material master number.
	 * @param name     A string description of the material.
	 * @param unit     OPC UA engineering unit information for the count.
	 * @param count    The amount of consumed (processed or defective) material on
	 *                 the current production job.
	 * @param accCount The cumulative count value of the material produced (or
	 *                 consumed). This counter gives the user a non-resetting
	 *                 counter that may be used for OEE calculations.
	 */
	public PackMLCountDataType(Integer id, String name, EUInformation unit, Integer count, Integer accCount) {
		this.name = name;
		this.id = id;
		this.count = count;
		this.unit = unit;
		this.accCount = accCount;
	}

	public String getName() {
		return name;
	}

	public Integer getId() {
		return id;
	}

	public Integer getCount() {
		return count;
	}

	public Integer getAccCount() {
		return accCount;
	}

	public EUInformation getUnit() {
		return unit;
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
		PackMLCountDataType that = (PackMLCountDataType) o;
		return count == that.count && Objects.equal(name, that.name) && Objects.equal(id, that.id) && unit == that.unit;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, id, count, unit);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID\t").append(getId());
		sb.append('\n').append("Name\t").append(getName());
		sb.append('\n').append("Unit\t").append("EuInformation");
		sb.append(PackMLUtils.euInformationToString(getUnit()));
		sb.append('\n').append("Count\t").append(getCount());
		sb.append('\n').append("Accum\t").append(getAccCount());
		return sb.toString();
	}

	/**
	 * Binary encoding/decoding codec
	 */
	public static class Codec extends GenericDataTypeCodec<PackMLCountDataType> {
		@Override
		public Class<PackMLCountDataType> getType() {
			return PackMLCountDataType.class;
		}

		@Override
		public PackMLCountDataType decode(SerializationContext context, UaDecoder decoder)
				throws UaSerializationException {

			Integer id = decoder.readInt32(ID_FIELD);
			String name = decoder.readString(NAME_FIELD);
			EUInformation unit = (EUInformation) decoder.readStruct(UNIT_FIELD, EUInformation.TYPE_ID);
			Integer count = decoder.readInt32(COUNT_FIELD);
			Integer accCount = decoder.readInt32(ACCCOUNT_FIELD);

			return new PackMLCountDataType(id, name, unit, count, accCount);
		}

		@Override
		public void encode(SerializationContext context, UaEncoder encoder, PackMLCountDataType value)
				throws UaSerializationException {

			encoder.writeInt32(ID_FIELD, value.id);
			encoder.writeString(NAME_FIELD, value.name);
			encoder.writeStruct(UNIT_FIELD, value.unit, EUInformation.TYPE_ID);
			encoder.writeInt32(COUNT_FIELD, value.count);
			encoder.writeInt32(ACCCOUNT_FIELD, value.accCount);
		}
	}
}
