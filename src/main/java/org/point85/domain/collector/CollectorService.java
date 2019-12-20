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
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.jms.JmsMessageListener;
import org.point85.domain.jms.JmsSource;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorCommandMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.modbus.ModbusEvent;
import org.point85.domain.modbus.ModbusEventListener;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.mqtt.MqttMessageListener;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.mqtt.QualityOfService;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.da.OpcDaDataChangeListener;
import org.point85.domain.opc.da.OpcDaMonitoredGroup;
import org.point85.domain.opc.da.OpcDaMonitoredItem;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.da.TagGroupInfo;
import org.point85.domain.opc.da.TagItemInfo;
import org.point85.domain.opc.ua.OpcUaAsynchListener;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Reason;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.rmq.RoutingKey;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.OeeEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class CollectorService
		implements HttpEventListener, OpcDaDataChangeListener, OpcUaAsynchListener, RmqMessageListener,
		JmsMessageListener, DatabaseEventListener, FileEventListener, MqttMessageListener, ModbusEventListener {

	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorService.class);

	// sec between status checks
	private static final long HEARTBEAT_SEC = 60;

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

	// flag for manual collector
	private boolean isManual = false;

	// name of a specific collector on this host
	private String collectorName = "Default";

	// data source information
	private final Map<String, OpcDaInfo> opcDaSubscriptionMap = new HashMap<>();
	private final Map<String, OpcUaInfo> opcUaSubscriptionMap = new HashMap<>();
	private final Map<String, HttpServerSource> httpServerMap = new HashMap<>();
	private final Map<String, RmqBrokerSource> rmqBrokerMap = new HashMap<>();
	private final Map<String, JmsBrokerSource> jmsBrokerMap = new HashMap<>();
	private final Map<String, PolledSource> databaseServerMap = new HashMap<>();
	private final Map<String, PolledSource> fileServerMap = new HashMap<>();
	private final Map<String, PolledSource> modbusSlaveMap = new HashMap<>();
	private final Map<String, MqttBrokerSource> mqttBrokerMap = new HashMap<>();

	public CollectorService() {
		initialize();
	}

	public CollectorService(String collectorName) {
		this.collectorName = collectorName;
		initialize();
	}

	public CollectorService(boolean isManual) {
		this.isManual = isManual;
		initialize();
	}

	private void initialize() {
		opcDaSubscriptionMap.clear();
		opcUaSubscriptionMap.clear();
		httpServerMap.clear();
		rmqBrokerMap.clear();
		jmsBrokerMap.clear();
		databaseServerMap.clear();
		fileServerMap.clear();
		mqttBrokerMap.clear();

		gson = new Gson();
		appContext = new OeeContext();
		equipmentResolver = new EquipmentEventResolver();
		collectors = new ArrayList<>();
	}

	public boolean isManual() {
		return this.isManual;
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
		RmqSource source = (RmqSource) resolver.getDataSource();
		String id = source.getId();

		RmqBrokerSource brokerSource = rmqBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found RMQ broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new RmqBrokerSource(source);
			rmqBrokerMap.put(id, brokerSource);
		}
	}

	// collect all JMS broker info
	private void buildJMSBrokers(EventResolver resolver) throws Exception {
		JmsSource source = (JmsSource) resolver.getDataSource();
		String id = source.getId();

		JmsBrokerSource brokerSource = jmsBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found JMS broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new JmsBrokerSource(source);
			jmsBrokerMap.put(id, brokerSource);
		}
	}

	// collect all MQTT broker info
	private void buildMQTTBrokers(EventResolver resolver) throws Exception {
		MqttSource source = (MqttSource) resolver.getDataSource();
		String id = source.getId();

		MqttBrokerSource brokerSource = mqttBrokerMap.get(id);

		if (brokerSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info(
						"Found MQTT broker specified for host " + source.getHost() + " on port " + source.getPort());
			}
			brokerSource = new MqttBrokerSource(source);
			mqttBrokerMap.put(id, brokerSource);
		}
	}

	// collect all database server sources
	private void buildDatabaseServers(EventResolver resolver) {
		DatabaseEventSource eventSource = (DatabaseEventSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod();
		String id = eventSource.getId();

		PolledSource dbSource = databaseServerMap.get(id);

		if (dbSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found database server specified for host " + eventSource.getHost());
			}

			dbSource = new PolledSource(eventSource);
			databaseServerMap.put(id, dbSource);
		}
		dbSource.getSourceIds().add(sourceId);
		dbSource.getPollingIntervals().add(pollingMillis);
	}

	// collect all file server info
	private void buildFileServers(EventResolver resolver) throws Exception {
		FileEventSource eventSource = (FileEventSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod();
		String id = eventSource.getId();

		PolledSource serverSource = fileServerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found file server specified for host " + eventSource.getHost());
			}

			serverSource = new PolledSource(eventSource);
			fileServerMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	// collect all Modbus slave info
	private void buildModbusSlaves(EventResolver resolver) throws Exception {
		ModbusSource eventSource = (ModbusSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod();
		String id = eventSource.getId();

		PolledSource serverSource = modbusSlaveMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found Modbus slave specified for host " + eventSource.getHost());
			}

			serverSource = new PolledSource(eventSource);
			modbusSlaveMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	private void connectToRmqBrokers(Map<String, RmqBrokerSource> brokerSources) throws Exception {
		for (Entry<String, RmqBrokerSource> entry : brokerSources.entrySet()) {
			RmqSource source = entry.getValue().getSource();

			RmqClient pubsub = new RmqClient();

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
			appContext.getRmqClients().add(pubsub);

			if (logger.isInfoEnabled()) {
				logger.info("Started RMQ event pubsub: " + source.getId());
			}
		}
	}

	private void connectToJmsBrokers(Map<String, JmsBrokerSource> brokerSources) throws Exception {
		for (Entry<String, JmsBrokerSource> entry : brokerSources.entrySet()) {
			JmsSource source = entry.getValue().getSource();

			JmsClient jmsClient = new JmsClient();

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			jmsClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);

			// subscribe to event messages
			jmsClient.consumeEvents(true);

			// add to context
			appContext.getJmsClients().add(jmsClient);

			if (logger.isInfoEnabled()) {
				logger.info("Started JMS client: " + source.getId());
			}
		}
	}

	private void connectToMqttBrokers(Map<String, MqttBrokerSource> brokerSources) throws Exception {
		for (Entry<String, MqttBrokerSource> entry : brokerSources.entrySet()) {
			MqttSource source = entry.getValue().getSource();

			MqttOeeClient mqttClient = new MqttOeeClient();

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			mqttClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, this);
			mqttClient.subscribeToEvents(QualityOfService.EXACTLY_ONCE);

			// add to context
			appContext.getMqttClients().add(mqttClient);

			if (logger.isInfoEnabled()) {
				logger.info("Started MQTT client: " + source.getId());
			}
		}
	}

	private void connectToDatabaseServers(Map<String, PolledSource> dbSources) throws Exception {
		for (Entry<String, PolledSource> entry : dbSources.entrySet()) {
			// source of database interface table events
			PolledSource databaseSource = entry.getValue();
			DatabaseEventSource databaseEventSource = (DatabaseEventSource) databaseSource.getEventSource();
			List<String> sourceIds = databaseSource.getSourceIds();
			List<Integer> pollingIntervals = databaseSource.getPollingIntervals();

			DatabaseEventClient dbClient = new DatabaseEventClient(this, databaseEventSource, sourceIds,
					pollingIntervals);

			dbClient.connectToServer(databaseEventSource.getId(), databaseEventSource.getUserName(),
					databaseEventSource.getUserPassword());

			// add to context
			appContext.getDatabaseEventClients().add(dbClient);

			dbClient.startPolling();

			if (logger.isInfoEnabled()) {
				logger.info("Polling interface table for database server: " + databaseEventSource.getId());
			}
		}
	}

	private void startModbusPolling(Map<String, PolledSource> slaveSources) throws Exception {
		for (Entry<String, PolledSource> entry : slaveSources.entrySet()) {
			// source of Modbus slave events
			PolledSource slaveSource = entry.getValue();
			ModbusSource modbusSource = (ModbusSource) slaveSource.getEventSource();
			List<String> sourceIds = slaveSource.getSourceIds();
			List<Integer> pollingIntervals = slaveSource.getPollingIntervals();

			ModbusMaster modbusMaster = new ModbusMaster(this, modbusSource, sourceIds, pollingIntervals);
			modbusMaster.connect();

			// add to context
			appContext.getModbusMasters().add(modbusMaster);

			modbusMaster.startPolling();

			if (logger.isInfoEnabled()) {
				logger.info("Polling Modbus slave: " + modbusSource.getId());
			}
		}
	}

	private void startFilePolling(Map<String, PolledSource> fileSources) throws Exception {
		for (Entry<String, PolledSource> entry : fileSources.entrySet()) {
			// source of file events
			PolledSource fileSource = entry.getValue();
			FileEventSource fileEventSource = (FileEventSource) fileSource.getEventSource();
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

			// check if a specific collector is named
			if (collectorName != null) {
				if (!collector.getName().equals(collectorName)) {
					logger.info("Collector named " + collectorName + " is specified.  Skipping collector "
							+ collector.getName());
					continue;
				}
			}

			// add to our collectors
			if (!collectors.contains(collector)) {
				collectors.add(collector);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Found data collector '" + collector.getName() + "', for host " + collector.getHost()
						+ " in state " + collector.getCollectorState() + ", resolver: " + resolver);
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

				case RMQ: {
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

				case MODBUS: {
					buildModbusSlaves(resolver);
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
		connectToRmqBrokers(rmqBrokerMap);

		// collect equipment events for JMS
		connectToJmsBrokers(jmsBrokerMap);

		// poll database servers for events in the interface table
		connectToDatabaseServers(databaseServerMap);

		// collect equipment events for MQTT
		connectToMqttBrokers(mqttBrokerMap);

		// poll file servers
		startFilePolling(fileServerMap);

		// connect to Modbus slaves
		startModbusPolling(modbusSlaveMap);

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
			logger.info("Beginning startup for " + collectorName + ", " + DomainUtils.getVersionInfo());
		}

		InetAddress address = InetAddress.getLocalHost();
		hostname = address.getHostName();
		ip = address.getHostAddress();

		if (logger.isInfoEnabled()) {
			logger.info("Configuring server for host " + getId());
		}

		// our host
		if (!isManual) {
			// configure all of the data sources
			buildDataSources();
		} else {
			// manual data collection
			List<CollectorState> states = new ArrayList<>();
			states.add(CollectorState.READY);
			states.add(CollectorState.RUNNING);

			// look for manual data collector
			for (DataCollector collector : PersistenceService.instance().fetchCollectorsByState(states)) {
				if (collector.getHost() == null || collector.getHost().trim().length() == 0) {
					collectors.add(collector);
				}
			}
		}

		// connect to broker for notifications and commands
		startPublishingNotifications();

		// start collecting data
		startDataCollection();

		// notify monitors
		onInformation("Collector " + collectorName + " started on host " + getId());
	}

	private synchronized void startPublishingNotifications() throws Exception {
		// connect to notification brokers for commands
		Map<String, BaseMessagingClient> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			DataSourceType brokerType = collector.getBrokerType();

			String brokerHostName = collector.getBrokerHost();

			if (brokerType == null || brokerHostName == null || brokerHostName.length() == 0) {
				continue;
			}

			Integer brokerPort = collector.getBrokerPort();
			String key = brokerHostName + ":" + brokerPort;

			// new publisher
			if (brokerType.equals(DataSourceType.RMQ)) {
				// first look in RMQ data brokers
				boolean exists = false;
				for (Entry<String, RmqBrokerSource> entry : rmqBrokerMap.entrySet()) {
					RmqSource source = entry.getValue().getSource();
					if (source.getHost().equals(brokerHostName) && source.getPort().equals(brokerPort)) {
						exists = true;
						break;
					}
				}

				if (exists) {
					continue;
				}

				// new broker
				RmqClient rmqClient = new RmqClient();
				pubSubs.put(key, rmqClient);

				// queue
				String queueName = "CMD_" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

				// connect to broker and subscribe for commands
				List<RoutingKey> routingKeys = new ArrayList<>();
				routingKeys.add(RoutingKey.COMMAND_MESSAGE);

				rmqClient.startUp(brokerHostName, brokerPort, collector.getBrokerUserName(),
						collector.getBrokerUserPassword(), queueName, routingKeys, this);

				// add to context
				appContext.getRmqClients().add(rmqClient);
			} else if (brokerType.equals(DataSourceType.JMS)) {
				// first look in JMS data brokers
				boolean exists = false;
				for (Entry<String, JmsBrokerSource> entry : jmsBrokerMap.entrySet()) {
					JmsSource source = entry.getValue().getSource();
					if (source.getHost().equals(brokerHostName) && source.getPort().equals(brokerPort)) {
						exists = true;
						break;
					}
				}

				if (exists) {
					continue;
				}

				// new broker
				JmsClient jmsClient = new JmsClient();
				pubSubs.put(key, jmsClient);

				jmsClient.startUp(brokerHostName, brokerPort, collector.getBrokerUserName(),
						collector.getBrokerUserPassword(), this);

				// add to context
				appContext.getJmsClients().add(jmsClient);

			} else if (brokerType.equals(DataSourceType.MQTT)) {
				// first look in MQTT data brokers
				boolean exists = false;
				for (Entry<String, MqttBrokerSource> entry : mqttBrokerMap.entrySet()) {
					MqttSource source = entry.getValue().getSource();
					if (source.getHost().equals(brokerHostName) && source.getPort().equals(brokerPort)) {
						exists = true;
						break;
					}
				}

				if (exists) {
					continue;
				}

				// new broker
				MqttOeeClient mqttClient = new MqttOeeClient();
				pubSubs.put(key, mqttClient);

				mqttClient.startUp(brokerHostName, brokerPort, collector.getBrokerUserName(),
						collector.getBrokerUserPassword(), this);

				// add to context
				appContext.getMqttClients().add(mqttClient);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Connected to broker " + key + " of type " + brokerType + " for collector "
						+ collector.getName());
			}
		}

		// maybe start status publishing
		if (heartbeatTimer == null) {
			// create timer and task
			heartbeatTimer = new Timer();
			heartbeatTask = new HeartbeatTask();
			heartbeatTimer.schedule(heartbeatTask, HEARTBEAT_SEC * 1000, HEARTBEAT_SEC * 1000);

			if (logger.isInfoEnabled()) {
				logger.info("Scheduled heartbeat task for interval (sec): " + HEARTBEAT_SEC);
			}
		}
	}

	private synchronized void sendNotification(String text, NotificationSeverity severity) throws Exception {
		CollectorNotificationMessage message = new CollectorNotificationMessage(hostname, ip);
		message.setText(text);
		message.setSeverity(severity);
		message.setSenderId(collectorName);
		String timeStamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), DomainUtils.OFFSET_DATE_TIME_8601);
		message.setTimestamp(timeStamp);

		for (RmqClient pubsub : appContext.getRmqClients()) {
			pubsub.sendNotificationMessage(message);
		}

		for (JmsClient pubsub : appContext.getJmsClients()) {
			pubsub.sendNotificationMessage(message);
		}

		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			pubsub.sendNotificationMessage(message);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Sent message for host " + getId() + " of type " + message.getMessageType());
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
				logger.warn("Invalid state " + state + " from state " + currentState);
			}
		}
		collectors = savedCollectors;
	}

	public synchronized void stopNotifications() throws Exception {
		for (RmqClient pubsub : appContext.getRmqClients()) {
			pubsub.disconnect();
		}
		appContext.getRmqClients().clear();
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
		for (RmqClient pubsub : appContext.getRmqClients()) {
			onInformation("Disconnecting from pubsub with binding key " + pubsub.getBindingKey());
			pubsub.disconnect();
		}
		appContext.getRmqClients().clear();

		// stop polling Modbus slaves
		for (ModbusMaster master : appContext.getModbusMasters()) {
			master.stopPolling();
			onInformation("Stopped polling for Modbus master " + master.getDataSource().getHost());
		}
		appContext.getModbusMasters().clear();

		// set back to ready
		saveCollectorState(CollectorState.READY);
	}

	public void shutdown() {
		try {
			// notify monitors
			onInformation("Collector " + collectorName + " shutting down on host " + getId());

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
		startPublishingNotifications();

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

		// Modbus polling
		for (ModbusMaster master : appContext.getModbusMasters()) {
			master.stopPolling();
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

		// Modbus slave polling
		for (ModbusMaster master : appContext.getModbusMasters()) {
			master.startPolling();
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
	public void onHttpEquipmentEvent(String sourceId, String dataValue, String timestamp, String reason) {
		getExecutorService().execute(new HttpTask(sourceId, dataValue, timestamp, reason));
	}

	// File request
	@Override
	public void resolveFileEvents(FileEventClient client, String sourceId, List<File> files) {
		getExecutorService().execute(new FileTask(client, sourceId, files));
	}

	// Modbus event
	@Override
	public void resolveModbusEvents(ModbusEvent event) {
		getExecutorService().execute(new ModbusTask(event));
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

	public OeeEvent saveOeeEvent(OeeEvent event) throws Exception {
		Equipment equipment = event.getEquipment();
		Duration days = equipment.findRetentionPeriod();

		if (days != null && days.equals(Duration.ZERO)) {
			// no need to save or purge
			if (logger.isInfoEnabled()) {
				logger.info("Retention period is zero.  No record will be saved.");
			}
			return null;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Saving OEE event to database: " + event);
		}

		List<KeyedObject> records = new ArrayList<>();
		records.add(event);

		// close off previous events if not summarized
		OeeEventType type = event.getEventType();

		if (!type.isProduction() && event.getOffsetEndTime() == null) {
			// availability, material or job change
			OeeEvent lastRecord = PersistenceService.instance().fetchLastEvent(event.getEquipment(), type);

			if (lastRecord != null) {
				lastRecord.setOffsetEndTime(event.getOffsetStartTime());
				Duration duration = Duration.between(lastRecord.getStartTime(), lastRecord.getEndTime());
				lastRecord.setDuration(duration);

				records.add(lastRecord);
			}
		}

		// save records
		List<KeyedObject> savedRecords = PersistenceService.instance().save(records);

		// purge old data
		if (!type.isProduction()) {
			purgeRecords(event);
		}

		return (OeeEvent) savedRecords.get(0);
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
		try {
			sendNotification(text, NotificationSeverity.ERROR);
		} catch (Exception e) {
			// ignore
		}
	}

	public void onInformation(String text) throws Exception {
		// log it
		logger.info(text);

		// send to monitors
		sendNotification(text, NotificationSeverity.INFO);
	}

	public void onWarning(String text) throws Exception {
		// log it
		logger.warn(text);

		// send to monitors
		sendNotification(text, NotificationSeverity.WARNING);
	}

	@Override
	public void onRmqMessage(ApplicationMessage message) {
		// execute on worker thread
		RmqTask task = new RmqTask(message);
		executorService.execute(task);
	}

	@Override
	public void onJmsMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new JmsTask(message));
	}

	@Override
	public void onMqttMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new MqttTask(message));
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
	private class RmqBrokerSource {
		private final RmqSource source;

		RmqBrokerSource(RmqSource source) {
			this.source = source;
		}

		private RmqSource getSource() {
			return source;
		}
	}

	// JMS brokers
	private class JmsBrokerSource {
		private final JmsSource source;

		JmsBrokerSource(JmsSource source) {
			this.source = source;
		}

		private JmsSource getSource() {
			return source;
		}
	}

	// MQTT brokers
	private class MqttBrokerSource {
		private final MqttSource source;

		MqttBrokerSource(MqttSource source) {
			this.source = source;
		}

		private MqttSource getSource() {
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

	// polled event source
	private class PolledSource {
		private final CollectorDataSource eventSource;
		private final List<String> sourceIds = new ArrayList<>();
		private final List<Integer> pollingIntervals = new ArrayList<>();

		private PolledSource(CollectorDataSource eventSource) {
			this.eventSource = eventSource;
		}

		private CollectorDataSource getEventSource() {
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
		private final String timestamp;
		private final String reason;

		HttpTask(String sourceId, String dataValue, String timestamp, String reason) {
			this.sourceId = sourceId;
			this.dataValue = dataValue;
			this.timestamp = timestamp;
			this.reason = reason;
		}

		@Override
		public void run() {
			try {
				if (logger.isInfoEnabled()) {
					logger.info(
							"HTTP event, source: " + sourceId + ", value: " + dataValue + ", timestamp: " + timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp, reason);

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
				Object dataValue = UaOpcClient.getJavaObject(uaValue.getValue());
				String sourceId = item.getReadValueId().getNodeId().toParseableString();
				DateTime dt = uaValue.getServerTime();
				OffsetDateTime timestamp = DomainUtils.localTimeFromDateTime(dt);

				if (logger.isInfoEnabled()) {
					logger.info("OPC UA subscription, node: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp, null);

			} catch (Exception e) {
				onException("Unable to invoke OPC UA script resolver.", e);
			}
		}
	}

	public synchronized void recordResolution(OeeEvent resolvedEvent) throws Exception {
		if (resolvedEvent.getOffsetEndTime() != null && resolvedEvent.getDuration() != null) {
			Duration delta = Duration.between(resolvedEvent.getStartTime(), resolvedEvent.getEndTime());

			if (delta.compareTo(resolvedEvent.getDuration()) < 0) {
				throw new Exception(DomainLocalizer.instance().getErrorString("invalid.duration",
						resolvedEvent.getDuration(), delta));
			}
		}

		// save in database
		OeeEvent savedEvent = saveOeeEvent(resolvedEvent);

		// send event message
		sendResolutionMessage(savedEvent);
	}

	private void sendResolutionMessage(OeeEvent resolvedEvent) throws Exception {
		// send resolution message to each subscriber
		CollectorResolvedEventMessage message = new CollectorResolvedEventMessage(hostname, ip);
		message.fromResolvedEvent(resolvedEvent);
		message.setSenderId(collectorName);

		for (RmqClient pubsub : appContext.getRmqClients()) {
			pubsub.sendResolvedEventMessage(message);
		}

		for (JmsClient pubsub : appContext.getJmsClients()) {
			pubsub.sendNotificationMessage(message);
		}

		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			pubsub.sendNotificationMessage(message);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Sent message for host " + getId() + " of type " + message.getMessageType()
					+ " for resolver type " + message.getResolverType());
		}
	}

	private void resolveEvent(String sourceId, Object dataValue, String timestamp, String reason) throws Exception {
		OffsetDateTime odt = null;
		if (timestamp != null) {
			odt = DomainUtils.offsetDateTimeFromString(timestamp, DomainUtils.OFFSET_DATE_TIME_8601);
		}
		resolveEvent(sourceId, dataValue, odt, reason);
	}

	private void resolveEvent(String sourceId, Object dataValue, OffsetDateTime timestamp, String reason)
			throws Exception {
		EventResolver eventResolver = equipmentResolver.getResolver(sourceId);

		// check to see if we are collecting this data
		String eventCollector = eventResolver.getCollector().getName();
		if (collectorName != null && !eventCollector.equals(collectorName)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring event.  It is assigned to collector " + eventCollector);
			}
			return;
		}

		// event
		OeeEvent resolvedEvent = equipmentResolver.invokeResolver(eventResolver, getAppContext(), dataValue, timestamp);

		if (resolvedEvent == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Resolver script returned a null result.");
			}
			return;
		}

		// reason
		String reasonName = reason;

		if (reasonName == null) {
			// could have been set in the resolver script code
			reasonName = eventResolver.getReason();
		}

		if (reasonName != null && reasonName.trim().length() > 0) {
			Reason eventReason = PersistenceService.instance().fetchReasonByName(reasonName);

			if (eventReason == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("undefined.reason", reasonName));
			}
			resolvedEvent.setReason(eventReason);
		}

		if (!eventResolver.isWatchMode()) {
			recordResolution(resolvedEvent);
		}
	}

	public String getCollectorName() {
		return collectorName;
	}

	public void setCollectorName(String collectorName) {
		this.collectorName = collectorName;
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
				OpcDaVariant variantValue = item.getValue();
				Object dataValue = variantValue.getValueAsObject();

				String sourceId = item.getPathName();
				OffsetDateTime timestamp = item.getLocalTimestamp();

				if (logger.isInfoEnabled()) {
					logger.info("OPC DA data change, group: " + item.getGroup().getName() + ", item: " + sourceId
							+ ", value: " + item.getValueString() + ", timestamp: " + timestamp);
				}

				// resolve event
				resolveEvent(sourceId, dataValue, timestamp, null);

			} catch (Exception e) {
				onException("Unable to invoke OPC DA script resolver.", e);
			}
		}
	}

	private void handleMessage(ApplicationMessage message) throws Exception {
		MessageType type = message.getMessageType();

		if (type.equals(MessageType.EQUIPMENT_EVENT)) {
			EquipmentEventMessage eventMessage = (EquipmentEventMessage) message;

			String sourceId = eventMessage.getSourceId();
			String dataValue = eventMessage.getValue();
			String timestamp = eventMessage.getTimestamp();
			String reason = eventMessage.getReason();

			if (logger.isInfoEnabled()) {
				logger.info("Equipment event for collector " + collectorName + ", source: " + sourceId + ", value: "
						+ dataValue + ", timestamp: " + timestamp);
			}

			// resolve event
			resolveEvent(sourceId, dataValue, timestamp, reason);

		} else if (type.equals(MessageType.COMMAND)) {
			CollectorCommandMessage commandMessage = (CollectorCommandMessage) message;

			if (commandMessage.getCommand().equals(CollectorCommandMessage.CMD_RESTART)) {
				logger.info("Received restart command");
				restart();
			}
		}
	}

	/********************* RMQ Handler ***********************************/
	private class RmqTask implements Runnable {
		private final ApplicationMessage message;

		RmqTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
			} catch (Exception e) {
				// processing failed
				onException("Unable to process event " + message.getMessageType(), e);
			}
		}
	}

	/********************* JMS Handler ***********************************/
	private class JmsTask implements Runnable {

		private final ApplicationMessage message;

		JmsTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
			} catch (Exception e) {
				// processing failed
				onException("Unable to process JMS equipment event ", e);
			}
		}
	}

	/********************* MQTT Handler ***********************************/
	private class MqttTask implements Runnable {

		private final ApplicationMessage message;

		MqttTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
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
				if (hostname == null || ip == null) {
					// unidentified sender
					return;
				}

				// send status message to each PubSub
				CollectorServerStatusMessage message = new CollectorServerStatusMessage(hostname, ip);

				String timeStamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(),
						DomainUtils.OFFSET_DATE_TIME_8601);
				message.setTimestamp(timeStamp);

				// publish to RMQ brokers
				for (RmqClient pubsub : appContext.getRmqClients()) {
					pubsub.sendNotificationMessage(message);
				}

				// publish to JMS brokers
				for (JmsClient pubsub : appContext.getJmsClients()) {
					pubsub.sendNotificationMessage(message);
				}

				// publish to MQTT brokers
				for (MqttOeeClient pubsub : appContext.getMqttClients()) {
					pubsub.sendNotificationMessage(message);
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
				OffsetDateTime timestamp = databaseEvent.getEventTime();
				String reason = databaseEvent.getReason();

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
				resolveEvent(sourceId, dataValue, timestamp, reason);

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
		private final String sourceId;
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
					String fileContent = fileClient.readFile(file);

					// move to in-process
					fileClient.moveFile(file, FileEventClient.READY_FOLDER, FileEventClient.PROCESSING_FOLDER);

					// resolve event
					resolveEvent(sourceId, fileContent, timestamp, null);

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

	// handle the Modbus event callback
	private class ModbusTask implements Runnable {
		private final ModbusEvent event;

		ModbusTask(ModbusEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Modbus event, source: " + event.getSource());
				}

				// resolve event
				resolveEvent(event.getSourceId(), event.getValues(), event.getEventTime(), event.getReason());

			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
			}
		}
	}
}
