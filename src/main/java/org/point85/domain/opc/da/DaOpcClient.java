
package org.point85.domain.opc.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;

import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.impl.OPCCommon;
import org.openscada.opc.dcom.da.OPCNAMESPACETYPE;
import org.openscada.opc.dcom.da.PropertyDescription;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;
import org.openscada.opc.dcom.da.impl.OPCGroupStateMgt;
import org.openscada.opc.dcom.da.impl.OPCItemProperties;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.browser.TreeBrowser;
import org.openscada.opc.lib.list.Category;
import org.openscada.opc.lib.list.ServerList;
import org.point85.domain.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class DaOpcClient {
	static {
		// jinterop uses java.util.logging directly
		// remove existing handlers attached to j.u.l root logger
		SLF4JBridgeHandler.removeHandlersForRootLogger();

		// add SLF4JBridgeHandler to j.u.l's root logger
		SLF4JBridgeHandler.install();
	}

	// logging utility
	private static final Logger logger = LoggerFactory.getLogger(DaOpcClient.class);

	private OpcDaSource connectedSource;

	private JISession jiSession;
	private OPCServer opcServer;
	private static final int DEFAULT_LOCALE_ID = 1033;
	private static final Integer TIME_BIAS = null;

	private final Random intGenerator = new Random(System.currentTimeMillis());

	private OpcDaTreeBrowser tagTreeBrowser;

	private final Map<String, OpcDaMonitoredGroup> monitoredGroups = new HashMap<>();

	public DaOpcClient() {
		// nothing to initialize
	}

	public Collection<OpcDaMonitoredGroup> getMonitoredGroups() {
		return this.monitoredGroups.values();
	}

	private OPCItemProperties getOPCItemProperties() {
		return opcServer.getItemPropertiesService();
	}

	private void createOpcServer(String host, String domain, String user, String password, String progId,
			String classId) throws Exception {
		JIComServer comServer = null;

		jiSession = JISession.createSession(domain, user, password);

		if (progId != null) {
			JIProgId jiProgId = JIProgId.valueOf(progId);
			comServer = new JIComServer(jiProgId, host, jiSession);
		} else if (classId != null) {
			JIClsid jiClsid = JIClsid.valueOf(classId);
			comServer = new JIComServer(jiClsid, host, jiSession);
		} else {
			throw new Exception("Either a progId or classId must be specified");
		}

		IJIComObject serverObject = comServer.createInstance();
		opcServer = new OPCServer(serverObject);
	}

	@Override
	public int hashCode() {
		String host = connectedSource != null ? connectedSource.getHost() : "host";
		String progId = connectedSource != null ? connectedSource.getProgId() : "progId";
		return Objects.hash(host, progId);
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof DaOpcClient)) {
			return false;
		}
		DaOpcClient otherClient = (DaOpcClient) other;

		return connectedSource.getHost().equals(otherClient.connectedSource.getHost())
				&& connectedSource.getProgId() == otherClient.connectedSource.getProgId();
	}

	@Override
	public String toString() {
		return connectedSource != null
				? "Host: " + connectedSource.getHost() + ", ProgId: " + connectedSource.getProgId()
				: "";
	}

	public void connect(OpcDaSource opcDaSource) throws Exception {
		this.connect(opcDaSource.getHost(), opcDaSource.getUserName(), opcDaSource.getUserPassword(),
				opcDaSource.getProgId(), opcDaSource.getClassId());
		connectedSource = opcDaSource;
	}

	private void connect(String host, String domainUser, String password, String progId, String classId)
			throws Exception {

		String[] userInfo = DomainUtils.parseDomainAndUser(domainUser);

		// prevents waiting indefinitely for socket close on server pinging
		JISystem.setJavaCoClassAutoCollection(false);

		JISystem.setAutoRegisteration(false);

		if (logger.isInfoEnabled()) {
			String text = "Connecting to OPC DA server, host: " + host + ", domain:" + userInfo[0] + ", user: "
					+ userInfo[1] + ", ProgId: " + progId + ", classid: " + classId;
			logger.info(text);
		}

		// create the server
		createOpcServer(host, userInfo[0], userInfo[1], password, progId, classId);
	}

	public void disconnect() throws Exception {

		if (jiSession != null) {
			// should it be destroyed?
			// JISession.destroySession(jiSession);
		}
		jiSession = null;
		connectedSource = null;
	}

	public OpcDaTreeBrowser getTreeBrowser() throws Exception {

		if (tagTreeBrowser == null) {

			if (opcServer == null) {
				throw new Exception("Not connected to an OPC server.");
			}

			OPCBrowseServerAddressSpace addressSpace = opcServer.getBrowser();
			if (addressSpace == null) {
				throw new Exception("Unable to obtain a tag browser.");
			}

			if (addressSpace.queryOrganization() != OPCNAMESPACETYPE.OPC_NS_HIERARCHIAL) {
				throw new Exception("OPC server must have a hierarchical name space organization.");
			}

			TreeBrowser treeBrowser = new TreeBrowser(addressSpace);

			tagTreeBrowser = new OpcDaTreeBrowser(treeBrowser, getOPCItemProperties());
		}

		return tagTreeBrowser;
	}

	public void releaseTreeBrowser() {
		this.tagTreeBrowser = null;
	}

	public OpcDaMonitoredGroup addGroup(String groupName, boolean active, int updateRate, float percentDeadband)
			throws Exception {

		if (opcServer == null) {
			throw new Exception("There is no connection to an OPC server.");
		}

		int clientHandle = intGenerator.nextInt();

		OPCGroupStateMgt opcGroupStateMgt = opcServer.addGroup(groupName, active, updateRate, clientHandle, TIME_BIAS,
				percentDeadband, DEFAULT_LOCALE_ID);

		return new OpcDaMonitoredGroup(opcGroupStateMgt, clientHandle);
	}

	public void removeGroup(OpcDaMonitoredGroup group) throws Exception {
		group.stopMonitoring();
		OPCGroupStateMgt groupManager = group.getGroupManager();
		opcServer.removeGroup(groupManager, true);

		group.getAllItems().clear();
	}

	public OpcDaMonitoredGroup getGroupByName(String name) throws Exception {
		OpcDaMonitoredGroup opcDaGroup = null;

		OPCGroupStateMgt opcGroupStateMgt = this.opcServer.getGroupByName(name);

		if (opcGroupStateMgt != null) {
			int clientHandle = intGenerator.nextInt();
			opcDaGroup = new OpcDaMonitoredGroup(opcGroupStateMgt, clientHandle);
		}

		return opcDaGroup;
	}

	public String getErrorCodeString(int errorCode) throws Exception {
		String errorString = "Unknown error";

		if (opcServer != null) {
			OPCCommon common = opcServer.getCommon();
			errorString = common.getErrorString(errorCode, 1033);
		}
		return errorString;
	}

	public static ArrayList<String> getServerProgIds(String hostName, String userName, String password)
			throws Exception {
		String[] userInfo = DomainUtils.parseDomainAndUser(userName);

		ServerList serverList = new ServerList(hostName, userInfo[0], password, userInfo[1]);

		Collection<ClassDetails> serverDetails = serverList.listServersWithDetails(
				new Category[] { org.openscada.opc.lib.list.Categories.OPCDAServer20 }, new Category[] {});

		ArrayList<String> progIds = new ArrayList<>();

		for (ClassDetails serverDetail : serverDetails) {
			progIds.add(serverDetail.getProgId());
		}

		return progIds;
	}

	public Map<Integer, OpcDaItemProperty> getItemProperties(OpcDaBrowserLeaf opcDaTag) throws Exception {
		Map<Integer, OpcDaItemProperty> properties = new HashMap<>();

		Collection<PropertyDescription> descriptors = opcDaTag.getProperties();
		if (descriptors == null) {
			return properties;
		}

		int[] propIds = new int[descriptors.size()];

		int i = 0;
		for (PropertyDescription pd : descriptors) {
			propIds[i] = pd.getId();
			OpcDaItemProperty itemProperty = new OpcDaItemProperty(pd.getId(), pd.getDescription().trim(),
					pd.getVarType());
			properties.put(new Integer(pd.getId()), itemProperty);
			i++;
		}

		OPCItemProperties itemManager = opcServer.getItemPropertiesService();
		String itemId = opcDaTag.getLeaf().getItemId();

		KeyedResultSet<Integer, JIVariant> values = itemManager.getItemProperties(itemId, propIds);

		for (KeyedResult<Integer, JIVariant> entry : values) {
			// int errorCode = entry.getErrorCode();
			OpcDaVariant variant = new OpcDaVariant(entry.getValue());
			properties.get(entry.getKey()).setValue(variant);
		}

		return properties;
	}

	public OpcDaServerStatus getServerStatus() throws Exception {
		return new OpcDaServerStatus(opcServer.getStatus());
	}

	public OpcDaMonitoredGroup registerTags(TagGroupInfo group, OpcDaDataChangeListener changeListener)
			throws Exception {

		OpcDaMonitoredGroup opcDaGroup = this.monitoredGroups.get(group.getGroupName());

		if (opcDaGroup == null) {
			// create a group
			opcDaGroup = addGroup(group.getGroupName(), true, group.getUpdatePeriod(), 0.0f);

			opcDaGroup.registerDataChangeListener(changeListener);

			monitoredGroups.put(group.getGroupName(), opcDaGroup);
		}

		OpcDaTreeBrowser browser = getTreeBrowser();

		List<TagItemInfo> tagItems = group.getTagItems();
		OpcDaBrowserLeaf[] tagArray = new OpcDaBrowserLeaf[tagItems.size()];

		for (int i = 0; i < tagItems.size(); i++) {
			TagItemInfo tagItem = tagItems.get(i);

			// find tag
			OpcDaBrowserLeaf tag = browser.findTag(tagItem.getPathName());

			if (tag == null) {
				throw new Exception("Unable to find tag with access path " + tagItem.getPathName());
			}
			tagArray[i] = tag;
		}

		opcDaGroup.addItems(tagArray, true);

		return opcDaGroup;
	}

	public OPCServer getNativeServer() {
		return opcServer;
	}

	public OpcDaVariant synchRead(String itemId) throws Exception {
		if (connectedSource == null) {
			throw new Exception("The OPC DA client is not connected to a server.");
		}

		String[] userInfo = DomainUtils.parseDomainAndUser(connectedSource.getUserName());

		// create connection information
		ConnectionInformation ci = new ConnectionInformation();
		ci.setHost(connectedSource.getHost());
		ci.setDomain(userInfo[0]);
		ci.setUser(userInfo[1]);
		ci.setPassword(connectedSource.getUserPassword());
		ci.setProgId(connectedSource.getProgId());

		// create a new server
		Server server = new Server(ci, Executors.newSingleThreadScheduledExecutor());

		// connect to server
		server.connect();

		// create a group only for the read
		String groupName = Long.toHexString(System.currentTimeMillis());
		Group group = server.addGroup(groupName);

		// Add a new item to the group
		Item item = group.addItem(itemId);

		// read it
		ItemState itemState = item.read(false);
		
		server.disconnect();

		int errorCode = itemState.getErrorCode();
		if (errorCode != 0) {
			throw new Exception("Unable to read " + itemId + ", error code: " + String.format("%08X", errorCode));
		}

		return new OpcDaVariant(itemState.getValue());
	}
	
	public void writeSynch(String itemId, OpcDaVariant variant) throws Exception {
		if (connectedSource == null) {
			throw new Exception("The OPC DA client is not connected to a server.");
		}

		String[] userInfo = DomainUtils.parseDomainAndUser(connectedSource.getUserName());

		// create connection information
		ConnectionInformation ci = new ConnectionInformation();
		ci.setHost(connectedSource.getHost());
		ci.setDomain(userInfo[0]);
		ci.setUser(userInfo[1]);
		ci.setPassword(connectedSource.getUserPassword());
		ci.setProgId(connectedSource.getProgId());

		// create a new server
		Server server = new Server(ci, Executors.newSingleThreadScheduledExecutor());

		// connect to server
		server.connect();

		// create a group only for the read
		String groupName = Long.toHexString(System.currentTimeMillis());
		Group group = server.addGroup(groupName);

		// Add a new item to the group
		Item item = group.addItem(itemId);

		// write to it
		Integer errorCode = item.write(variant.getJIVariant());
		
		server.disconnect();
		
		if (errorCode != 0) {
			throw new Exception("Unable to write to " + itemId + ", error code: " + String.format("%08X", errorCode));
		}
	}

}