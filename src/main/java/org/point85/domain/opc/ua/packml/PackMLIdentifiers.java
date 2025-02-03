package org.point85.domain.opc.ua.packml;

/**
 * Constants for PackML support
 */
public class PackMLIdentifiers {
	// NS0
	public static final String NS_URI_OPC_UA = "http://opcfoundation.org/UA/";

	// NS UA/PackML
	public static final String NS_URI_OPC_UA_PACKML = "http://opcfoundation.org/UA/PackML/";

	// encoding ids
	public static final int PackMLCountDataType_Encoding_DefaultBinary = 69;
	public static final int PackMLAlarmDataType_Encoding_DefaultBinary = 74;
	public static final int PackMLDescriptorDataType_Encoding_DefaultBinary = 77;
	public static final int PackMLIngredientsDataType_Encoding_DefaultBinary = 79;

	// data types
	public static final int PackMLCountDataType = 14;
	public static final int PackMLAlarmDataType = 15;
	public static final int PackMLDescriptorDataType = 16;
	public static final int PackMLIngredientsDataType = 17;

	// data type structure names
	public static final String PACKML_COUNT_STRUCTURE_NAME = "PackMLCountDataType";
	public static final String PACKML_ALARM_STRUCTURE_NAME = "PackMLAlarmDataType";
	public static final String PACKML_DESCRIPTER_STRUCTURE_NAME = "PackMLDescripterDataType";
	public static final String PACKML_INGREDIENTS_STRUCTURE_NAME = "PackMLIngredientsDataType";
}