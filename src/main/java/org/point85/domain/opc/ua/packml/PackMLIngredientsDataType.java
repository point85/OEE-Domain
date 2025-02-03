package org.point85.domain.opc.ua.packml;

import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.serialization.UaDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaEncoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaStructure;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.GenericDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;

import com.google.common.base.Objects;

/**
 * The PackMLIngredientsDataType provides the PackML Parameter structure. 
 */
public class PackMLIngredientsDataType implements UaStructure {
	// data type node id
	public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse(String.format("nsu=%s;i=%d",
			PackMLIdentifiers.NS_URI_OPC_UA_PACKML, PackMLIdentifiers.PackMLIngredientsDataType));

	// encoding node id
	public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId
			.parse(String.format("nsu=%s;i=%d", PackMLIdentifiers.NS_URI_OPC_UA_PACKML,
					PackMLIdentifiers.PackMLIngredientsDataType_Encoding_DefaultBinary));

	// fields
	private static final String ID_FIELD = "IngredientID";
	private static final String PARAMETERS_FIELD = "Parameter";

	// A unique number assigned to the ingredient.
	private Integer ingredientID;

	// The array of Parameters that correspond to the ingredient
	private PackMLDescriptorDataType[] parameters;

	public PackMLIngredientsDataType() {
		this(null, null);
	}

	/**
	 * Constructor to populate all fields
	 * @param ingredientID A unique number assigned to the ingredient
	 * @param parameters The array of {@link PackMLDescriptorDataType} parameters that correspond to the ingredient
	 */
	public PackMLIngredientsDataType(Integer ingredientID, PackMLDescriptorDataType[] parameters) {
		this.ingredientID = ingredientID;
		this.parameters = parameters;
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
		PackMLIngredientsDataType that = (PackMLIngredientsDataType) o;
		return ingredientID == that.ingredientID;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ingredientID);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID\t").append(getIngredientID());
		sb.append('\n').append("Parameters");
		sb.append(PackMLUtils.parametersToString(getParameters()));
		return sb.toString();
	}

	public Integer getIngredientID() {
		return ingredientID;
	}

	public PackMLDescriptorDataType[] getParameters() {
		return parameters;
	}

	/**
	 * Binary encoding/decoding codec
	 */
	public static class Codec extends GenericDataTypeCodec<PackMLIngredientsDataType> {
		@Override
		public Class<PackMLIngredientsDataType> getType() {
			return PackMLIngredientsDataType.class;
		}

		@Override
		public PackMLIngredientsDataType decode(SerializationContext context, UaDecoder decoder)
				throws UaSerializationException {
			Integer id = decoder.readInt32(ID_FIELD);
			PackMLDescriptorDataType[] descriptors = (PackMLDescriptorDataType[]) decoder
					.readStructArray(PARAMETERS_FIELD, PackMLDescriptorDataType.TYPE_ID);

			return new PackMLIngredientsDataType(id, descriptors);
		}

		@Override
		public void encode(SerializationContext context, UaEncoder encoder, PackMLIngredientsDataType value)
				throws UaSerializationException {

			encoder.writeInt32(ID_FIELD, value.ingredientID);
			encoder.writeStructArray(PARAMETERS_FIELD, value.parameters, PackMLDescriptorDataType.TYPE_ID);
		}
	}
}
