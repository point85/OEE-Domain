package org.point85.domain.script;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.point85.domain.db.DatabaseEventClient;
import org.point85.domain.file.FileEventClient;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.jms.JMSClient;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains information about the execution environment
 * 
 *
 */
public class OeeContext {
	// logger for scripting
	private Logger logger;

	// material key
	private static final String MATL_KEY = "MATL";

	// job key
	private static final String JOB_KEY = "JOB";

	// logger key
	private static final String LOGGER_KEY = "LOGGER";

	// OPC DA client key
	private static final String OPC_DA_KEY = "OPC_DA";

	// OPC UA client key
	private static final String OPC_UA_KEY = "OPC_UA";

	// RMQ client key
	private static final String MSG_KEY = "MESSAGE";

	// JMS client key
	private static final String JMS_KEY = "JMS";

	// database client key
	private static final String DB_KEY = "DB";

	// file server key
	private static final String FILE_KEY = "FILE";

	// HTTP server key
	private static final String HTTP_KEY = "HTTP";

	// external reason key
	private static final String REASON_KEY = "REASON";

	// hash map of objects exposed to scripting
	private final ConcurrentMap<String, Object> contextMap;

	// cached reasons
	private final ConcurrentMap<String, Reason> reasonCache;

	public OeeContext() {
		contextMap = new ConcurrentHashMap<>();
		reasonCache = new ConcurrentHashMap<>();

		contextMap.put(MATL_KEY, new ConcurrentHashMap<Equipment, Material>());
		contextMap.put(JOB_KEY, new ConcurrentHashMap<Equipment, String>());
		contextMap.put(REASON_KEY, new ConcurrentHashMap<Equipment, Reason>());

		setOpcDaClients(new HashSet<DaOpcClient>());
		setOpcUaClients(new HashSet<UaOpcClient>());
		setMessagingClients(new HashSet<MessagingClient>());
		setJMSClients(new HashSet<JMSClient>());
		setHttpServers(new HashSet<OeeHttpServer>());
		setDatabaseEventClients(new HashSet<DatabaseEventClient>());
		setFileEventClients(new HashSet<FileEventClient>());
	}

	/**
	 * Get the job running on this equipment
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @return Job
	 */
	public String getJob(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);

		return jobMap.get(equipment);
	}

	/**
	 * Set the job running on this equipment
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @param job
	 *            Job
	 */
	public void setJob(Equipment equipment, String job) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);
		jobMap.put(equipment, job);
	}

	/**
	 * Get the material being produced on this equipment
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @return {@link Material}
	 */
	public Material getMaterial(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);

		return materialMap.get(equipment);
	}

	/**
	 * Set the material being produced on this equipment
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @param material
	 *            {@link Material}
	 */
	public void setMaterial(Equipment equipment, Material material) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);
		materialMap.put(equipment, material);
	}

	/**
	 * Get the equipment reason for a quality event
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @return {@link Reason}
	 */
	public Reason getQualityReason(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Reason> reasonMap = (ConcurrentMap<Equipment, Reason>) contextMap.get(REASON_KEY);

		return reasonMap.get(equipment);
	}

	/**
	 * Set the equipment reason for a quality event
	 * 
	 * @param equipment
	 *            {@link Equipment}
	 * @param reasonName
	 *            Reason id
	 * @throws Exception
	 *             Exception
	 */
	public void setQualityReason(Equipment equipment, String reasonName) throws Exception {
		// fetch the reason
		if (equipment == null || reasonName == null) {
			return;
		}

		Reason reason = fetchReason(reasonName);

		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Reason> reasonMap = (ConcurrentMap<Equipment, Reason>) contextMap.get(REASON_KEY);
		reasonMap.put(equipment, reason);
	}

	private Reason fetchReason(String reasonName) throws Exception {
		Reason reason = reasonCache.get(reasonName);

		if (reason == null) {
			// fetch from database
			reason = PersistenceService.instance().fetchReasonByName(reasonName);

			// cache it
			if (reason != null) {
				reasonCache.put(reasonName, reason);
			} else {
				throw new Exception("Reason " + reasonName + " not found in database.");
			}
		}
		return reason;
	}

	/**
	 * Get a logger
	 * 
	 * @return Logger
	 */
	public Logger getLogger() {
		if (logger == null) {
			logger = LoggerFactory.getLogger(getClass());
			setLogger();
		}
		return (Logger) contextMap.get(LOGGER_KEY);
	}

	private void setLogger() {
		contextMap.put(LOGGER_KEY, logger);
	}

	/**
	 * Get a list of the OPC DA clients defined for the collector
	 * 
	 * @return Collection of {@link DaOpcClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<DaOpcClient> getOpcDaClients() {
		return (Collection<DaOpcClient>) contextMap.get(OPC_DA_KEY);
	}

	/**
	 * Get the first (only) OPC DA client
	 * 
	 * @return {@link DaOpcClient}
	 */
	public DaOpcClient getOpcDaClient() {
		// get the first one
		DaOpcClient client = null;

		if (!getOpcDaClients().isEmpty()) {
			client = getOpcDaClients().iterator().next();
		}
		return client;
	}

	/**
	 * Set OPC DA clients
	 * 
	 * @param clients
	 *            Set of {@link DaOpcClient}
	 */
	public void setOpcDaClients(Set<DaOpcClient> clients) {
		contextMap.put(OPC_DA_KEY, clients);
	}

	/**
	 * Add an OPC DA client to the list
	 * 
	 * @param daClient
	 *            {@link DaOpcClient}
	 */
	public void addOpcDaClient(DaOpcClient daClient) {
		if (!getOpcDaClients().contains(daClient)) {
			getOpcDaClients().add(daClient);
		}
	}

	/**
	 * Remove an OPC DA client from the list
	 * 
	 * @param daClient
	 *            {@link DaOpcClient}
	 */
	public void removeOpcDaClient(DaOpcClient daClient) {
		if (getOpcDaClients().contains(daClient)) {
			getOpcDaClients().remove(daClient);
		}
	}

	/**
	 * Get a list of the OPC UA clients defined for the collector
	 * 
	 * @return Collection of {@link UaOpcClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<UaOpcClient> getOpcUaClients() {
		return (Collection<UaOpcClient>) contextMap.get(OPC_UA_KEY);
	}

	/**
	 * Get the first (only) OPC UA client
	 * 
	 * @return {@link UaOpcClient}
	 */
	public UaOpcClient getOpcUaClient() {
		// get the first one
		UaOpcClient client = null;

		if (!getOpcUaClients().isEmpty()) {
			client = getOpcUaClients().iterator().next();
		}
		return client;
	}

	/**
	 * Set the list of the OPC UA clients defined for the collector
	 * 
	 * @param clients
	 *            Set of {@link UaOpcClient}
	 */
	public void setOpcUaClients(Set<UaOpcClient> clients) {
		contextMap.put(OPC_UA_KEY, clients);
	}

	/**
	 * Add an OPC UA client to the list
	 * 
	 * @param uaClient
	 *            {@link UaOpcClient}
	 */
	public void addOpcUaClient(UaOpcClient uaClient) {
		if (!getOpcUaClients().contains(uaClient)) {
			getOpcUaClients().add(uaClient);
		}
	}

	/**
	 * Remove an OPC UA client from the list
	 * 
	 * @param uaClient
	 *            {@link UaOpcClient}
	 */
	public void removeOpcUaClient(UaOpcClient uaClient) {
		if (getOpcUaClients().contains(uaClient)) {
			getOpcUaClients().remove(uaClient);
		}
	}

	/**
	 * Get a list of the messaging clients defined for the collector
	 * 
	 * @return Collection of {@link MessagingClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<MessagingClient> getMessagingClients() {
		return (Collection<MessagingClient>) contextMap.get(MSG_KEY);
	}

	/**
	 * Get a list of the JMS clients defined for the collector
	 * 
	 * @return Collection of {@link JMSClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<JMSClient> getJMSClients() {
		return (Collection<JMSClient>) contextMap.get(JMS_KEY);
	}

	/**
	 * Get a list of the database event clients defined for the collector
	 * 
	 * @return Collection of {@link DatabaseEventClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<DatabaseEventClient> getDatabaseEventClients() {
		return (Collection<DatabaseEventClient>) contextMap.get(DB_KEY);
	}

	/**
	 * Get a list of the file event clients defined for the collector
	 * 
	 * @return Collection of {@link FileEventClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<FileEventClient> getFileEventClients() {
		return (Collection<FileEventClient>) contextMap.get(FILE_KEY);
	}

	/**
	 * Get the first (only) messaging client
	 * 
	 * @return {@link MessagingClient}
	 */
	public MessagingClient getMessagingClient() {
		// get the first one
		MessagingClient client = null;

		if (!getMessagingClients().isEmpty()) {
			client = getMessagingClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) JMS client
	 * 
	 * @return {@link JMSClient}
	 */
	public JMSClient getJMSClient() {
		// get the first one
		JMSClient client = null;

		if (!getJMSClients().isEmpty()) {
			client = getJMSClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) database event client
	 * 
	 * @return {@link DatabaseEventClient}
	 */
	public DatabaseEventClient getDatabaseEventClient() {
		// get the first one
		DatabaseEventClient client = null;

		if (!getDatabaseEventClients().isEmpty()) {
			client = getDatabaseEventClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) file event client
	 * 
	 * @return {@link DatabaseEventClient}
	 */
	public FileEventClient getFileEventClient() {
		// get the first one
		FileEventClient client = null;

		if (!getFileEventClients().isEmpty()) {
			client = getFileEventClients().iterator().next();
		}
		return client;
	}

	/**
	 * Set the list of the messaging clients defined for the collector
	 * 
	 * @param clients
	 *            Set of {@link MessagingClient}
	 */
	public void setMessagingClients(Collection<MessagingClient> clients) {
		contextMap.put(MSG_KEY, clients);
	}

	/**
	 * Set the list of the JMS clients defined for the collector
	 * 
	 * @param clients
	 *            Set of {@link JMSClient}
	 */
	public void setJMSClients(Collection<JMSClient> clients) {
		contextMap.put(JMS_KEY, clients);
	}

	/**
	 * Set the list of the database event clients defined for the collector
	 * 
	 * @param clients
	 *            Set of {@link DatabaseEventClient}
	 */
	public void setDatabaseEventClients(Collection<DatabaseEventClient> clients) {
		contextMap.put(DB_KEY, clients);
	}

	/**
	 * Set the list of the file event clients defined for the collector
	 * 
	 * @param clients
	 *            Set of {@link FileEventClient}
	 */
	public void setFileEventClients(Collection<FileEventClient> clients) {
		contextMap.put(FILE_KEY, clients);
	}

	/**
	 * Add a messaging client to the list
	 * 
	 * @param client
	 *            {@link MessagingClient}
	 */
	public void addMessagingClient(MessagingClient client) {
		if (!getMessagingClients().contains(client)) {
			getMessagingClients().add(client);
		}
	}

	/**
	 * Add a JMS client to the list
	 * 
	 * @param client
	 *            {@link JMSClient}
	 */
	public void addJMSClient(JMSClient client) {
		if (!getJMSClients().contains(client)) {
			getJMSClients().add(client);
		}
	}

	/**
	 * Add a database event client to the list
	 * 
	 * @param client
	 *            {@link DatabaseEventClient}
	 */
	public void addDatabaseEventClient(DatabaseEventClient client) {
		if (!getDatabaseEventClients().contains(client)) {
			getDatabaseEventClients().add(client);
		}
	}

	/**
	 * Add a file event client to the list
	 * 
	 * @param client
	 *            {@link FileEventClient}
	 */
	public void addFileEventClient(FileEventClient client) {
		if (!getFileEventClients().contains(client)) {
			getFileEventClients().add(client);
		}
	}

	/**
	 * Remove a messaging client from the list
	 * 
	 * @param client
	 *            {@link MessagingClient}
	 */
	public void removeMessagingClient(MessagingClient client) {
		if (getMessagingClients().contains(client)) {
			getMessagingClients().remove(client);
		}
	}

	/**
	 * Remove a JMS client from the list
	 * 
	 * @param client
	 *            {@link JMSClient}
	 */
	public void removeJMSClient(JMSClient client) {
		if (getJMSClients().contains(client)) {
			getJMSClients().remove(client);
		}
	}

	/**
	 * Remove a database event client from the list
	 * 
	 * @param client
	 *            {@link DatabaseEventClient}
	 */
	public void removeDatabaseEventClient(DatabaseEventClient client) {
		if (getDatabaseEventClients().contains(client)) {
			getDatabaseEventClients().remove(client);
		}
	}

	/**
	 * Remove a file event client from the list
	 * 
	 * @param client
	 *            {@link FileEventClient}
	 */
	public void removeFileEventClient(FileEventClient client) {
		if (getFileEventClients().contains(client)) {
			getFileEventClients().remove(client);
		}
	}

	/**
	 * Get a list of the HTTP servers defined for the collector
	 * 
	 * @return Collection of {@link OeeHttpServer}
	 */
	@SuppressWarnings("unchecked")
	public Collection<OeeHttpServer> getHttpServers() {
		return (Collection<OeeHttpServer>) contextMap.get(HTTP_KEY);
	}

	/**
	 * Get the first (only) HTTP server
	 * 
	 * @return {@link OeeHttpServer}
	 */
	public OeeHttpServer getHttpServer() {
		// get the first one
		OeeHttpServer server = null;

		if (!getHttpServers().isEmpty()) {
			server = getHttpServers().iterator().next();
		}
		return server;
	}

	/**
	 * Set a list of the HTTP servers defined for the collector
	 * 
	 * @param servers
	 *            Set of {@link OeeHttpServer}
	 */
	public void setHttpServers(Set<OeeHttpServer> servers) {
		contextMap.put(HTTP_KEY, servers);
	}

	/**
	 * Add an HTTP server to the list
	 * 
	 * @param server
	 *            {@link OeeHttpServer}
	 */
	public void addHttpServer(OeeHttpServer server) {
		if (!getHttpServers().contains(server)) {
			getHttpServers().add(server);
		}
	}

	/**
	 * Remove an HTTP server from the list
	 * 
	 * @param server
	 *            {@link OeeHttpServer}
	 */
	public void removeHttpServer(OeeHttpServer server) {
		if (getHttpServers().contains(server)) {
			getHttpServers().remove(server);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("\n OPC DA clients ...");
		for (DaOpcClient daClient : getOpcDaClients()) {
			sb.append('\t').append(daClient.toString());
		}

		sb.append("\n OPC UA clients ...");
		for (UaOpcClient uaClient : getOpcUaClients()) {
			sb.append('\t').append(uaClient.toString());
		}

		sb.append("\n Messaging clients ...");
		for (MessagingClient client : getMessagingClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n JMS clients ...");
		for (JMSClient client : getJMSClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n HTTP servers ...");
		for (OeeHttpServer server : this.getHttpServers()) {
			sb.append('\t').append(server.toString());
		}

		return sb.toString();
	}
}
