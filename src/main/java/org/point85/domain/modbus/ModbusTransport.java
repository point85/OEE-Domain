package org.point85.domain.modbus;

import org.point85.domain.i18n.DomainLocalizer;

/**
 * The transports supported by the Modbus master. A default serial connection is
 * used (9600 baud, 8 data bits, 1 stop bit, no parity).
 *
 */
public enum ModbusTransport {
	TCP, UDP, SERIAL;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case TCP:
			key = "tcp.type";
			break;

		case UDP:
			key = "udp.type";
			break;

		case SERIAL:
			key = "serial.type";
			break;

		default:
			break;
		}
		return DomainLocalizer.instance().getLangString(key);
	}
}
