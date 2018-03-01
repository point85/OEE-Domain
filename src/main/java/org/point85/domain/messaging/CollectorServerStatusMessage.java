package org.point85.domain.messaging;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class CollectorServerStatusMessage extends ApplicationMessage {
	private static final double MB = 1024 * 1024;

	private double usedMemory = 0.0;
	private double freeMemory = 0.0;
	private double processCpuLoad = 0.0;

	public CollectorServerStatusMessage(String senderHostName,  String senderHostAddress) {
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

	private void setProcessCpuLoad() {
		try {
			//OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
			//systemLoadAvg = bean.getSystemLoadAverage();
			
			
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();			
			ObjectName oname = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
			
			processCpuLoad = (Double) mbs.getAttribute(oname, "ProcessCpuLoad");
			
			//System.out.println(cpuLoad);

			/*
			AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

			if (list.isEmpty()) {
				return;
			}

			Attribute att = (Attribute) list.get(0);
			Double value = (Double) att.getValue();

			// usually takes a couple of seconds before we get real values
			if (value != -1.0) {
				// returns a percentage value with 1 decimal point precision
				setProcessCpuLoad((int) ((long) (value * 1000) / 10.0));
			}
			*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
