package org.point85.domain.opc.ua;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;

public class OpcUaServerStatus {
	// state
	private ServerState state;

	// when server started
	private DateTime startTime;
	
	// build information
	private BuildInfo buildInfo;


	public OpcUaServerStatus() {

	}

	public ServerState getState() {
		return state;
	}

	public void setState(ServerState state) {
		this.state = state;
	}

	public DateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(DateTime startTime) {
		this.startTime = startTime;
	}

	public BuildInfo getBuildInfo() {
		return buildInfo;
	}

	public void setBuildInfo(BuildInfo buildInfo) {
		this.buildInfo = buildInfo;
	}

}
