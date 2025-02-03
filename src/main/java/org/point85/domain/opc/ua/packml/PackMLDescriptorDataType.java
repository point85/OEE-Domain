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
 * The PackMLDescriptorDataType provides the PackML Parameter structure.
 */
public class PackMLDescriptorDataType implements UaStructure {
	// data type node id
	public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLDescriptorDataType));

	// encoding node id
	public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLDescriptorDataType_Encoding_DefaultBinary));

	// fields
	private static final String ID_FIELD = "ID";
	private static final String NAME_FIELD = "Name";
	private static final String UNIT_FIELD = "Unit";
	private static final String VALUE_FIELD = "Value";

	// A unique number assigned to the parameter
	private Integer id;

	// The name of the parameter
	private String name;

	// OPC UA engineering unit information
	private EUInformation unit;

	// This is the numeric value of the parameter
	private Float value;

	public PackMLDescriptorDataType() {
		this(null, null, null, null);
	}

	/**
	 * Constructor to populate all fields
	 * 
	 * @param id    A unique number assigned to the parameter
	 * @param name  The name of the parameter
	 * @param unit  OPC UA engineering unit information
	 * @param value This is the numeric value of the parameter
	 */
	public PackMLDescriptorDataType(Integer id, String name, EUInformation unit, Float value) {
		this.name = name;
		this.id = id;
		this.unit = unit;
		this.value = value;
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
		PackMLDescriptorDataType that = (PackMLDescriptorDataType) o;
		return value == that.value && Objects.equal(name, that.name) && Objects.equal(id, that.id) && unit == that.unit;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, id, value, unit);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID\t").append(getId());
		sb.append('\n').append("Name\t").append(getName());
		sb.append('\n').append("Unit\t").append("EuInformation");
		sb.append(PackMLUtils.euInformationToString(getUnit()));
		sb.append('\n').append("Value\t").append(getValue());
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public Integer getId() {
		return id;
	}

	public EUInformation getUnit() {
		return unit;
	}

	public Float getValue() {
		return value;
	}

	/**
	 * Binary encoding/decoding codec
	 */
	public static class Codec extends GenericDataTypeCodec<PackMLDescriptorDataType> {
		@Override
		public Class<PackMLDescriptorDataType> getType() {
			return PackMLDescriptorDataType.class;
		}

		@Override
		public PackMLDescriptorDataType decode(SerializationContext context, UaDecoder decoder)
				throws UaSerializationException {

			Integer id = decoder.readInt32(ID_FIELD);
			String name = decoder.readString(NAME_FIELD);
			EUInformation unit = (EUInformation) decoder.readStruct(UNIT_FIELD, EUInformation.TYPE_ID);
			Float value = decoder.readFloat(VALUE_FIELD);

			return new PackMLDescriptorDataType(id, name, unit, value);
		}

		@Override
		public void encode(SerializationContext context, UaEncoder encoder, PackMLDescriptorDataType value)
				throws UaSerializationException {

			encoder.writeInt32(ID_FIELD, value.id);
			encoder.writeString(NAME_FIELD, value.name);
			encoder.writeStruct(UNIT_FIELD, value.unit, EUInformation.TYPE_ID);
			encoder.writeFloat(VALUE_FIELD, value.value);
		}
	}
}
