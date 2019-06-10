package org.point85.domain.modbus;

/**
 * 
 * Callback with Data from polling a Modbus slave
 *
 */
public interface ModbusEventListener {
	void resolveModbusEvents(ModbusEvent event);
}
