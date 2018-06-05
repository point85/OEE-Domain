package org.point85.domain.script;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	// RMQ pub/sub key
	private static final String PUB_SUB_KEY = "PUB_SUB";

	// HTTP server key
	private static final String HTTP_KEY = "HTTP";

	// hash map of objects exposed to scripting
	private final ConcurrentMap<String, Object> contextMap;

	public OeeContext() {
		contextMap = new ConcurrentHashMap<>();

		contextMap.put(MATL_KEY, new ConcurrentHashMap<Equipment, Material>());
		contextMap.put(JOB_KEY, new ConcurrentHashMap<Equipment, String>());

		setOpcDaClients(new ArrayList<DaOpcClient>());
		setOpcUaClients(new ArrayList<UaOpcClient>());
		setPublisherSubscribers(new ArrayList<PublisherSubscriber>());
		setHttpServers(new ArrayList<OeeHttpServer>());
	}

	public String getJob(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);

		return jobMap.get(equipment);
	}

	public void setJob(Equipment equipment, String job) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, String> jobMap = (ConcurrentMap<Equipment, String>) contextMap.get(JOB_KEY);
		jobMap.put(equipment, job);
	}

	public Material getMaterial(Equipment equipment) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);

		return materialMap.get(equipment);
	}

	public void setMaterial(Equipment equipment, Material material) {
		@SuppressWarnings("unchecked")
		ConcurrentMap<Equipment, Material> materialMap = (ConcurrentMap<Equipment, Material>) contextMap.get(MATL_KEY);
		materialMap.put(equipment, material);
	}

	public Object getItem(String key) {
		return this.contextMap.get(key);
	}

	public void setItem(String key, Object value) {
		this.contextMap.put(key, value);
	}

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

	@SuppressWarnings("unchecked")
	public List<DaOpcClient> getOpcDaClients() {
		return (List<DaOpcClient>) this.contextMap.get(OPC_DA_KEY);
	}

	public DaOpcClient getOpcDaClient() {
		// get the first one
		DaOpcClient client = null;

		if (!getOpcDaClients().isEmpty()) {
			client = getOpcDaClients().get(0);
		}
		return client;
	}

	public void setOpcDaClients(List<DaOpcClient> clients) {
		this.contextMap.put(OPC_DA_KEY, clients);
	}

	@SuppressWarnings("unchecked")
	public List<UaOpcClient> getOpcUaClients() {
		return (List<UaOpcClient>) this.contextMap.get(OPC_UA_KEY);
	}

	public UaOpcClient getOpcUaClient() {
		// get the first one
		UaOpcClient client = null;

		if (!getOpcUaClients().isEmpty()) {
			client = getOpcUaClients().get(0);
		}
		return client;
	}

	public void setOpcUaClients(List<UaOpcClient> clients) {
		this.contextMap.put(OPC_UA_KEY, clients);
	}

	@SuppressWarnings("unchecked")
	public List<PublisherSubscriber> getPublisherSubscribers() {
		return (List<PublisherSubscriber>) this.contextMap.get(PUB_SUB_KEY);
	}

	public PublisherSubscriber getPublisherSubscriber() {
		// get the first one
		PublisherSubscriber client = null;

		if (!getPublisherSubscribers().isEmpty()) {
			client = getPublisherSubscribers().get(0);
		}
		return client;
	}

	public void setPublisherSubscribers(List<PublisherSubscriber> clients) {
		this.contextMap.put(PUB_SUB_KEY, clients);
	}

	@SuppressWarnings("unchecked")
	public List<OeeHttpServer> getHttpServers() {
		return (List<OeeHttpServer>) this.contextMap.get(HTTP_KEY);
	}

	public OeeHttpServer getHttpServer() {
		// get the first one
		OeeHttpServer server = null;

		if (!getHttpServers().isEmpty()) {
			server = getHttpServers().get(0);
		}
		return server;
	}

	public void setHttpServers(List<OeeHttpServer> servers) {
		this.contextMap.put(HTTP_KEY, servers);
	}

}
