/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.domain.opc.da;

import java.time.OffsetDateTime;

import org.openscada.opc.dcom.da.OPCSERVERSTATUS;
import org.point85.domain.DomainUtils;

public class OpcDaServerStatus {

	private final OPCSERVERSTATUS serverStatus;

	public OpcDaServerStatus(OPCSERVERSTATUS status) {
		this.serverStatus = status;
	}

	public String getServerState() {
		return (serverStatus != null) ? serverStatus.getServerState().toString() : "";
	}

	public String getVendorInfo() {
		return (serverStatus != null) ? serverStatus.getVendorInfo() : "";
	}

	public String getVersion() {
		String buildNumber = "";

		if (serverStatus != null) {
			buildNumber = String.format("%d.%d.%d", serverStatus.getMajorVersion(), serverStatus.getMinorVersion(),
					serverStatus.getBuildNumber());
		}
		return buildNumber;
	}

	public int getBandWidth() {
		return (serverStatus != null) ? serverStatus.getBandWidth() : 0;
	}

	public int getGroupCount() {
		return (serverStatus != null) ? serverStatus.getGroupCount() : 0;
	}

	public OffsetDateTime getStartTime() {
		return (serverStatus != null) ? DomainUtils.fromFiletime(serverStatus.getStartTime()) : null;
	}

	public OffsetDateTime getCurrentTime() {
		return (serverStatus != null) ? DomainUtils.fromFiletime(serverStatus.getCurrentTime()) : null;
	}

	public OffsetDateTime getLastUpdateTime() {
		return (serverStatus != null) ? DomainUtils.fromFiletime(serverStatus.getLastUpdateTime()) : null;
	}
}
