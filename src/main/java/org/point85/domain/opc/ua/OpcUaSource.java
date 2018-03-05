package org.point85.domain.opc.ua;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.OPC_UA_VALUE)

//@NamedQueries({
		//@NamedQuery(name = OpcUaSource.UA_SRC_BY_NAME, query = "SELECT source FROM OpcUaSource source WHERE source.name = :name"), })

public class OpcUaSource extends DataSource {

	// queries
	//public static final String UA_SRC_BY_NAME = "OPCUA.ByName";

	private transient SecurityPolicy policy;

	private transient String endpointUrl;

	public OpcUaSource() {
		super();
		setDataSourceType(DataSourceType.OPC_UA);
	}

	public OpcUaSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.OPC_UA);
	}

	@Override
	public String getId() {
		return getEndpointUrl();
	}

	@Override
	public void setId(String id) {
		String[] tokens = id.split(":");
		if (tokens.length == 2) {
			setHost(tokens[0]);
			setPort(Integer.valueOf(tokens[1]));
		}
	}

	public String getPath() {
		return param1;
	}

	public void setPath(String path) {
		this.param1 = path;
	}

	public String getEndpointUrl() {
		if (endpointUrl == null) {
			// only TCP is supported
			endpointUrl = String.format("opc.tcp://%s:%s", getHost(), getPort());

			if (getPath() != null && getPath().length() > 0) {
				endpointUrl += "/" + getPath();
			}
		}
		return endpointUrl;
	}

	public SecurityPolicy getSecurityPolicy() {
		return policy;
	}

	public void setSecurityPolicy(SecurityPolicy policy) {
		this.policy = policy;
	}
}
