package org.point85.domain.collector;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.point85.domain.DomainUtils;
import org.point85.domain.db.DatabaseEvent;
import org.point85.domain.db.DatabaseEventClient;
import org.point85.domain.db.DatabaseEventListener;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.db.DatabaseEventStatus;
import org.point85.domain.file.FileEventClient;
import org.point85.domain.file.FileEventListener;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpEventListener;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.jms.JMSClient;
import org.point85.domain.jms.JMSEquipmentEventListener;
import org.point85.domain.jms.JMSSource;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.CollectorCommandMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.mqtt.MQTTClient;
import org.point85.domain.mqtt.MQTTEquipmentEventListener;
import org.point85.domain.mqtt.MQTTSource;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.da.OpcDaDataChangeListener;
import org.point85.domain.opc.da.OpcDaMonitoredGroup;
import org.point85.domain.opc.da.OpcDaMonitoredItem;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.da.OpcDaVariantType;
import org.point85.domain.opc.da.TagGroupInfo;
import org.point85.domain.opc.da.TagItemInfo;
import org.point85.domain.opc.ua.OpcUaAsynchListener;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.OeeEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class CollectorService
		implements HttpEventListener, OpcDaDataChangeListener, OpcUaAsynchListener, MessageListener,
		JMSEquipmentEventListener, DatabaseEventListener, FileEventListener, MQTTEquipmentEventListener {

	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorService.class);

	// sec between status checks
	private static final long HEARTBEAT_SEC = 60;

	// sec for heartbeat message to live in the queue
	private static final int HEARTBEAT_TTL_SEC = 3600;

	// sec for a status message to live in the queue
	private static final int STATUS_TTL_SEC = 3600;

	// sec for a event resolution message to live in the queue
	private static final int RESOLUTION_TTL_SEC = 3600;

	// thread pool service
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	// timer to broadcast status
	private Timer heartbeatTimer;

	// status task
	private HeartbeatTask heartbeatTask;

	// serializer
	protected Gson gson;

	// script execution context
	private OeeContext appContext;

	// resolver
	private EquipmentEventResolver equipmentResolver;

	// data collectors
	private List<DataCollector> collectors;

	// JVM host name
	private String hostname;

	// JVM host IP address
	private String ip;

	// data source information
	private final Map<String, OpcDaInfo> opcDaSubscriptionMap = new HashMap<>();
	private final Map<String, OpcUaInfo> opcUaSubscriptionMap = new HashMap<>();
	private final Map<String, HttpServerSource> httpServerMap = new HashMap<>();
	private final Map<String, MessageBrokerSource> messageBrokerMap = new HashMap<>();
	private final Map<String, JMSBrokerSource> jmsBrokerMap = new HashMap<>();
	private final Map<String, DatabaseServerSource> databaseServerMap = new HashMap<>();
	private final Map<String, FileServerSource> fileServerMap = new HashMap<>();
	private final Map<String, MQTTBrokerSource> mqttBrokerMap = new HashMap<>();

	private boolean webContainer = false;

	public CollectorService() {
		initialize();
	}

	private void initialize() {
		opcDaSubscriptionMap.clear();
		opcUaSubscriptionMap.clear();
		httpServerMap.clear();
		messageBrokerMap.clear();
		jmsBrokerMap.clear();
		databaseServerMap.clear();
		fileServerMap.clear();
		mqttBrokerMap.clear();

		gson = new Gson();
		appContext = new OeeContext();
		equipmentResolver = new EquipmentEventResolver();
		collectors = new ArrayList<>();
	}

	public OeeContext getAppContext() {
		return appContext;
	}

	// collect all HTTP server info
	private void buildHttpServers(EventResolver resolver) throws Exception {
		HttpSource source = (HttpSource) resolver.getDataSource();
		String id = source.getId();

		HttpServerSource serverSource = httpServerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info(
						"Found HTTP server specified for host " + source.getHost() + " on port " + source.getPort());
			}
			serverSource = new HttpServerSource(source);
			httpServerMap.put(id, serverSource);
		}
	}

	// collect all RMQ broker info
	private void buildMessagingBrokers(EventResolver resolver) throws Exception {
		MessagingSource source = (MessagingSource) resolver.getDataSource();
		String id = source.getId();

		MessageBrokerSource brokerSource = messageBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found RMQ broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new MessageBrokerSource(source);
			messageBrokerMap.put(id, brokerSource);
		}
	}

	// collect all JMS broker info
	private void buildJMSBrokers(EventResolver resolver) throws Exception {
		JMSSource source = (JMSSource) resolver.getDataSource();
		String id = source.getId();

		JMSBrokerSource brokerSource = jmsBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found JMS broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new JMSBrokerSource(source);
			jmsBrokerMap.put(id, brokerSource);
		}
	}

	// collect all MQTT broker info
	private void buildMQTTBrokers(EventResolver resolver) throws Exception {
		MQTTSource source = (MQTTSource) resolver.getDataSource();
		String id = source.getId();

		MQTTBrokerSource brokerSource = mqttBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info(
						"Found MQTT broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new MQTTBrokerSource(source);
			mqttBrokerMap.put(id, brokerSource);
		}
	}

	// collect all database server sources
	private void buildDatabaseServers(EventResolver resolver) {
		DatabaseEventSource source = (DatabaseEventSource) resolver.getDataSource();
		String id = source.getId();

		DatabaseServerSource dbSource = databaseServerMap.get(id);

		if (dbSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found database server specified for host " + source.getHost());
			}
			dbSource = new DatabaseServerSource(source, resolver.getUpdatePeriod());

			databaseServerMap.put(id, dbSource);
		}
	}

	// collect all file server info
	private void buildFileServers(EventResolver resolver) throws Exception {
		FileEventSource eventSource = (FileEventSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod();

		String id = eventSource.getId();

		FileServerSource serverSource = fileServerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found file server specified for host " + eventSource.getHost());
			}

			serverSource = new FileServerSource(eventSource);
			fileServerMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	private void connectToEventBrokers(Map<String, MessageBrokerSource> brokerSources) throws Exception {
		for (Entry<String, MessageBrokerSource> entry : brokerSources.entrySet()) {
			MessagingSource source = entry.getValue().getSource();

			MessagingClient pubsub = new MessagingClient();

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			// queue on each RMQ broker
			String queueName = "EVT_" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

			List<RoutingKey> routingKeys = new ArrayList<>();
			routingKeys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);

			pubsub.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, queueName, routingKeys, this);

			// add to context
			appContext.getMessagingClients().add(pubsub);

			if (logger.isInfoEnabled()) {
				logger.info("Started RMQ event pubsub: " + source.getId());
			}
		}
	}

	private void connectToJMSBrokers(Map<String, JMSBrokerSource> brokerSources) throws Exception {
		for (Entry<String, JMSBrokerSource> entry : brokerSources.entrySet()) {
			JMSSource source = entry.getValue().getSource();

			JMSClient jmsClient = new JMSClient();

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			jmsClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);

			// add to context
			appContext.getJMSClients().add(jmsClient);

			if (logger.isInfoEnabled()) {
				logger.info("Started JMS client: " + source.getId());
			}
		}
	}

	private void connectToMQTTBrokers(Map<String, MQTTBrokerSource> brokerSources) throws Exception {
		for (Entry<String, MQTTBrokerSource> entry : brokerSources.entrySet()) {
			MQTTSource source = entry.getValue().getSource();

			MQTTClient mqttClient = new MQTTClient();

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			mqttClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);

			// add to context
			appContext.getMQTTClients().add(mqttClient);

			if (logger.isInfoEnabled()) {
				logger.info("Started MQTT client: " + source.getId());
			}
		}
	}

	private void connectToDatabaseServers(Map<String, DatabaseServerSource> dbSources) throws Exception {
		for (Entry<String, DatabaseServerSource> entry : dbSources.entrySet()) {
			DatabaseServerSource source = entry.getValue();
			DatabaseEventSource eventSource = source.getSource();

			Integer pollingInterval = source.getPollingInterval();
			if (pollingInterval == null) {
				pollingInterval = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;
			}

			DatabaseEventClient dbClient = new DatabaseEventClient(this, pollingInterval);

			dbClient.connectToServer(eventSource.getId(), eventSource.getUserName(), eventSource.getUserPassword());

			dbClient.startPolling();

			// add to context
			appContext.getDatabaseEventClients().add(dbClient);

			if (logger.isInfoEnabled()) {
				logger.info("Connnected to database server: " + eventSource.getId());
			}
		}
	}

	private void startFilePolling(Map<String, FileServerSource> fileSources) throws Exception {
		for (Entry<String, FileServerSource> entry : fileSources.entrySet()) {
			// source of file events
			FileServerSource fileSource = entry.getValue();
			FileEventSource fileEventSource = fileSource.getEventSource();
			List<String> sourceIds = fileSource.getSourceIds();
			List<Integer> pollingIntervals = fileSource.getPollingIntervals();

			FileEventClient fileClient = new FileEventClient(this, fileEventSource, sourceIds, pollingIntervals);

			// add to context
			appContext.getFileEventClients().add(fileClient);

			if (logger.isInfoEnabled()) {
				logger.info("Polling files on server: " + fileEventSource.getId());
			}
			fileClient.startPolling();
		}
	}

	private void startHttpServers(Map<String, HttpServerSource> httpServerSources) throws Exception {
		for (Entry<String, HttpServerSource> entry : httpServerSources.entrySet()) {
			HttpSource source = entry.getValue().getSource();

			Integer port = source.getPort();

			if (logger.isInfoEnabled()) {
				logger.info("Starting embedded HTTP server on port " + port);
			}

			OeeHttpServer httpServer = new OeeHttpServer(port);
			httpServer.setDataChangeListener(this);
			httpServer.setAcceptingEventRequests(true);
			httpServer.startup();

			// add to context
			appContext.getHttpServers().add(httpServer);

			if (logger.isInfoEnabled()) {
				logger.info("Started HTTP server on port " + port);
			}
		}
	}

	private void buildOpcUaSubscriptions(EventResolver resolver) throws Exception {
		OpcUaSource source = (OpcUaSource) resolver.getDataSource();
		String url = source.getEndpointUrl();

		OpcUaInfo uaInfo = opcUaSubscriptionMap.get(url);

		if (uaInfo == null) {
			uaInfo = new OpcUaInfo(source);
			opcUaSubscriptionMap.put(url, uaInfo);
		}

		String nodeName = resolver.getSourceId();
		NodeId monitoredNodeId = NodeId.parse(nodeName);

		uaInfo.getMonitoredNodes().add(monitoredNodeId);

		double publishingInterval = resolver.getUpdatePeriod().doubleValue();

		if (publishingInterval < uaInfo.getPublishingInterval()) {
			uaInfo.setPublishingInterval(publishingInterval);
		}
	}

	public void subscribeToOpcUaSources(Map<String, OpcUaInfo> uaSubscriptions) throws Exception {
		for (Entry<String, OpcUaInfo> entry : uaSubscriptions.entrySet()) {
			OpcUaInfo uaInfo = entry.getValue();

			UaOpcClient uaClient = new UaOpcClient();
			uaClient.connect(uaInfo.getSource());

			// add to context
			appContext.getOpcUaClients().add(uaClient);

			uaClient.registerAsynchListener(this);

			double publishingInterval = uaInfo.getPublishingInterval();

			for (NodeId monitoredNodeId : entry.getValue().getMonitoredNodes()) {
				uaClient.subscribe(monitoredNodeId, publishingInterval, null);
			}
		}
	}

	private void buildOpcDaSubscriptions(EventResolver resolver) throws Exception {
		// equipment (group name)
		String equipmentName = resolver.getEquipment().getName();

		// OPC DA server
		OpcDaSource daSource = (OpcDaSource) resolver.getDataSource();

		// access path
		String sourceId = resolver.getSourceId();

		// get subscription
		String mapKey = daSource.getHost() + "." + daSource.getProgId();

		OpcDaInfo daSubscription = opcDaSubscriptionMap.get(mapKey);

		if (daSubscription == null) {
			daSubscription = new OpcDaInfo(daSource);
			opcDaSubscriptionMap.put(mapKey, daSubscription);
		}

		// create the tag info
		TagItemInfo tagItem = new TagItemInfo(sourceId);
		Integer updatePeriod = resolver.getUpdatePeriod();
		
		if (updatePeriod == null) {
			updatePeriod = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;
		}
		tagItem.setUpdatePeriod(updatePeriod);
		daSubscription.addTagItem(equipmentName, tagItem);

		if (logger.isInfoEnabled()) {
			logger.info("Found subscription. Source: " + daSource + ", equipment: " + equipmentName + ", tag: "
					+ tagItem.getPathName());
		}
	}

	private void monitorOpcDaTags(Map<String, OpcDaInfo> daSubscriptions) throws Exception {
		for (Entry<String, OpcDaInfo> entry : daSubscriptions.entrySet()) {

			// connect to data source
			OpcDaInfo subscribingClient = entry.getValue();
			OpcDaSource daSource = subscribingClient.getSource();

			DaOpcClient opcDaClient = new DaOpcClient();

			opcDaClient.connect(daSource);

			// put in context
			appContext.getOpcDaClients().add(opcDaClient);

			// subscribe to tags, one group per equipment
			Map<String, List<TagItemInfo>> subscribedItems = entry.getValue().getSubscribedItems();

			for (Entry<String, List<TagItemInfo>> tagsEntry : subscribedItems.entrySet()) {

				// create OPC DA group, name = equipment
				TagGroupInfo tagGroup = new TagGroupInfo(tagsEntry.getKey());

				List<TagItemInfo> tagItems = tagsEntry.getValue();

				for (TagItemInfo tagItem : tagItems) {
					int period = tagItem.getUpdatePeriod();

					if (period < tagGroup.getUpdatePeriod()) {
						tagGroup.setUpdatePeriod(period);

						if (logger.isInfoEnabled()) {
							logger.info("Setting update period to " + tagGroup.getUpdatePeriod());
						}
					}

					tagGroup.addTagItem(tagItem);
				}

				// register for data change events
				OpcDaMonitoredGroup opcDaGroup = opcDaClient.registerTags(tagGroup, this);

				// start monitoring
				opcDaGroup.startMonitoring();
			}
		}
	}

	private void buildDataSources() throws Exception {
		// query for configuration by host name or IP address
		List<String> hostNames = new ArrayList<>(2);
		hostNames.add(hostname);
		hostNames.add(ip);

		// fetch script resolvers from database
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.DEV);
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);
		List<EventResolver> resolvers = PersistenceService.instance().fetchEventResolversByHost(hostNames, states);

		if (resolvers.isEmpty()) {
			logger.warn("No resolvers found for hosts " + hostNames);
		}

		for (EventResolver resolver : resolvers) {
			// build list of runnable collectors
			DataCollector collector = resolver.getCollector();

			if (!collectors.contains(collector)) {
				collectors.add(collector);

				if (logger.isInfoEnabled()) {
					logger.info("Found data collector " + collector.getName() + ", for host " + collector.getHost()
							+ " in state " + collector.getCollectorState() + ", resolver: " + resolver);
				}
			}

			// gather data for each data source
			if (!resolver.getCollector().getCollectorState().equals(CollectorState.DEV)) {
				switch (resolver.getDataSource().getDataSourceType()) {
				case OPC_DA: {
					buildOpcDaSubscriptions(resolver);
					break;
				}

				case HTTP: {
					buildHttpServers(resolver);
					break;
				}

				case OPC_UA: {
					buildOpcUaSubscriptions(resolver);
					break;
				}

				case MESSAGING: {
					buildMessagingBrokers(resolver);
					break;
				}

				case JMS: {
					buildJMSBrokers(resolver);
					break;
				}

				case MQTT: {
					buildMQTTBrokers(resolver);
					break;
				}

				case DATABASE: {
					buildDatabaseServers(resolver);
					break;
				}

				case FILE: {
					buildFileServers(resolver);
					break;
				}

				default:
					break;
				}
			}
		}
	}

	public synchronized void startDataCollection() throws Exception {

		// collect data for OPC DA
		monitorOpcDaTags(opcDaSubscriptionMap);

		// collect data for OPC UA
		subscribeToOpcUaSources(opcUaSubscriptionMap);

		// collect data for HTTP
		startHttpServers(httpServerMap);

		// collect data for RMQ and receive commands
		connectToEventBrokers(messageBrokerMap);

		// collect equipment events for JMS
		connectToJMSBrokers(jmsBrokerMap);

		// poll database servers for events in the interface table
		connectToDatabaseServers(databaseServerMap);

		// collect equipment events for MQTT
		connectToMQTTBrokers(mqttBrokerMap);

		// poll file servers
		startFilePolling(fileServerMap);

		if (logger.isInfoEnabled()) {
			logger.info("Startup finished.");
		}

		// update collector state from READY to RUNNING
		saveCollectorState(CollectorState.RUNNING);
	}

	private String getId() {
		return hostname + " (" + ip + ")";
	}

	public void startup() throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info("Beginning startup");
		}

		// our host
		InetAddress address = InetAddress.getLocalHost();
		hostname = address.getHostName();
		ip = address.getHostAddress();

		if (logger.isInfoEnabled()) {
			logger.info("Configuring server for host " + getId());
		}

		// configure all of the data sources
		buildDataSources();

		// add a collector for a web server container if necessary
		addWebCollector();

		// connect to broker for notifications and commands
		startNotifications();

		// start collecting data
		startDataCollection();

		// notify monitors
		onInformation("Collector server started on host " + getId());
	}

	public boolean isWebContainer() {
		return webContainer;
	}

	public void setWebContainer(boolean web) {
		this.webContainer = web;
	}

	private void addWebCollector() {
		if (!webContainer) {
			return;
		}

		// search for a collector on this host
		DataCollector thisCollector = null;

		for (DataCollector collector : collectors) {
			if (collector.getHost().equals(hostname)) {
				thisCollector = collector;
				break;
			}
		}

		if (thisCollector != null) {
			// already a collector
			return;
		}

		// fetch from database
		List<String> hostNames = new ArrayList<String>();
		hostNames.add(hostname);

		// get collector on our host
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);

		List<DataCollector> ourCollectors = PersistenceService.instance().fetchCollectorsByHostAndState(hostNames,
				states);

		if (!ourCollectors.isEmpty()) {
			DataCollector ourCollector = ourCollectors.get(0);
			collectors.add(ourCollector);

			if (logger.isInfoEnabled()) {
				logger.info("Added data collector for this host " + ourCollector.getName() + " in state "
						+ ourCollector.getCollectorState());
			}
		}
	}

	private synchronized void startNotifications() throws Exception {
		// connect to notification brokers for commands
		Map<String, MessagingClient> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			String brokerHostName = collector.getBrokerHost();

			if (brokerHostName == null || brokerHostName.length() == 0) {
				continue;
			}
			Integer brokerPort = collector.getBrokerPort();

			String key = brokerHostName + ":" + brokerPort;

			if (pubSubs.get(key) == null) {
				// new publisher
				MessagingClient pubsub = new MessagingClient();
				pubSubs.put(key, pubsub);

				// queue
				String queueName = "CMD_" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

				// connect to broker and subscribe for commands
				List<RoutingKey> routingKeys = new ArrayList<>();
				routingKeys.add(RoutingKey.COMMAND_MESSAGE);

				pubsub.startUp(brokerHostName, brokerPort, collector.getBrokerUserName(),
						collector.getBrokerUserPassword(), queueName, routingKeys, this);

				// add to context
				appContext.getMessagingClients().add(pubsub);

				if (logger.isInfoEnabled()) {
					logger.info("Connected to RMQ broker " + key + " for collector " + collector.getName());
				}
			}
		}

		// maybe start status publishing
		if (!appContext.getMessagingClients().isEmpty() && heartbeatTimer == null) {
			// create timer and task
			heartbeatTimer = new Timer();
			heartbeatTask = new HeartbeatTask();
			heartbeatTimer.schedule(heartbeatTask, HEARTBEAT_SEC * 1000, HEARTBEAT_SEC * 1000);

			if (logger.isInfoEnabled()) {
				logger.info("Scheduled heartbeat task for interval (sec): " + HEARTBEAT_SEC);
			}
		}
	}

	private synchronized void sendNotification(String text, NotificationSeverity severity) {
		if (appContext.getMessagingClients().isEmpty()) {
			return;
		}

		CollectorNotificationMessage message = new CollectorNotificationMessage(hostname, ip);
		message.setText(text);
		message.setSeverity(severity);

		for (MessagingClient pubSub : appContext.getMessagingClients()) {
			try {
				pubSub.publish(message, RoutingKey.NOTIFICATION_MESSAGE, STATUS_TTL_SEC);
			} catch (Exception e) {
				onException("Unable to publish notification.", e);
			}
		}
	}

	private void saveCollectorState(CollectorState state) throws Exception {
		List<DataCollector> savedCollectors = new ArrayList<>();

		for (DataCollector collector : collectors) {

			CollectorState currentState = collector.getCollectorState();

			if (currentState.isValidTransition(state)) {
				collector.setCollectorState(state);
				DataCollector saved = (DataCollector) PersistenceService.instance().save(collector);

				if (logger.isInfoEnabled()) {
					logger.info("Saved collector " + collector.getName());
				}
				savedCollectors.add(saved);
			} else {
				if (logger.isInfoEnabled()) {
					logger.info("Invalid state " + state + " from state " + currentState);
				}
			}
		}
		collectors = savedCollectors;
	}

	public synchronized void stopNotifications() throws Exception {
		for (MessagingClient pubsub : appContext.getMessagingClients()) {
			pubsub.shutDown();
		}
		appContext.getMessagingClients().clear();
	}

	public synchronized void stopDataCollection() throws Exception {
		// clear resolution caches
		equipmentResolver.clearCache();

		// stop polling file servers
		for (FileEventClient fileClient : appContext.getFileEventClients()) {
			fileClient.stopPolling();
			onInformation("Stopped polling file server " + fileClient.getFileEventSource().getHost());
		}
		appContext.getFileEventClients().clear();

		// disconnect from database servers
		for (DatabaseEventClient dbClient : appContext.getDatabaseEventClients()) {
			dbClient.disconnect();
			onInformation("Disconnected from database server " + dbClient.getJdbcUrl());
		}
		appContext.getDatabaseEventClients().clear();

		// shutdown HTTP servers
		for (OeeHttpServer httpServer : appContext.getHttpServers()) {
			httpServer.shutdown();
			onInformation("Shutdown HTTP server on host " + httpServer.getHostname());
		}
		appContext.getHttpServers().clear();

		// disconnect OPC DA clients
		for (DaOpcClient daClient : appContext.getOpcDaClients()) {
			daClient.disconnect();
			onInformation("Disconnected from OPC DA client ");
		}
		appContext.getOpcDaClients().clear();

		// disconnect from OPC UA clients
		for (UaOpcClient uaClient : appContext.getOpcUaClients()) {
			uaClient.disconnect();
			onInformation("Disconnected from OPC UA client ");
		}
		appContext.getOpcUaClients().clear();

		// disconnect from RMQ brokers
		for (MessagingClient pubsub : appContext.getMessagingClients()) {
			onInformation("Disconnecting from pubsub with binding key " + pubsub.getBindingKey());
			pubsub.shutDown();
		}
		appContext.getMessagingClients().clear();

		// set back to ready
		saveCollectorState(CollectorState.READY);
	}

	public void shutdown() {
		// notify monitors
		onInformation("Collector server shutting down on host " + getId());

		try {
			// stop data collection
			stopDataCollection();

			// stop RMQ notifications
			stopNotifications();
		} catch (Exception e) {
			onException("Unable to stop data collection.", e);
		}

		// shutdown executor service
		executorService.shutdown();

		try {
			if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Shutdown finished.");
		}
	}

	public void restart() throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Restarting");
		}

		// stop current activity
		stopDataCollection();
		stopNotifications();

		initialize();

		startup();
	}

	public void restartDataCollection() throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Restarting data collection");
		}

		stopDataCollection();
		startDataCollection();

		stopNotifications();
		startNotifications();

		// notify monitors
		onInformation("Restarted data collection on host " + getId());
	}

	public void unsubscribeFromDataSource() throws Exception {
		// DA clients
		for (DaOpcClient daClient : appContext.getOpcDaClients()) {
			for (OpcDaMonitoredGroup group : daClient.getMonitoredGroups()) {
				group.unregisterDataChangeListener(this);
			}
		}

		// UA clients
		for (UaOpcClient uaClient : appContext.getOpcUaClients()) {
			uaClient.unregisterAsynchListener(this);
		}

		// database polling
		for (DatabaseEventClient dbClient : appContext.getDatabaseEventClients()) {
			dbClient.stopPolling();
		}

		// file share polling
		for (FileEventClient fileClient : appContext.getFileEventClients()) {
			fileClient.stopPolling();
		}

		// HTTP servers
		for (OeeHttpServer httpServer : appContext.getHttpServers()) {
			httpServer.shutdown();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Unsubscribed from data sources.");
		}
	}

	public void subscribeToDataSource() throws Exception {
		// DA clients
		for (DaOpcClient daClient : appContext.getOpcDaClients()) {
			for (OpcDaMonitoredGroup group : daClient.getMonitoredGroups()) {
				group.registerDataChangeListener(this);
			}
		}

		// UA clients
		for (UaOpcClient uaClient : appContext.getOpcUaClients()) {
			uaClient.registerAsynchListener(this);
		}

		// database polling
		for (DatabaseEventClient dbClient : appContext.getDatabaseEventClients()) {
			dbClient.startPolling();
		}

		// file share polling
		for (FileEventClient fileClient : appContext.getFileEventClients()) {
			fileClient.startPolling();
		}

		// HTTP servers
		for (OeeHttpServer httpServer : appContext.getHttpServers()) {
			httpServer.startup();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Subscribed to data sources.");
		}
	}

	protected ExecutorService getExecutorService() {
		return this.executorService;
	}

	// HTTP request
	@Override
	public void onHttpEquipmentEvent(String sourceId, String dataValue, OffsetDateTime timestamp) {
		getExecutorService().execute(new HttpTask(sourceId, dataValue, timestamp));
	}

	// File request
	@Override
	public void resolveFileEvents(FileEventClient client, String sourceId, List<File> files) {
		getExecutorService().execute(new FileTask(client, sourceId, files));
	}

	@Override
	public void onOpcDaDataChange(OpcDaMonitoredItem item) {
		// execute on separate thread
		getExecutorService().execute(new OpcDaTask(item));
	}

	private void purgeRecords(OeeEvent event) throws Exception {
		Equipment equipment = event.getEquipment();

		Duration days = equipment.findRetentionPeriod();

		if (days == null) {
			days = Equipment.DEFAULT_RETENTION_PERIOD;
		}

		OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days.toDays());

		logger.info("Purging records for equipment " + equipment.getName() + " older than " + cutoff);

		// purge database tables
		PersistenceService.instance().purge(equipment, cutoff);
	}

	public void saveOeeEvent(OeeEvent event) throws Exception {
		Equipment equipment = event.getEquipment();
		Duration days = equipment.findRetentionPeriod();

		if (days != null && days.equals(Duration.ZERO)) {
			// no need to save or purge
			if (logger.isInfoEnabled()) {
				logger.info("Retention period is zero.  No record will be saved.");
			}
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Saving OEE event to database: " + event);
		}

		List<KeyedObject> records = new ArrayList<>();
		records.add(event);

		// close off previous events if not summarized
		OeeEventType type = event.getEventType();

		if (!type.isProduction() && event.getEndTime() == null) {
			// availability, material or job change
			OeeEvent lastRecord = PersistenceService.instance().fetchLastEvent(event.getEquipment(), type);

			if (lastRecord != null) {
				lastRecord.setEndTime(event.getStartTime());
				Duration duration = Duration.between(lastRecord.getStartTime(), lastRecord.getEndTime());
				lastRecord.setDuration(duration);

				records.add(lastRecord);
			}
		}

		// save records
		PersistenceService.instance().save(records);

		// purge old data
		if (!type.isProduction()) {
			purgeRecords(event);
		}
	}

	@Override
	public void onOpcUaRead(List<DataValue> dataValues) {
		// no asynch read
	}

	@Override
	public void onOpcUaWrite(List<StatusCode> statusCodes) {
		// no asynch write
	}

	@Override
	public void onOpcUaSubscription(DataValue dataValue, UaMonitoredItem item) {
		getExecutorService().execute(new OpcUaTask(dataValue, item));
	}

	public void onException(String preface, Exception any) {
		// dump stack trace to log
		any.printStackTrace();

		// notify monitors
		String text = any.getMessage();

		if (preface != null) {
			text = preface + "  " + text;
		}

		// log it
		logger.error(text);

		// send to monitors
		sendNotification(text, NotificationSeverity.ERROR);
	}

	public void onInformation(String text) {
		// log it
		logger.info(text);

		// send to monitors
		sendNotification(text, NotificationSeverity.INFO);
	}

	public void onWarning(String text) {
		// log it
		logger.warn(text);

		// send to monitors
		sendNotification(text, NotificationSeverity.WARNING);
	}

	@Override
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) {
		if (channel == null || message == null) {
			return;
		}

		// ack it now
		try {
			channel.basicAck(envelope.getDeliveryTag(), MessagingClient.ACK_MULTIPLE);
		} catch (Exception e) {
			onException("Failed to ack message.", e);
			return;
		}
		// execute on worker thread
		MessageTask task = new MessageTask(channel, envelope, message);
		executorService.execute(task);
	}

	@Override
	public void onJMSEquipmentEvent(EquipmentEventMessage message) {
		// execute on worker thread
		executorService.execute(new JMSTask(message));
	}

	@Override
	public void onMQTTEquipmentEvent(EquipmentEventMessage message) {
		// execute on worker thread
		executorService.execute(new MQTTTask(message));
	}

	@Override
	public void resolveDatabaseEvents(DatabaseEventClient databaseClient, List<DatabaseEvent> events) {
		for (DatabaseEvent event : events) {
			// execute on worker thread
			DatabaseEventTask task = new DatabaseEventTask(databaseClient, event);
			executorService.execute(task);
		}
	}

	// subscribed OPC DA items by source
	private class OpcDaInfo {
		private final OpcDaSource source;

		private final Map<String, List<TagItemInfo>> subscribedItems = new HashMap<>();

		OpcDaInfo(OpcDaSource source) {
			this.source = source;
		}

		private void addTagItem(String equipmentName, TagItemInfo item) {
			List<TagItemInfo> items = subscribedItems.get(equipmentName);

			if (items == null) {
				items = new ArrayList<>();
				subscribedItems.put(equipmentName, items);
			}
			items.add(item);
		}

		private Map<String, List<TagItemInfo>> getSubscribedItems() {
			return subscribedItems;
		}

		private OpcDaSource getSource() {
			return source;
		}
	}

	// subscribed OPC UA nodes by source
	private class OpcUaInfo {
		private final OpcUaSource source;

		private List<NodeId> monitoredNodes;

		private double publishingInterval = Double.MAX_VALUE;

		OpcUaInfo(OpcUaSource source) {
			this.source = source;
		}

		private List<NodeId> getMonitoredNodes() {
			if (monitoredNodes == null) {
				monitoredNodes = new ArrayList<>();
			}
			return monitoredNodes;
		}

		private double getPublishingInterval() {
			return publishingInterval;
		}

		private void setPublishingInterval(double publishingInterval) {
			this.publishingInterval = publishingInterval;
		}

		private OpcUaSource getSource() {
			return source;
		}
	}

	// RMQ brokers
	private class MessageBrokerSource {
		private final MessagingSource source;

		MessageBrokerSource(MessagingSource source) {
			this.source = source;
		}

		private MessagingSource getSource() {
			return source;
		}
	}

	// JMS brokers
	private class JMSBrokerSource {
		private final JMSSource source;

		JMSBrokerSource(JMSSource source) {
			this.source = source;
		}

		private JMSSource getSource() {
			return source;
		}
	}

	// MQTT brokers
	private class MQTTBrokerSource {
		private final MQTTSource source;

		MQTTBrokerSource(MQTTSource source) {
			this.source = source;
		}

		private MQTTSource getSource() {
			return source;
		}
	}

	// HTTP servers
	private class HttpServerSource {
		private final HttpSource source;

		HttpServerSource(HttpSource source) {
			this.source = source;
		}

		private HttpSource getSource() {
			return source;
		}
	}

	// database servers
	private class DatabaseServerSource {
		private final DatabaseEventSource source;
		private final Integer pollingInterval;

		DatabaseServerSource(DatabaseEventSource source, Integer pollingInterval) {
			this.source = source;
			this.pollingInterval = pollingInterval;
		}

		private DatabaseEventSource getSource() {
			return source;
		}

		private Integer getPollingInterval() {
			return pollingInterval;
		}
	}

	// file servers
	private class FileServerSource {
		private final FileEventSource eventSource;
		private final List<String> sourceIds = new ArrayList<>();
		private final List<Integer> pollingIntervals = new ArrayList<>();

		private FileServerSource(FileEventSource eventSource) {
			this.eventSource = eventSource;
		}

		private FileEventSource getEventSource() {
			return eventSource;
		}

		private List<String> getSourceIds() {
			return sourceIds;
		}

		private List<Integer> getPollingIntervals() {
			return pollingIntervals;
		}
	}

	// handle the HTTP callback
	private class HttpTask implements Runnable {
		private final String sourceId;
		private final String dataValue;
		private final OffsetDateTime timestamp;

		HttpTask(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.sourceId = sourceId;
			this.dataValue = dataValue;
			this.timestamp = timestamp;
		}

		@Override
		public void run() {
			try {
				if (logger.isInfoEnabled()) {
					logger.info(
							"HTTP event, source: " + sourceId + ", value: " + dataValue + ", timestamp: " + timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
			}
		}
	}

	// handle the OPC UA callback
	private class OpcUaTask implements Runnable {
		private final DataValue uaValue;
		private final UaMonitoredItem item;

		OpcUaTask(DataValue dataValue, UaMonitoredItem item) {
			this.uaValue = dataValue;
			this.item = item;
		}

		@Override
		public void run() {
			try {
				Object dataValue = uaValue.getValue().getValue();
				String sourceId = item.getReadValueId().getNodeId().toParseableString();
				DateTime dt = uaValue.getServerTime();
				OffsetDateTime timestamp = DomainUtils.localTimeFromDateTime(dt);

				if (logger.isInfoEnabled()) {
					logger.info("OPC UA subscription, node: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

			} catch (Exception e) {
				onException("Unable to invoke OPC UA script resolver.", e);
			}
		}
	}

	public synchronized void recordResolution(OeeEvent resolvedEvent) throws Exception {
		if (resolvedEvent.getEndTime() != null && resolvedEvent.getDuration() != null) {
			Duration delta = Duration.between(resolvedEvent.getStartTime(), resolvedEvent.getEndTime());

			if (delta.compareTo(resolvedEvent.getDuration()) < 0) {
				throw new Exception("The event duration of " + resolvedEvent.getDuration()
						+ " cannot be greater than the time period duration of " + delta);
			}
		}

		// save in database
		saveOeeEvent(resolvedEvent);

		// send event message
		sendResolutionMessage(resolvedEvent);
	}

	private void sendResolutionMessage(OeeEvent resolvedEvent) {
		try {
			if (appContext.getMessagingClients().size() == 0) {
				return;
			}

			// send resolution message to each subscriber
			CollectorResolvedEventMessage message = new CollectorResolvedEventMessage(hostname, ip);
			message.fromResolvedEvent(resolvedEvent);

			for (MessagingClient pubsub : appContext.getMessagingClients()) {
				pubsub.publish(message, RoutingKey.RESOLVED_EVENT, RESOLUTION_TTL_SEC);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType()
						+ " for resolver type " + message.getResolverType());
			}
		} catch (Exception e) {
			onException("Sending resolved event message failed.", e);
		}
	}

	private void resolveEvent(String sourceId, Object dataValue, OffsetDateTime timestamp) throws Exception {
		// find resolver
		EventResolver eventResolver = equipmentResolver.getResolver(sourceId);

		if (eventResolver == null) {
			throw new Exception("Unable to find an event resolver for source id " + sourceId);
		}

		OeeEvent resolvedDataItem = equipmentResolver.invokeResolver(eventResolver, getAppContext(), dataValue,
				timestamp);

		if (!eventResolver.isWatchMode()) {
			recordResolution(resolvedDataItem);
		}
	}

	/********************* OPC DA ***********************************/

	// handle the OPC DA callback
	private class OpcDaTask implements Runnable {
		private final OpcDaMonitoredItem item;

		OpcDaTask(OpcDaMonitoredItem item) {
			this.item = item;
		}

		@Override
		public void run() {
			try {
				OpcDaVariant varientValue = item.getValue();

				Object dataValue = null;

				if (varientValue.getDataType().equals(OpcDaVariantType.STRING)) {
					dataValue = varientValue.getValueAsString();
				} else {
					dataValue = varientValue.getValueAsNumber();
				}

				String sourceId = item.getPathName();
				OffsetDateTime timestamp = item.getLocalTimestamp();

				if (logger.isInfoEnabled()) {
					logger.info("OPC DA data change, group: " + item.getGroup().getName() + ", item: " + sourceId
							+ ", value: " + item.getValueString() + ", timestamp: " + timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

			} catch (Exception e) {
				onException("Unable to invoke OPC DA script resolver.", e);
			}
		}
	}

	/********************* MessageHandler ***********************************/
	private class MessageTask implements Runnable {

		private final Channel channel;
		private final Envelope envelope;
		private final ApplicationMessage message;

		MessageTask(Channel channel, Envelope envelope, ApplicationMessage message) {
			this.envelope = envelope;
			this.message = message;
			this.channel = channel;
		}

		@Override
		public void run() {
			MessageType type = message.getMessageType();

			try {
				if (type.equals(MessageType.EQUIPMENT_EVENT)) {
					EquipmentEventMessage eventMessage = (EquipmentEventMessage) message;

					String sourceId = eventMessage.getSourceId();
					String dataValue = eventMessage.getValue();
					OffsetDateTime timestamp = eventMessage.getDateTime();

					if (logger.isInfoEnabled()) {
						logger.info("RMQ equipment event, source: " + sourceId + ", value: " + dataValue
								+ ", timestamp: " + timestamp);
					}

					// resolve event
					resolveEvent(sourceId, dataValue, timestamp);

				} else if (type.equals(MessageType.COMMAND)) {
					CollectorCommandMessage commandMessage = (CollectorCommandMessage) message;

					if (commandMessage.getCommand().equals(CollectorCommandMessage.CMD_RESTART)) {
						logger.info("Received restart command");
						restart();
					}
				}
			} catch (Exception e) {
				// processing failed
				onException("Unable to process event " + type, e);
			} finally {
				// ack message
				try {
					if (channel.isOpen()) {
						channel.basicAck(envelope.getDeliveryTag(), MessagingClient.ACK_MULTIPLE);
					}
				} catch (Exception ex) {
					// ack failed
					onException("Unable to ack message.", ex);
				}
			}
		}
	}

	/********************* JMS Handler ***********************************/
	private class JMSTask implements Runnable {

		private final EquipmentEventMessage eventMessage;

		JMSTask(EquipmentEventMessage message) {
			this.eventMessage = message;
		}

		@Override
		public void run() {
			try {
				String sourceId = eventMessage.getSourceId();
				String dataValue = eventMessage.getValue();
				OffsetDateTime timestamp = eventMessage.getDateTime();

				if (logger.isInfoEnabled()) {
					logger.info("JMS equipment event, source: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

			} catch (Exception e) {
				// processing failed
				onException("Unable to process JMS equipment event ", e);
			}
		}
	}

	/********************* MQTT Handler ***********************************/
	private class MQTTTask implements Runnable {

		private final EquipmentEventMessage eventMessage;

		MQTTTask(EquipmentEventMessage message) {
			this.eventMessage = message;
		}

		@Override
		public void run() {
			try {
				String sourceId = eventMessage.getSourceId();
				String dataValue = eventMessage.getValue();
				OffsetDateTime timestamp = eventMessage.getDateTime();

				if (logger.isInfoEnabled()) {
					logger.info("JMS equipment event, source: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

			} catch (Exception e) {
				// processing failed
				onException("Unable to process MQTT equipment event ", e);
			}
		}
	}

	/********************* Status Task ***********************************/
	private class HeartbeatTask extends TimerTask {
		@Override
		public void run() {
			try {
				if (appContext.getMessagingClients().size() == 0) {
					return;
				}

				// send status message to each PubSub
				CollectorServerStatusMessage message = new CollectorServerStatusMessage(hostname, ip);

				for (MessagingClient pubsub : appContext.getMessagingClients()) {
					pubsub.publish(message, RoutingKey.NOTIFICATION_STATUS, HEARTBEAT_TTL_SEC);
				}

				if (logger.isInfoEnabled()) {
					logger.info("Sent status message for host " + getId());
				}
			} catch (Exception e) {
				onException("Sending server status message failed.", e);
			}
		}
	}

	/********************* Database Event Task ***********************************/
	private class DatabaseEventTask implements Runnable {
		private final DatabaseEventClient databaseClient;
		private final DatabaseEvent databaseEvent;

		DatabaseEventTask(DatabaseEventClient databaseClient, DatabaseEvent databaseEvent) {
			this.databaseClient = databaseClient;
			this.databaseEvent = databaseEvent;
		}

		@Override
		public void run() {
			try {
				String sourceId = databaseEvent.getSourceId();
				String dataValue = databaseEvent.getInputValue();
				OffsetDateTime timestamp = databaseEvent.getTime();

				if (logger.isInfoEnabled()) {
					logger.info("Database event, source: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ timestamp);
				}

				// set status to processing
				databaseEvent.setStatus(DatabaseEventStatus.PROCESSING);
				databaseEvent.setError(null);

				try {
					databaseClient.save(databaseEvent);
				} catch (Exception ex) {
					onException("Unable to save database event.", ex);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp);

				// pass
				databaseEvent.setStatus(DatabaseEventStatus.PASS);
				databaseEvent.setError(null);

			} catch (Exception e) {
				// fail
				databaseEvent.setStatus(DatabaseEventStatus.FAIL);
				databaseEvent.setError(e.getMessage());

				onException("Unable to invoke script resolver.", e);
			} finally {
				try {
					databaseClient.save(databaseEvent);
				} catch (Exception ex) {
					onException("Unable to save database event.", ex);
				}
			}
		}
	}

	// handle the File event callback
	private class FileTask implements Runnable {
		private final FileEventClient fileClient;
		private String sourceId;
		private final List<File> files;

		FileTask(FileEventClient fileClient, String sourceId, List<File> files) {
			this.fileClient = fileClient;
			this.sourceId = sourceId;
			this.files = files;
		}

		@Override
		public void run() {
			for (File file : files) {
				try {
					// event time (unless set by script)
					OffsetDateTime timestamp = fileClient.getFileService().extractTimestamp(file);

					if (logger.isInfoEnabled()) {
						logger.info("File event, file: " + file.getName() + ", source: " + sourceId + ", timestamp: "
								+ timestamp);
					}

					// read contents in ready folder
					String dataValue = fileClient.readFile(file);

					// move to in-process
					fileClient.moveFile(file, FileEventClient.READY_FOLDER, FileEventClient.PROCESSING_FOLDER);

					// resolve event
					resolveEvent(sourceId, dataValue, timestamp);

					// move to pass folder
					fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.PASS_FOLDER);

				} catch (Exception e) {
					onException("Unable to invoke script resolver.", e);

					// fail
					try {
						fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.FAIL_FOLDER, e);
					} catch (IOException ex) {
						onException("Unable to move file.", ex);
					}
				}
			}
		}
	}
}
