
package org.point85.domain.opc.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.EventHandler;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.Result;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.da.IOPCDataCallback;
import org.openscada.opc.dcom.da.OPCDATASOURCE;
import org.openscada.opc.dcom.da.OPCITEMDEF;
import org.openscada.opc.dcom.da.OPCITEMRESULT;
import org.openscada.opc.dcom.da.OPCITEMSTATE;
import org.openscada.opc.dcom.da.ValueData;
import org.openscada.opc.dcom.da.WriteRequest;
import org.openscada.opc.dcom.da.impl.OPCGroupStateMgt;
import org.openscada.opc.dcom.da.impl.OPCItemMgt;
import org.openscada.opc.dcom.da.impl.OPCSyncIO;
import org.point85.domain.i18n.DomainLocalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcDaMonitoredGroup {

	// logging utility
	private static final Logger logger = LoggerFactory.getLogger(OpcDaMonitoredGroup.class);

	private final OPCGroupStateMgt groupManager;
	private final Map<Integer, OpcDaMonitoredItem> itemMap = new HashMap<>();
	private EventHandler eventHandler;
	private final Random intGenerator = new Random(System.currentTimeMillis());
	private final int clientHandle;
	private Integer[] serverHandles;

	// list of data change listeners
	private final List<OpcDaDataChangeListener> listeners;

	public OpcDaMonitoredGroup(OPCGroupStateMgt groupManager, int clientHandle) {
		this.groupManager = groupManager;
		this.clientHandle = clientHandle;
		listeners = Collections.synchronizedList(new ArrayList<OpcDaDataChangeListener>());
	}

	public String getName() {
		String name = null;
		try {
			name = getGroupManager().getState().getName();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return name;
	}

	public int getClientHandle() {
		return clientHandle;
	}

	public void startMonitoring() throws Exception {
		if (eventHandler == null) {
			eventHandler = groupManager.attach(new GroupAsynchReadCallback());
		}
	}

	public void stopMonitoring() throws Exception {
		if (eventHandler == null) {
			return;
		}
		eventHandler.detach();
		eventHandler = null;

		// de-activate server handles
		getGroupManager().getItemManagement().setActiveState(false, serverHandles);

		// remove handles
		getGroupManager().getItemManagement().remove(serverHandles);
	}

	public void addItems(OpcDaBrowserLeaf[] itemIds, boolean isActive) throws Exception {
		OPCItemMgt itemManager = groupManager.getItemManagement();
		Collection<OPCITEMDEF> opcItemDefs = new ArrayList<>(itemIds.length);
		serverHandles = new Integer[itemIds.length];
		Integer[] clientHandles = new Integer[itemIds.length];

		for (int i = 0; i < itemIds.length; i++) {
			OPCITEMDEF opcItemDef = new OPCITEMDEF();
			opcItemDef.setItemID(itemIds[i].getItemId());

			clientHandles[i] = intGenerator.nextInt();
			opcItemDef.setClientHandle(clientHandles[i]);
			opcItemDefs.add(opcItemDef);
		}

		OPCITEMDEF[] itemDefArray = opcItemDefs.toArray(new OPCITEMDEF[opcItemDefs.size()]);

		// validate
		KeyedResultSet<OPCITEMDEF, OPCITEMRESULT> result = itemManager.validate(itemDefArray);

		for (KeyedResult<OPCITEMDEF, OPCITEMRESULT> resultEntry : result) {
			if (resultEntry.isFailed()) {
				throw new Exception(DomainLocalizer.instance().getErrorString("validation.failed",
						resultEntry.getKey().getItemID(), String.format("%08X", resultEntry.getErrorCode())));
			}
		}

		// add them to the group
		result = itemManager.add(itemDefArray);

		int i = 0;
		for (KeyedResult<OPCITEMDEF, OPCITEMRESULT> resultEntry : result) {
			OPCITEMDEF itemDef = resultEntry.getKey();
			OPCITEMRESULT itemResult = resultEntry.getValue();

			if (resultEntry.isFailed()) {

				throw new Exception(DomainLocalizer.instance().getErrorString("add.item.failed",
						String.format("%08X", resultEntry.getValue().getServerHandle()),
						String.format("%d", resultEntry.getValue().getCanonicalDataType()),
						String.format("%d", resultEntry.getValue().getAccessRights()),
						String.format("%d", resultEntry.getValue().getReserved())));
			} else {
				String pathName = itemIds[i].getPathName();
				OpcDaMonitoredItem opcDaItem = new OpcDaMonitoredItem(itemDef, itemResult);
				opcDaItem.setPathName(pathName);
				opcDaItem.setGroup(this);
				Integer serverHandle = Integer.valueOf(itemResult.getServerHandle());
				serverHandles[i] = serverHandle;
				itemMap.put(clientHandles[i], opcDaItem);
				i++;
			}
		}

		// set them active
		ResultSet<Integer> resultSet = itemManager.setActiveState(isActive, serverHandles);

		for (Result<Integer> resultEntry : resultSet) {
			if (resultEntry.getErrorCode() != 0) {
				throw new Exception(DomainLocalizer.instance().getErrorString("set.active.failed",
						String.format("%08X", resultEntry.getValue()),
						String.format("%08X", resultEntry.getErrorCode())));
			}
		}

		// set client handles
		ResultSet<Integer> handleSet = itemManager.setClientHandles(serverHandles, clientHandles);

		for (Result<Integer> resultEntry : handleSet) {
			if (resultEntry.getErrorCode() != 0) {
				throw new Exception(DomainLocalizer.instance().getErrorString("set.handles.failed",
						String.format("%08X", resultEntry.getValue()),
						String.format("%08X", resultEntry.getErrorCode())));
			}
		}
	}

	public void removeItems(OpcDaMonitoredItem[] items) throws Exception {
		Integer[] removeServerHandles = new Integer[items.length];

		for (int i = 0; i < items.length; i++) {
			removeServerHandles[i] = items[i].getItemResult().getServerHandle();
		}

		groupManager.getItemManagement().remove(removeServerHandles);

		for (OpcDaMonitoredItem item : items) {
			Integer key = Integer.valueOf(item.getItemDef().getClientHandle());
			itemMap.remove(key);
		}
	}

	public void synchWrite(OpcDaMonitoredItem[] daItems, OpcDaVariant[] variants) throws Exception {
		OPCItemMgt itemManagement = groupManager.getItemManagement();

		OPCITEMDEF[] defs = new OPCITEMDEF[daItems.length];

		for (int i = 0; i < daItems.length; i++) {
			OPCITEMDEF def = new OPCITEMDEF();
			def.setActive(true);
			def.setItemID(daItems[i].getItemId());
			defs[i] = def;
		}

		KeyedResultSet<OPCITEMDEF, OPCITEMRESULT> result = itemManagement.add(defs);

		WriteRequest[] writeRequests = new WriteRequest[daItems.length];
		Integer[] writeServerHandles = new Integer[daItems.length];

		for (int i = 0; i < daItems.length; i++) {
			if (result.get(i).getErrorCode() != 0) {
				throw new JIException(result.get(i).getErrorCode());
			}

			JIVariant variant = variants[i].getJIVariant();
			writeRequests[i] = new WriteRequest(result.get(i).getValue().getServerHandle(), variant);
			writeServerHandles[i] = writeRequests[i].getServerHandle();
		}

		// Perform write
		OPCSyncIO syncIO = groupManager.getSyncIO();
		ResultSet<WriteRequest> writeResults = syncIO.write(writeRequests);

		for (int i = 0; i < daItems.length; i++) {
			Result<WriteRequest> writeResult = writeResults.get(i);

			if (writeResult.getErrorCode() != 0) {
				throw new Exception(DomainLocalizer.instance().getErrorString("can.not.write", daItems[i].getItemId(),
						String.format("%08X", writeResult.getErrorCode())));
			}
		}
	}

	public void updateSettings(OpcDaGroupState groupState) throws Exception {
		// leave null for current value
		groupManager.setState(groupState.getUpdateRate(), groupState.isActive(), null, groupState.getPercentDeadband(),
				null, clientHandle);
	}

	public OpcDaGroupState getState() throws Exception {
		return new OpcDaGroupState(groupManager.getState());
	}

	public OPCGroupStateMgt getGroupManager() {
		return groupManager;
	}

	public Map<Integer, OpcDaMonitoredItem> getAllItems() {
		return itemMap;
	}

	public void removeAllItems() throws Exception {
		OpcDaMonitoredItem[] items = new OpcDaMonitoredItem[itemMap.size()];
		itemMap.values().toArray(items);
		removeItems(items);
	}

	public void inactivateItems(Integer[] serverHandles) throws Exception {
		groupManager.getItemManagement().setActiveState(false, serverHandles);
	}

	public KeyedResultSet<Integer, OPCITEMSTATE> synchRead() throws Exception {
		OPCSyncIO syncIO = groupManager.getSyncIO();
		return syncIO.read(OPCDATASOURCE.OPC_DS_DEVICE, serverHandles);
	}

	@Override
	public String toString() {
		String value = "";
		try {
			value = groupManager.getState().getName();
		} catch (Exception e) {
			// ignore
		}
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpcDaMonitoredGroup) {
			OpcDaMonitoredGroup other = (OpcDaMonitoredGroup) obj;
			if (getName().equals(other.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(groupManager);
	}

	public void registerDataChangeListener(OpcDaDataChangeListener listener) {
		if (!listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	public void unregisterDataChangeListener(OpcDaDataChangeListener listener) {
		if (listeners.contains(listener)) {
			this.listeners.remove(listener);
		}
	}

	// GroupAsynchReadCallback
	private class GroupAsynchReadCallback implements IOPCDataCallback {

		@Override
		public void cancelComplete(int transactionId, int serverGroupHandle) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("cancelComplete: %08X, Group: %08X", transactionId, serverGroupHandle));
			}
		}

		@Override
		public void dataChange(int transactionId, int serverGroupHandle, int masterQuality, int masterErrorCode,
				KeyedResultSet<Integer, ValueData> result) {

			if (masterErrorCode != 0) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format(
							"Transaction Id: %d, Server Group Handle: %08X, Master Quality: %d, Error Code: %d",
							transactionId, serverGroupHandle, masterQuality, masterErrorCode));
				}
				return;
			}

			for (final KeyedResult<Integer, ValueData> entry : result) {
				Integer aClientHandle = entry.getKey();
				OpcDaMonitoredItem opcDaItem = itemMap.get(aClientHandle);

				if (opcDaItem != null) {
					try {
						opcDaItem.setValueData(entry.getValue());
					} catch (Exception e) {
						logger.error(e.getMessage());
						continue;
					}

					for (OpcDaDataChangeListener listener : listeners) {
						try {
							listener.onOpcDaDataChange(opcDaItem);
						} catch (Exception e) {
							logger.error(e.getMessage());
						}
					}
				}
			}
		}

		@Override
		public void readComplete(int transactionId, int serverGroupHandle, int masterQuality, int masterErrorCode,
				final KeyedResultSet<Integer, ValueData> result) {

			if (masterErrorCode != 0) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format("readComplete: %d, Group: %08X, MasterQ: %d, Error: %d", transactionId,
							serverGroupHandle, masterQuality, masterErrorCode));
				}
			}

			for (final KeyedResult<Integer, ValueData> entry : result) {

				if (entry.getErrorCode() != 0) {
					if (logger.isErrorEnabled()) {
						logger.error(String.format("%08X - Error: %08X, Quality: %d, %Tc - %s", entry.getKey(),
								entry.getErrorCode(), entry.getValue().getQuality(), entry.getValue().getTimestamp(),
								entry.getValue().getValue().toString()));
					}
				}
			}
		}

		@Override
		public void writeComplete(int transactionId, int serverGroupHandle, int masterErrorCode,
				ResultSet<Integer> result) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("writeComplete: %08X, Group: %08X", transactionId, serverGroupHandle));
			}
		}
	}
}
