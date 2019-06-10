package org.point85.domain.modbus;

/**
 * This class identifies the data to be read or written from/to a Modbus slave.
 *
 */
public class ModbusEndpoint {
	private static final String DELIMITER = ",";
	private transient Integer unitId;

	private transient ModbusRegisterType registerType;

	private transient ModbusDataType dataType;

	private transient Integer registerAddress;

	private transient Integer valueCount;

	private transient Boolean reverseEndianess;

	public ModbusEndpoint() {

	}

	/**
	 * Constructor with the source information
	 * 
	 * @param sourceId Resolver souce id
	 */
	public ModbusEndpoint(String sourceId) {
		String[] tokens = sourceId.split(DELIMITER);

		unitId = Integer.valueOf(tokens[0].trim());
		registerType = ModbusRegisterType.fromDatabaseString(tokens[1].trim());
		registerAddress = Integer.valueOf(tokens[2].trim());
		valueCount = Integer.valueOf(tokens[3].trim());
		dataType = ModbusDataType.valueOf(tokens[4].trim());
		reverseEndianess = Boolean.valueOf(tokens[5].trim());
	}

	/**
	 * Create the sourceId
	 * 
	 * @return Source identifer
	 */
	public String buildSourceId() {
		StringBuilder sb = new StringBuilder();

		sb.append(unitId).append(DELIMITER).append(registerType.toDatabaseString()).append(DELIMITER)
				.append(registerAddress).append(DELIMITER).append(valueCount).append(DELIMITER).append(dataType.name())
				.append(DELIMITER).append(reverseEndianess);
		return sb.toString();
	}

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer id) {
		this.unitId = id;
	}

	public ModbusRegisterType getRegisterType() {
		return registerType;
	}

	public void setRegisterType(ModbusRegisterType type) {
		this.registerType = type;
	}

	public Integer getRegisterAddress() {
		return registerAddress;
	}

	public void setRegisterAddress(Integer address) {
		this.registerAddress = address;
	}

	public Integer getValueCount() {
		return valueCount;
	}

	public void setValueCount(Integer count) {
		this.valueCount = count;
	}

	public ModbusDataType getDataType() {
		return dataType;
	}

	public void setModbusDataType(ModbusDataType type) {
		this.dataType = type;
	}

	public Boolean isReverseEndianess() {
		return reverseEndianess;
	}

	public void setReverseEndianess(Boolean value) {
		this.reverseEndianess = value;
	}
}
