package org.point85.domain.dto;

import org.point85.domain.modbus.ModbusSource;

public class ModbusSourceDto extends CollectorDataSourceDto {
	private String transportName;

	public ModbusSourceDto(ModbusSource source) {
		super(source);

		setTransportName(source.getTransport() != null ? source.getTransport().name() : null);
	}

	public String getTransportName() {
		return transportName;
	}

	public void setTransportName(String transportName) {
		this.transportName = transportName;
	}
}
