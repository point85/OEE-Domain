
package org.point85.domain.opc.da;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.FILETIME;
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
import org.openscada.opc.lib.da.browser.TreeBrowser;
import org.openscada.opc.lib.list.Category;
import org.openscada.opc.lib.list.ServerList;
import org.point85.domain.CollectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * 
 * @author Kent Randall
 */
public class OpcDaClient {
	static {
		// jinterop uses java.util.logging directly
		// remove existing handlers attached to j.u.l root logger
		SLF4JBridgeHandler.removeHandlersForRootLogger();

		// add SLF4JBridgeHandler to j.u.l's root logger
		SLF4JBridgeHandler.install();
	}

	// logging utility
	private static Logger logger = LoggerFactory.getLogger(OpcDaClient.class);

	private JISession jiSession;
	private OPCServer opcServer;
	private static final int DEFAULT_LOCALE_ID = 1033;
	private static final Integer TIME_BIAS = null;

	private Random intGenerator = new Random(System.currentTimeMillis());

	private OpcDaTreeBrowser tagTreeBrowser;

	private Map<String, OpcDaMonitoredGroup> monitoredGroups = new HashMap<>();

	public OpcDaClient() {

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

	public void connect(OpcDaSource opcDaSource) throws Exception {
		this.connect(opcDaSource.getHost(), opcDaSource.getUserName(), opcDaSource.getPassword(),
				opcDaSource.getProgId(), opcDaSource.getClassId());
	}

	private void connect(String host, String domainUser, String password, String progId, String classId)
			throws Exception {

		String[] userInfo = CollectorUtils.parseDomainAndUser(domainUser);

		// prevents waiting indefinitely for socket close on server pinging
		JISystem.setJavaCoClassAutoCollection(false);

		JISystem.setAutoRegisteration(false);

		if (logger.isInfoEnabled()) {
			String text = "Connecting to OPC DA server, host: " + host + ", domain:" + userInfo[0] + ", user: "
					+ userInfo[1] + ", password: " + password + ", progId: " + progId + ", classid: " + classId;
			logger.info(text);
		}

		// create the server
		createOpcServer(host, userInfo[0], userInfo[1], password, progId, classId);
	}

	public void disconnect() throws Exception {

		if (jiSession != null) {
			JISession.destroySession(jiSession);
		}
		jiSession = null;
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

		OpcDaMonitoredGroup opcDaGroup = new OpcDaMonitoredGroup(opcGroupStateMgt, clientHandle);

		return opcDaGroup;
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
		String[] userInfo = CollectorUtils.parseDomainAndUser(userName);

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

	public static String[] parseDomainAndUser(String name) {
		// TODO
		return new String[0];
	}

	public void saveSession() {
		// TODO
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

			// OpcDaBrowserLeaf tag = tagItem.getLeafTag();

			// TODO
			/*
			 * List<String> ap = tag.getAccesspath(); String fn = tag.getFullName(); String
			 * id = tag.getItemId();
			 */

			// if (tag == null) {
			// find it
			OpcDaBrowserLeaf tag = browser.findTag(tagItem.getPathName());
			// }

			if (tag == null) {
				throw new Exception("Unable to find tag with access path " + tagItem.getPathName());
			}
			tagArray[i] = tag;
		}

		opcDaGroup.addItems(tagArray, true);

		return opcDaGroup;
	}

	/*
	 * public OpcDaBrowserLeaf getLeafFromTagItem(TagItem tagItem) throws Exception
	 * { OpcDaBrowserLeaf leaf = tagItem.getLeafTag();
	 * 
	 * if (leaf == null) { // find it OpcDaTagTreeBrowser browser =
	 * getTreeBrowser(); leaf = browser.findTag(tagItem.getAccessPath(),
	 * tagItem.getItemName()); tagItem.setLeafTag(leaf); } return leaf; }
	 */

	public static ZonedDateTime fromFiletime(FILETIME filetime) {
		Calendar cal = filetime.asCalendar();
		Instant instant = Instant.ofEpochMilli(cal.getTime().getTime());
		ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, cal.getTimeZone().toZoneId());
		return zdt;
	}

}