package org.point85.domain.script;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.point85.domain.cron.CronEventClient;
import org.point85.domain.db.DatabaseEventClient;
import org.point85.domain.file.FileEventClient;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.rmq.RmqClient;
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

	// MQTT client key
	private static final String MQTT_KEY = "MQTT";

	// database client key
	private static final String DB_KEY = "DB";

	// file server key
	private static final String FILE_KEY = "FILE";

	// cron scheduler key
	private static final String CRON_KEY = "CRON";

	// HTTP server key
	private static final String HTTP_KEY = "HTTP";

	// Modbus key
	private static final String MODBUS_KEY = "MODBUS";

	// hash map of objects exposed to scripting
	private final ConcurrentMap<String, Object> contextMap;

	public OeeContext() {
		contextMap = new ConcurrentHashMap<>();

		contextMap.put(MATL_KEY, new ConcurrentHashMap<Equipment, Material>());
		contextMap.put(JOB_KEY, new ConcurrentHashMap<Equipment, String>());

		setOpcDaClients(new HashSet<>());
		setOpcUaClients(new HashSet<>());
		setMessagingClients(new HashSet<>());
		setJMSClients(new HashSet<>());
		setMQTTClients(new HashSet<>());
		setHttpServers(new HashSet<>());
		setDatabaseEventClients(new HashSet<>());
		setFileEventClients(new HashSet<>());
		setCronEventClients(new HashSet<>());
		setModbusMasters(new HashSet<>());
	}

	/**
	 * Get the job running on this equipment
	 * 
	 * @param equipment {@link Equipment}
	 * @return Job
	 */
	public String getJob(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);

		return equipment != null ? jobMap.get(equipment) : null;
	}

	/**
	 * Set the job running on this equipment
	 * 
	 * @param equipment {@link Equipment}
	 * @param job       Job
	 */
	public void setJob(Equipment equipment, String job) {
		if (job == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);
		jobMap.put(equipment, job);
	}

	/**
	 * Get the material being produced on this equipment
	 * 
	 * @param equipment {@link Equipment}
	 * @return {@link Material}
	 */
	public Material getMaterial(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);

		return equipment != null ? materialMap.get(equipment) : null;
	}

	/**
	 * Set the material being produced on this equipment
	 * 
	 * @param equipment {@link Equipment}
	 * @param material  {@link Material}
	 */
	public void setMaterial(Equipment equipment, Material material) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);
		materialMap.put(equipment, material);
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
	 * @param clients Set of {@link DaOpcClient}
	 */
	public void setOpcDaClients(Set<DaOpcClient> clients) {
		contextMap.put(OPC_DA_KEY, clients);
	}

	/**
	 * Add an OPC DA client to the list
	 * 
	 * @param daClient {@link DaOpcClient}
	 */
	public void addOpcDaClient(DaOpcClient daClient) {
		if (!getOpcDaClients().contains(daClient)) {
			getOpcDaClients().add(daClient);
		}
	}

	/**
	 * Remove an OPC DA client from the list
	 * 
	 * @param daClient {@link DaOpcClient}
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
	 * @param clients Set of {@link UaOpcClient}
	 */
	public void setOpcUaClients(Set<UaOpcClient> clients) {
		contextMap.put(OPC_UA_KEY, clients);
	}

	/**
	 * Add an OPC UA client to the list
	 * 
	 * @param uaClient {@link UaOpcClient}
	 */
	public void addOpcUaClient(UaOpcClient uaClient) {
		if (!getOpcUaClients().contains(uaClient)) {
			getOpcUaClients().add(uaClient);
		}
	}

	/**
	 * Remove an OPC UA client from the list
	 * 
	 * @param uaClient {@link UaOpcClient}
	 */
	public void removeOpcUaClient(UaOpcClient uaClient) {
		if (getOpcUaClients().contains(uaClient)) {
			getOpcUaClients().remove(uaClient);
		}
	}

	/**
	 * Get a list of the messaging clients defined for the collector
	 * 
	 * @return Collection of {@link RmqClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<RmqClient> getRmqClients() {
		return (Collection<RmqClient>) contextMap.get(MSG_KEY);
	}

	/**
	 * Get a list of the JMS clients defined for the collector
	 * 
	 * @return Collection of {@link JmsClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<JmsClient> getJmsClients() {
		return (Collection<JmsClient>) contextMap.get(JMS_KEY);
	}

	/**
	 * Get a list of the MQTT clients defined for the collector
	 * 
	 * @return Collection of {@link MqttOeeClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<MqttOeeClient> getMqttClients() {
		return (Collection<MqttOeeClient>) contextMap.get(MQTT_KEY);
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
	 * Get a list of the cron event clients defined for the collector
	 * 
	 * @return Collection of {@link CronEventClient}
	 */
	@SuppressWarnings("unchecked")
	public Collection<CronEventClient> getCronEventClients() {
		return (Collection<CronEventClient>) contextMap.get(CRON_KEY);
	}

	/**
	 * Get a list of Modbus masters defined for the collector
	 * 
	 * @return Collection of {@link ModbusMaster}
	 */
	@SuppressWarnings("unchecked")
	public Collection<ModbusMaster> getModbusMasters() {
		return (Collection<ModbusMaster>) contextMap.get(MODBUS_KEY);
	}

	/**
	 * Get the first (only) messaging client
	 * 
	 * @return {@link RmqClient}
	 */
	public RmqClient getMessagingClient() {
		// get the first one
		RmqClient client = null;

		if (!getRmqClients().isEmpty()) {
			client = getRmqClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) JMS client
	 * 
	 * @return {@link JmsClient}
	 */
	public JmsClient getJMSClient() {
		// get the first one
		JmsClient client = null;

		if (!getJmsClients().isEmpty()) {
			client = getJmsClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) MQTT client
	 * 
	 * @return {@link MqttOeeClient}
	 */
	public MqttOeeClient getMQTTClient() {
		// get the first one
		MqttOeeClient client = null;

		if (!getMqttClients().isEmpty()) {
			client = getMqttClients().iterator().next();
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
	 * @return {@link FileEventClient}
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
	 * Get the first (only) cron event client
	 * 
	 * @return {@link CronEventClient}
	 */
	public CronEventClient getCronEventClient() {
		// get the first one
		CronEventClient client = null;

		if (!getCronEventClients().isEmpty()) {
			client = getCronEventClients().iterator().next();
		}
		return client;
	}

	/**
	 * Get the first (only) Modbus master
	 * 
	 * @return {@link ModbusMaster}
	 */
	public ModbusMaster getModbusMaster() {
		// get the first one
		ModbusMaster master = null;

		if (!getModbusMasters().isEmpty()) {
			master = getModbusMasters().iterator().next();
		}
		return master;
	}

	/**
	 * Set the list of the messaging clients defined for the collector
	 * 
	 * @param clients Set of {@link RmqClient}
	 */
	public void setMessagingClients(Collection<RmqClient> clients) {
		contextMap.put(MSG_KEY, clients);
	}

	/**
	 * Set the list of the JMS clients defined for the collector
	 * 
	 * @param clients Set of {@link JmsClient}
	 */
	public void setJMSClients(Collection<JmsClient> clients) {
		contextMap.put(JMS_KEY, clients);
	}

	/**
	 * Set the list of the MQTT clients defined for the collector
	 * 
	 * @param clients Set of {@link MqttOeeClient}
	 */
	public void setMQTTClients(Collection<MqttOeeClient> clients) {
		contextMap.put(MQTT_KEY, clients);
	}

	/**
	 * Set the list of the database event clients defined for the collector
	 * 
	 * @param clients Set of {@link DatabaseEventClient}
	 */
	public void setDatabaseEventClients(Collection<DatabaseEventClient> clients) {
		contextMap.put(DB_KEY, clients);
	}

	/**
	 * Set the list of the file event clients defined for the collector
	 * 
	 * @param clients Set of {@link FileEventClient}
	 */
	public void setFileEventClients(Collection<FileEventClient> clients) {
		contextMap.put(FILE_KEY, clients);
	}

	/**
	 * Set the list of the cron event clients defined for the collector
	 * 
	 * @param clients Set of {@link CronEventClient}
	 */
	public void setCronEventClients(Collection<CronEventClient> clients) {
		contextMap.put(CRON_KEY, clients);
	}

	/**
	 * Set the list of the Modbus masters defined for the collector
	 * 
	 * @param masters Set of {@link ModbusMaster}
	 */
	public void setModbusMasters(Collection<ModbusMaster> masters) {
		contextMap.put(MODBUS_KEY, masters);
	}

	/**
	 * Add a messaging client to the list
	 * 
	 * @param client {@link RmqClient}
	 */
	public void addMessagingClient(RmqClient client) {
		if (!getRmqClients().contains(client)) {
			getRmqClients().add(client);
		}
	}

	/**
	 * Add a JMS client to the list
	 * 
	 * @param client {@link JmsClient}
	 */
	public void addJMSClient(JmsClient client) {
		if (!getJmsClients().contains(client)) {
			getJmsClients().add(client);
		}
	}

	/**
	 * Add an MQTT client to the list
	 * 
	 * @param client {@link MqttOeeClient}
	 */
	public void addMQTTClient(MqttOeeClient client) {
		if (!getMqttClients().contains(client)) {
			getMqttClients().add(client);
		}
	}

	/**
	 * Add a database event client to the list
	 * 
	 * @param client {@link DatabaseEventClient}
	 */
	public void addDatabaseEventClient(DatabaseEventClient client) {
		if (!getDatabaseEventClients().contains(client)) {
			getDatabaseEventClients().add(client);
		}
	}

	/**
	 * Add a file event client to the list
	 * 
	 * @param client {@link FileEventClient}
	 */
	public void addFileEventClient(FileEventClient client) {
		if (!getFileEventClients().contains(client)) {
			getFileEventClients().add(client);
		}
	}

	/**
	 * Add a cron event client to the list
	 * 
	 * @param client {@link CronEventClient}
	 */
	public void addCronEventClient(CronEventClient client) {
		if (!getCronEventClients().contains(client)) {
			getCronEventClients().add(client);
		}
	}

	/**
	 * Add a Modbus master to the list
	 * 
	 * @param master {@link ModbusMaster}
	 */
	public void addModbusMaster(ModbusMaster master) {
		if (!getModbusMasters().contains(master)) {
			getModbusMasters().add(master);
		}
	}

	/**
	 * Remove a messaging client from the list
	 * 
	 * @param client {@link RmqClient}
	 */
	public void removeMessagingClient(RmqClient client) {
		if (getRmqClients().contains(client)) {
			getRmqClients().remove(client);
		}
	}

	/**
	 * Remove a JMS client from the list
	 * 
	 * @param client {@link JmsClient}
	 */
	public void removeJMSClient(JmsClient client) {
		if (getJmsClients().contains(client)) {
			getJmsClients().remove(client);
		}
	}

	/**
	 * Remove an MQTT client from the list
	 * 
	 * @param client {@link MqttOeeClient}
	 */
	public void removeMQTTClient(MqttOeeClient client) {
		if (getMqttClients().contains(client)) {
			getMqttClients().remove(client);
		}
	}

	/**
	 * Remove a database event client from the list
	 * 
	 * @param client {@link DatabaseEventClient}
	 */
	public void removeDatabaseEventClient(DatabaseEventClient client) {
		if (getDatabaseEventClients().contains(client)) {
			getDatabaseEventClients().remove(client);
		}
	}

	/**
	 * Remove a file event client from the list
	 * 
	 * @param client {@link FileEventClient}
	 */
	public void removeFileEventClient(FileEventClient client) {
		if (getFileEventClients().contains(client)) {
			getFileEventClients().remove(client);
		}
	}

	/**
	 * Remove a cron event client from the list
	 * 
	 * @param client {@link CronEventClient}
	 */
	public void removeCronEventClient(CronEventClient client) {
		if (getCronEventClients().contains(client)) {
			getCronEventClients().remove(client);
		}
	}

	/**
	 * Remove a Modbus master from the list
	 * 
	 * @param master {@link ModbusMaster}
	 */
	public void removeModbusMaster(ModbusMaster master) {
		if (getModbusMasters().contains(master)) {
			getModbusMasters().remove(master);
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
	 * @param servers Set of {@link OeeHttpServer}
	 */
	public void setHttpServers(Set<OeeHttpServer> servers) {
		contextMap.put(HTTP_KEY, servers);
	}

	/**
	 * Add an HTTP server to the list
	 * 
	 * @param server {@link OeeHttpServer}
	 */
	public void addHttpServer(OeeHttpServer server) {
		if (!getHttpServers().contains(server)) {
			getHttpServers().add(server);
		}
	}

	/**
	 * Remove an HTTP server from the list
	 * 
	 * @param server {@link OeeHttpServer}
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
		for (RmqClient client : getRmqClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n JMS clients ...");
		for (JmsClient client : getJmsClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n MQTT clients ...");
		for (MqttOeeClient client : getMqttClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n HTTP servers ...");
		for (OeeHttpServer server : getHttpServers()) {
			sb.append('\t').append(server.toString());
		}

		sb.append("\n Database clients ...");
		for (DatabaseEventClient client : getDatabaseEventClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n File clients ...");
		for (FileEventClient client : getFileEventClients()) {
			sb.append('\t').append(client.toString());
		}

		sb.append("\n Cron clients ...");
		for (CronEventClient client : getCronEventClients()) {
			sb.append('\t').append(client.toString());
		}

		return sb.toString();
	}
}
