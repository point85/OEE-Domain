/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.domain.opc.da;

import java.time.ZonedDateTime;

import org.openscada.opc.dcom.da.OPCSERVERSTATUS;

/**
 *
 * @author krandall
 */
public class OpcDaServerStatus {

	private OPCSERVERSTATUS serverStatus;

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

	public ZonedDateTime getStartTime() {
		return (serverStatus != null) ? DaOpcClient.fromFiletime(serverStatus.getStartTime()) : null;
	}

	public ZonedDateTime getCurrentTime() {
		return (serverStatus != null) ? DaOpcClient.fromFiletime(serverStatus.getCurrentTime()) : null;
	}

	public ZonedDateTime getLastUpdateTime() {
		return (serverStatus != null) ? DaOpcClient.fromFiletime(serverStatus.getLastUpdateTime()) : null;
	}
}
