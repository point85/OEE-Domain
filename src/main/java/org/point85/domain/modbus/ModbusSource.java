package org.point85.domain.modbus;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.ModbusSourceDto;

@Entity
@DiscriminatorValue(DataSourceType.MODBUS_VALUE)

/**
 * This class represents a Modbus slave data source. The Modbus master
 * communicates with a slave identified by its unit id.
 *
 */
public class ModbusSource extends CollectorDataSource {
	public static final int DEFAULT_PORT = 502;
	public static final int DEFAULT_UNIT_ID = 0;

	// overloaded for transport name
	@Column(name = "END_PATH")
	private String transportName;

	@Transient
	private ModbusTransport transport;

	@Transient
	private ModbusEndpoint endpoint;

	/**
	 * Constructor
	 */
	public ModbusSource() {
		super();
		setDataSourceType(DataSourceType.MODBUS);
	}

	/**
	 * Constructor
	 * 
	 * @param name        Unique name for the source
	 * @param description Description of the source
	 */
	public ModbusSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.MODBUS);
	}

	public ModbusSource(ModbusSourceDto dto) {
		super(dto);
		setDataSourceType(DataSourceType.MODBUS);

		this.transportName = dto.getTransportName();
		this.transport = dto.getTransportName() != null ? ModbusTransport.valueOf(dto.getTransportName()) : null;
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		String[] tokens = id.split(":");
		if (tokens.length == 3) {
			setHost(tokens[0]);
			setPort(Integer.valueOf(tokens[1]));
			transportName = tokens[2];
		}
	}

	public ModbusTransport getTransport() {
		if (transport == null) {
			transport = ModbusTransport.valueOf(transportName);
		}
		return transport;
	}

	public void setTransport(ModbusTransport transport) {
		this.transport = transport;
		transportName = transport.name();
	}

	public ModbusEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(ModbusEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModbusSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getTransport());
	}
}
