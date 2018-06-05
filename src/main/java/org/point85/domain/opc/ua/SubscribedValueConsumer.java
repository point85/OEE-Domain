package org.point85.domain.opc.ua;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

public class SubscribedValueConsumer implements Consumer<DataValue> {

	private final UaMonitoredItem item;

	private final UaSubscription subscription;

	// asynch listeners
	private final List<OpcUaAsynchListener> asynchReceivers = new ArrayList<>();

	public SubscribedValueConsumer(UaSubscription subscription, UaMonitoredItem item) {
		this.item = item;
		this.subscription = subscription;
	}

	@Override
	public void accept(DataValue dataValue) {
		for (OpcUaAsynchListener listener : asynchReceivers) {
				listener.onOpcUaSubscription(dataValue, item);
		}
	}

	public UaMonitoredItem getItem() {
		return item;
	}

	public void addAsynchListener(OpcUaAsynchListener listener) {
		asynchReceivers.add(listener);
	}

	public void removeAsynchListener(OpcUaAsynchListener listener) {
		asynchReceivers.remove(listener);
	}

	public UaSubscription getSubscription() {
		return subscription;
	}

}