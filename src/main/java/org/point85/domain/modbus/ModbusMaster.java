package org.point85.domain.modbus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.polling.PollingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.facade.ModbusUDPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * This class represents a Modbus master that reads and writes data from/to a
 * Modbus slave via a Modbus master. It periodically polls the slave for data.
 *
 */
public class ModbusMaster extends PollingClient {
	// logging utility
	private static final Logger logger = LoggerFactory.getLogger(ModbusMaster.class);

	// service handling the queried data
	private ModbusEventListener eventListener;

	// wrapped native Modbus master
	private AbstractModbusMaster nativeModbusMaster;

	// connection flag
	private boolean isConnected = false;

	public ModbusMaster(ModbusSource eventSource) {
		setDataSource(eventSource);
	}

	/**
	 * Constructor
	 * 
	 * @param eventListener  Callback listener {@link ModbusEventListener}
	 * @param eventSource    Modbus slave source {@link ModbusSource}
	 * @param sourceIds      List of slave source identifiers
	 * @param pollingPeriods List of polling periods for the slaves
	 */
	public ModbusMaster(ModbusEventListener eventListener, ModbusSource eventSource, List<String> sourceIds,
			List<Integer> pollingPeriods) {
		super(eventSource, sourceIds, pollingPeriods);
		this.eventListener = eventListener;
		setDataSource(eventSource);
	}

	@Override
	public void setDataSource(CollectorDataSource eventSource) {
		if (eventSource == null) {
			return;
		}
		super.setDataSource(eventSource);

		ModbusTransport transport = ((ModbusSource) eventSource).getTransport();
		String address = ((ModbusSource) eventSource).getHost();
		Integer port = ((ModbusSource) eventSource).getPort();

		switch (transport) {
		case SERIAL:
			nativeModbusMaster = new ModbusSerialMaster(new SerialParameters());

			if (logger.isInfoEnabled()) {
				logger.info("Created ModbusSerialMaster");
			}
			break;
		case TCP:
			nativeModbusMaster = new ModbusTCPMaster(address, port);

			if (logger.isInfoEnabled()) {
				logger.info("Created ModbusTCPMaster");
			}
			break;
		case UDP:
			nativeModbusMaster = new ModbusUDPMaster(address, port);

			if (logger.isInfoEnabled()) {
				logger.info("Created ModbusUDPMaster");
			}
			break;
		default:
			break;
		}
	}

	public AbstractModbusMaster getNativeModbusMaster() {
		return nativeModbusMaster;
	}

	/**
	 * Connect to the Modbus master data source
	 * 
	 * @throws Exception Exception
	 */
	public void connect() throws Exception {
		if (isConnected) {
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Connecting to modbus master: " + dataSource.getId());
		}

		// native connect
		nativeModbusMaster.connect();
		setConnected(true);
	}

	/**
	 * Disconnect from the Modbus master
	 */
	public void disconnect() {
		if (logger.isInfoEnabled()) {
			logger.info("Disconnecting from modbus master: " + dataSource.getId());
		}

		stopPolling();

		nativeModbusMaster.disconnect();
		setConnected(false);
	}

	/**
	 * Read the coil registers
	 * 
	 * @param unitId  Unit identifier
	 * @param address Starting register address
	 * @param count   Number of coils to read
	 * @return Array of boolean values
	 * @throws Exception Exception
	 */
	public boolean[] readCoils(int unitId, int address, int count) throws Exception {
		BitVector bv = nativeModbusMaster.readCoils(unitId, address, count);
		return ModbusUtils.bitVectorToBooleans(bv);
	}

	/**
	 * Read discrete registers
	 * 
	 * @param unitId  Unit identifier
	 * @param address Starting register address
	 * @param count   Number of discrete registers to read
	 * @return Array of boolean values
	 * @throws Exception Exception
	 */
	public boolean[] readDiscretes(int unitId, int address, int count) throws Exception {
		BitVector bv = nativeModbusMaster.readInputDiscretes(unitId, address, count);
		return ModbusUtils.bitVectorToBooleans(bv);
	}

	private byte[][] registersToBytes(InputRegister[] registers) {
		byte[][] byteArray = new byte[registers.length][2];

		for (int i = 0; i < registers.length; i++) {
			// 2 bytes each word
			byteArray[i] = registers[i].toBytes();

			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Read word[" + i + "]: %02X%02X", byteArray[i][0], byteArray[i][1]));
			}
		}
		return byteArray;
	}

	/**
	 * Read input registers
	 * 
	 * @param unitId  Unit identifier
	 * @param address Starting register address
	 * @param count   Number of registers to read
	 * @return Array of 2-byte words
	 * @throws Exception Exception
	 */
	public byte[][] readInputRegisters(int unitId, int address, int count) throws Exception {
		InputRegister[] registers = nativeModbusMaster.readInputRegisters(unitId, address, count);
		return registersToBytes(registers);
	}

	/**
	 * Read holding registers
	 * 
	 * @param unitId  Unit identifier
	 * @param address Starting register address
	 * @param count   Number of registers to read
	 * @return Array of 2-byte words
	 * @throws Exception Exception
	 */
	public byte[][] readHoldingRegisters(int unitId, int address, int count) throws Exception {
		Register[] registers = nativeModbusMaster.readMultipleRegisters(unitId, address, count);
		return registersToBytes(registers);
	}

	/**
	 * Write to the coil register
	 * 
	 * @param unitId  Unit identifier
	 * @param address Register address
	 * @param bit     Value to write
	 * @throws Exception Exception
	 */
	public void writeCoil(int unitId, int address, boolean bit) throws Exception {
		boolean[] bits = new boolean[1];
		bits[0] = bit;
		writeCoils(unitId, address, bits);
	}

	/**
	 * Write multiple values to the coil registers
	 * 
	 * @param unitId  Unit identifier
	 * @param address Starting register address
	 * @param bits    Values to write
	 * @throws Exception Exception
	 */
	public void writeCoils(int unitId, int address, boolean[] bits) throws Exception {
		BitVector bv = new BitVector(bits.length);

		for (int i = 0; i < bits.length; i++) {
			bv.setBit(i, bits[i]);
		}
		nativeModbusMaster.writeMultipleCoils(unitId, address, bv);
	}

	@Override
	protected void onPoll(String sourceId) {
		if (logger.isInfoEnabled()) {
			logger.info("Polling Modbus source with source id " + sourceId);
		}

		ModbusEndpoint endpoint = new ModbusEndpoint(sourceId);

		List<ModbusVariant> values = null;

		try {
			values = readDataSource(endpoint);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return;
		}

		// call the listener
		ModbusEvent event = new ModbusEvent((ModbusSource) dataSource, sourceId, OffsetDateTime.now(), values);
		eventListener.resolveModbusEvents(event);
	}

	private List<ModbusVariant> readDiscreteRegisters(ModbusEndpoint slaveSource) throws Exception {
		Integer unitId = slaveSource.getUnitId();
		Integer valueCount = slaveSource.getValueCount();
		Integer address = slaveSource.getRegisterAddress();
		ModbusRegisterType type = slaveSource.getRegisterType();

		List<ModbusVariant> values = new ArrayList<>(valueCount);

		boolean[] data = null;

		if (type.equals(ModbusRegisterType.COIL)) {
			data = readCoils(unitId, address, valueCount);
		} else if (type.equals(ModbusRegisterType.DISCRETE)) {
			data = readDiscretes(unitId, address, valueCount);
		}

		if (data != null) {
			for (boolean bool : data) {
				values.add(new ModbusVariant(bool));
			}
		}
		return values;
	}

	/**
	 * Write the variant value to the holding register
	 * 
	 * @param endpoint {@link ModbusEndpoint}
	 * @param value    {@link ModbusVariant}
	 * @throws Exception Exception
	 */
	public void writeHoldingRegister(ModbusEndpoint endpoint, ModbusVariant value) throws Exception {
		List<ModbusVariant> values = new ArrayList<>(1);
		values.add(value);
		writeHoldingRegisters(endpoint, values);
	}

	/**
	 * Write the variant values to the holding registers
	 * 
	 * @param endpoint {@link ModbusEndpoint}
	 * @param values   List of {@link ModbusVariant}
	 * @throws Exception Exception
	 */
	public void writeHoldingRegisters(ModbusEndpoint endpoint, List<ModbusVariant> values) throws Exception {
		if (values.isEmpty()) {
			return;
		}

		Integer unitId = endpoint.getUnitId();
		Integer address = endpoint.getRegisterAddress();
		ModbusDataType dataType = values.get(0).getDataType();
		Boolean reverse = endpoint.isReverseEndianess();

		Register[] registers = null;

		switch (dataType) {
		case BYTE_HIGH:
		case BYTE_LOW: {
			// 1 byte high or low
			int index = reverse ? 0 : 1;

			if (dataType.equals(ModbusDataType.BYTE_HIGH)) {
				index = reverse ? 1 : 0;
			}

			registers = new Register[values.size()];

			for (int i = 0; i < values.size(); i++) {
				Byte value = values.get(i).getByte();

				byte[] bytes = new byte[2];
				bytes[index] = value;

				registers[i] = new SimpleRegister(bytes[0], bytes[1]);

				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Writing words for " + dataType.name() + "[" + value + "] %02X%02X",
							bytes[0], bytes[1]) + ", Address: " + address);
				}
			}
			break;
		}

		case DOUBLE: {
			// double (8 bytes)
			registers = new Register[values.size() * 4];

			for (int i = 0; i < values.size(); i++) {
				Double value = (Double) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromDouble(value);

				int offset = 4 * i;
				if (reverse) {
					registers[offset] = new SimpleRegister(bytes[7], bytes[6]);
					registers[offset + 1] = new SimpleRegister(bytes[5], bytes[4]);
					registers[offset + 2] = new SimpleRegister(bytes[3], bytes[2]);
					registers[offset + 3] = new SimpleRegister(bytes[1], bytes[0]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format(
								"Writing words for DOUBLE[" + value + "] %02X%02X %02X%02X %02X%02X %02X%02X", bytes[7],
								bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]) + ", Address: "
								+ address);
					}

				} else {
					registers[offset] = new SimpleRegister(bytes[0], bytes[1]);
					registers[offset + 1] = new SimpleRegister(bytes[2], bytes[3]);
					registers[offset + 2] = new SimpleRegister(bytes[4], bytes[5]);
					registers[offset + 3] = new SimpleRegister(bytes[6], bytes[7]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format(
								"Writing words for DOUBLE[" + value + "] %02X%02X %02X%02X %02X%02X %02X%02X", bytes[0],
								bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]) + ", Address: "
								+ address);
					}
				}
			}
			break;
		}

		case INT16: {
			// short (2 bytes)
			registers = new Register[values.size()];

			for (int i = 0; i < values.size(); i++) {
				Short value = (Short) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromShort(value);

				if (reverse) {
					registers[i] = new SimpleRegister(bytes[1], bytes[0]);

					if (logger.isTraceEnabled()) {
						logger.trace(
								String.format("Writing words for INT16[" + value + "] %02X%02X", bytes[1], bytes[0])
										+ ", Address: " + address);
					}
				} else {
					registers[i] = new SimpleRegister(bytes[0], bytes[1]);

					if (logger.isTraceEnabled()) {
						logger.trace(
								String.format("Writing words for INT16[" + value + "] %02X%02X", bytes[0], bytes[1])
										+ ", Address: " + address);
					}
				}
			}
			break;
		}

		case UINT16: {
			// promoted to int (4 bytes)
			registers = new Register[values.size()];

			for (int i = 0; i < values.size(); i++) {
				Integer value = (Integer) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromInteger(value);

				if (reverse) {
					registers[i] = new SimpleRegister(bytes[3], bytes[2]);

					if (logger.isTraceEnabled()) {
						logger.trace(
								String.format("Writing words for UINT16[" + value + "] %02X%02X", bytes[3], bytes[2])
										+ ", Address: " + address);
					}
				} else {
					registers[i] = new SimpleRegister(bytes[2], bytes[3]);

					if (logger.isTraceEnabled()) {
						logger.trace(
								String.format("Writing words for UINT16[" + value + "] %02X%02X", bytes[2], bytes[3])
										+ ", Address: " + address);
					}
				}
			}
			break;
		}

		case INT32: {
			// int (4 bytes)
			registers = new Register[values.size() * 2];

			for (int i = 0; i < values.size(); i++) {
				Integer value = (Integer) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromInteger(value);

				int offset = 2 * i;
				if (reverse) {
					registers[offset] = new SimpleRegister(bytes[3], bytes[2]);
					registers[offset + 1] = new SimpleRegister(bytes[1], bytes[0]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for INT32[" + value + "] %02X%02X %02X%02X", bytes[3],
								bytes[2], bytes[1], bytes[0]) + ", Address: " + address);
					}
				} else {
					registers[offset] = new SimpleRegister(bytes[0], bytes[1]);
					registers[offset + 1] = new SimpleRegister(bytes[2], bytes[3]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for INT32[" + value + "] %02X%02X %02X%02X", bytes[0],
								bytes[1], bytes[2], bytes[3]) + ", Address: " + address);
					}
				}
			}
			break;
		}

		case INT64: {
			// long (8 bytes)
			registers = new Register[values.size() * 4];

			for (int i = 0; i < values.size(); i++) {
				Long value = (Long) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromLong(value);

				int offset = 4 * i;
				if (reverse) {
					registers[offset] = new SimpleRegister(bytes[7], bytes[6]);
					registers[offset + 1] = new SimpleRegister(bytes[5], bytes[4]);
					registers[offset + 2] = new SimpleRegister(bytes[3], bytes[2]);
					registers[offset + 3] = new SimpleRegister(bytes[1], bytes[0]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format(
								"Writing words for INT64[" + value + "] %02X%02X %02X%02X %02X%02X %02X%02X", bytes[7],
								bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]) + ", Address: "
								+ address);
					}
				} else {
					registers[offset] = new SimpleRegister(bytes[0], bytes[1]);
					registers[offset + 1] = new SimpleRegister(bytes[2], bytes[3]);
					registers[offset + 2] = new SimpleRegister(bytes[4], bytes[5]);
					registers[offset + 3] = new SimpleRegister(bytes[6], bytes[7]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format(
								"Writing words for INT64[" + value + "] %02X%02X %02X%02X %02X%02X %02X%02X", bytes[0],
								bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]) + ", Address: "
								+ address);
					}
				}
			}
			break;
		}

		case SINGLE: {
			// float (4 bytes)
			registers = new Register[values.size() * 2];

			for (int i = 0; i < values.size(); i++) {
				Float value = (Float) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromFloat(value);

				int offset = 2 * i;
				if (reverse) {
					registers[offset] = new SimpleRegister(bytes[3], bytes[2]);
					registers[offset + 1] = new SimpleRegister(bytes[1], bytes[0]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for SINGLE[" + value + "] %02X%02X %02X%02X",
								bytes[3], bytes[2], bytes[1], bytes[0]) + ", Address: " + address);
					}
				} else {
					registers[offset] = new SimpleRegister(bytes[0], bytes[1]);
					registers[offset + 1] = new SimpleRegister(bytes[2], bytes[3]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for SINGLE[" + value + "] %02X%02X %02X%02X",
								bytes[0], bytes[1], bytes[2], bytes[3]) + ", Address: " + address);
					}
				}
			}
			break;
		}

		case STRING: {
			// only one string
			String value = values.get(0).getString();

			if (logger.isTraceEnabled()) {
				logger.trace("Writing string '" + value + "', Address: " + address);
			}

			int wordCount = value.length() / 2;

			if (Math.floorMod(value.length(), 2) != 0) {
				wordCount++;
			}

			registers = new Register[wordCount];
			byte[] bytes = value.getBytes();

			int j = 0;
			for (int i = 0; i < wordCount; i++) {
				byte next = 0x00;

				if ((j + 1) < value.length()) {
					next = bytes[j + 1];
				}

				if (reverse) {
					registers[i] = new SimpleRegister(next, bytes[j]);
				} else {
					registers[i] = new SimpleRegister(bytes[j], next);
				}
				j += 2;
			}
			break;
		}

		case UINT32: {
			// promoted to long (8 bytes)
			registers = new Register[values.size() * 2];

			for (int i = 0; i < values.size(); i++) {
				Long value = (Long) values.get(i).getNumber();
				byte[] bytes = ModbusUtils.fromLong(value);

				int offset = 2 * i;
				if (reverse) {
					registers[offset] = new SimpleRegister(bytes[7], bytes[6]);
					registers[offset + 1] = new SimpleRegister(bytes[5], bytes[4]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for UINT32[" + i + "] %02X%02X %02X%02X", bytes[7],
								bytes[6], bytes[5], bytes[4]) + ", Address: " + address);
					}
				} else {
					registers[offset] = new SimpleRegister(bytes[4], bytes[5]);
					registers[offset + 1] = new SimpleRegister(bytes[6], bytes[7]);

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Writing words for UINT32[" + i + "] %02X%02X %02X%02X", bytes[4],
								bytes[5], bytes[6], bytes[7]) + ", Address: " + address);
					}
				}
			}
			break;
		}

		default:
			return;
		}

		// write to the registers
		nativeModbusMaster.writeMultipleRegisters(unitId, address, registers);
	}

	private List<ModbusVariant> readValueRegisters(ModbusEndpoint slaveSource) throws Exception {
		Integer unitId = slaveSource.getUnitId();
		Integer valueCount = slaveSource.getValueCount();
		Integer address = slaveSource.getRegisterAddress();
		Boolean reverse = slaveSource.isReverseEndianess();
		ModbusRegisterType type = slaveSource.getRegisterType();
		ModbusDataType dataType = slaveSource.getDataType();

		List<ModbusVariant> values = new ArrayList<>(valueCount);

		switch (dataType) {
		case DOUBLE: {
			// Double (8 bytes)
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount * 4);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount * 4);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Double value = ModbusUtils.toDouble(data[offset], data[offset + 1], data[offset + 2], data[offset + 3],
						reverse);
				offset = offset + 4;

				if (logger.isTraceEnabled()) {
					logger.trace("Double value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case BYTE_LOW:
		case BYTE_HIGH: {
			int index = reverse ? 0 : 1;

			if (dataType.equals(ModbusDataType.BYTE_HIGH)) {
				index = reverse ? 1 : 0;
			}

			// Byte
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Byte value = Byte.valueOf(data[offset][index]);
				offset++;

				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Byte value is: %02X (%d)", value, value));
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case INT16: {
			// Short
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Short value = ModbusUtils.toShort(data[offset], reverse);
				offset++;

				if (logger.isTraceEnabled()) {
					logger.trace("Short value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case INT32: {
			// Integer
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount * 2);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount * 2);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Integer value = ModbusUtils.toInteger(data[offset], data[offset + 1], reverse);
				offset = offset + 2;

				if (logger.isTraceEnabled()) {
					logger.trace("Integer value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case INT64: {
			// Long
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount * 4);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount * 4);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Long value = ModbusUtils.toLong(data[offset], data[offset + 1], data[offset + 2], data[offset + 3],
						reverse);
				offset = offset + 4;

				if (logger.isTraceEnabled()) {
					logger.trace("Long value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case SINGLE: {
			// Float
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount * 2);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount * 2);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Float value = ModbusUtils.toFloat(data[offset], data[offset + 1], reverse);
				offset = offset + 2;

				if (logger.isTraceEnabled()) {
					logger.trace("Float value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case UINT16: {
			// Integer
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount);
			}

			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Integer value = ModbusUtils.toUnsignedShort(data[offset], reverse);
				offset++;

				if (logger.isTraceEnabled()) {
					logger.trace("Integer value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case UINT32: {
			// Long
			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, valueCount * 2);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, valueCount * 2);
			}
			int offset = 0;
			for (int i = 0; i < valueCount; i++) {
				Long value = ModbusUtils.toUnsignedInteger(data[offset], data[offset + 1], reverse);
				offset = offset + 2;

				if (logger.isTraceEnabled()) {
					logger.trace("Long value is: " + value);
				}

				values.add(new ModbusVariant(dataType, value));
			}
			break;
		}

		case STRING: {
			// 1 byte per character, valueCount = number of characters
			// one string only, no arrays
			values = new ArrayList<>(1);

			int wordCount = valueCount / 2;

			if (Math.floorMod(valueCount, 2) != 0) {
				wordCount++;
			}

			byte[][] data = null;

			if (type.equals(ModbusRegisterType.HOLDING_REGISTER)) {
				data = readHoldingRegisters(unitId, address, wordCount);
			} else if (type.equals(ModbusRegisterType.INPUT_REGISTER)) {
				data = readInputRegisters(unitId, address, wordCount);
			} else {
				return values;
			}

			String value = ModbusUtils.toUTF8String(data, valueCount, reverse);

			if (logger.isTraceEnabled()) {
				logger.trace("String value is: " + value);
			}

			values.add(new ModbusVariant(value));

			break;
		}

		default:
			break;
		}
		return values;
	}

	/**
	 * Read the slave register values
	 * 
	 * @param source {@link ModbusSource}
	 * @return List of {@link ModbusVariant} values
	 * @throws Exception Exception
	 */
	public List<ModbusVariant> readDataSource(ModbusEndpoint source) throws Exception {

		List<ModbusVariant> values = null;

		switch (source.getRegisterType()) {
		case COIL:
		case DISCRETE:
			values = readDiscreteRegisters(source);
			break;

		case HOLDING_REGISTER:
		case INPUT_REGISTER:
			values = readValueRegisters(source);
			break;

		default:
			break;
		}
		return values;
	}

	/**
	 * Write the values to the slave endpoint
	 * 
	 * @param endpoint {@link ModbusEndpoint}
	 * @param values   List of {@link ModbusVariant}
	 * @throws Exception Exception
	 */
	public void writeDataSource(ModbusEndpoint endpoint, List<ModbusVariant> values) throws Exception {
		switch (endpoint.getRegisterType()) {
		case COIL: {
			BitVector bv = new BitVector(values.size());

			for (int i = 0; i < values.size(); i++) {
				bv.setBit(i, values.get(i).getBoolean());
			}
			nativeModbusMaster.writeMultipleCoils(endpoint.getUnitId(), endpoint.getRegisterAddress(), bv);
			break;
		}

		case HOLDING_REGISTER: {
			writeHoldingRegisters(endpoint, values);
			break;
		}

		case INPUT_REGISTER:
		case DISCRETE:
		default:
			break;
		}
	}

	public boolean isConnected() {
		return isConnected;
	}

	private void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
}