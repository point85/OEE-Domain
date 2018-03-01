package org.point85.domain.opc.ua;

import java.util.List;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

public interface OpcUaAsynchListener {
	void onOpcUaRead(List<DataValue> dataValues);

	void onOpcUaWrite(List<StatusCode> statusCodes);

	void onOpcUaSubscription(DataValue dataValue, UaMonitoredItem item);
}
