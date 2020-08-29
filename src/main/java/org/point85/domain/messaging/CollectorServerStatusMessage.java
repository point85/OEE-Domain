package org.point85.domain.messaging;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class CollectorServerStatusMessage extends ApplicationMessage {
	private static final double MB = 1024d * 1024d;

	private double usedMemory = 0.0;
	private double freeMemory = 0.0;
	private double processCpuLoad = 0.0;

	public CollectorServerStatusMessage(String senderHostName, String senderHostAddress) throws Exception {
		super(senderHostName, senderHostAddress, MessageType.STATUS);

		// calculate memory and CPU
		this.setMemoryUsage();
		this.setProcessCpuLoad();
	}

	private void setMemoryUsage() {
		// memory
		Runtime runtime = Runtime.getRuntime();
		usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / MB;
		freeMemory = runtime.freeMemory() / MB;
	}

	private void setProcessCpuLoad() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName oname = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);

		processCpuLoad = (Double) mbs.getAttribute(oname, "ProcessCpuLoad");
	}

	public double getUsedMemory() {
		return usedMemory;
	}

	public double getFreeMemory() {
		return freeMemory;
	}

	public double getSystemLoadAvg() {
		return processCpuLoad;
	}

}
