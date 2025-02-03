package org.point85.domain.collector;

import java.io.File;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.point85.domain.DomainUtils;
import org.point85.domain.OeeEquipmentEvent;
import org.point85.domain.cron.CronEventClient;
import org.point85.domain.cron.CronEventListener;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEvent;
import org.point85.domain.db.DatabaseEventClient;
import org.point85.domain.db.DatabaseEventListener;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.db.DatabaseEventStatus;
import org.point85.domain.dto.EquipmentEventRequestDto;
import org.point85.domain.email.EmailClient;
import org.point85.domain.email.EmailMessageListener;
import org.point85.domain.email.EmailSource;
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
import org.point85.domain.kafka.KafkaMessageListener;
import org.point85.domain.kafka.KafkaOeeClient;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.messaging.ApplicationMessage;
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
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.proficy.ProficyClient;
import org.point85.domain.proficy.ProficyEventListener;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.proficy.TagData;
import org.point85.domain.proficy.TagDataType;
import org.point85.domain.proficy.TagQuality;
import org.point85.domain.proficy.TagSample;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.rmq.RoutingKey;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.socket.WebSocketMessageListener;
import org.point85.domain.socket.WebSocketOeeServer;
import org.point85.domain.socket.WebSocketSource;
import org.point85.domain.uom.UnitOfMeasure;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * CollectorService listens for events from various sources and resolves them
 * into OEE performance, availability and quality records.
 *
 */
public class CollectorService
		implements HttpEventListener, OpcDaDataChangeListener, OpcUaAsynchListener, RmqMessageListener,
		JmsMessageListener, DatabaseEventListener, FileEventListener, MqttMessageListener, ModbusEventListener,
		CronEventListener, KafkaMessageListener, EmailMessageListener, ProficyEventListener, WebSocketMessageListener {

	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorService.class);

	// sec between status checks
	private static final long HEARTBEAT_SEC = 60;

	// thread pool service
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	// timer to broadcast status
	private Timer heartbeatTimer;

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
	private String collectorName;

	// data source information
	private final Map<String, OpcDaInfo> opcDaSubscriptionMap = new HashMap<>();
	private final Map<String, OpcUaInfo> opcUaSubscriptionMap = new HashMap<>();
	private final Map<String, HttpServerSource> httpServerMap = new HashMap<>();
	private final Map<String, RmqBrokerSource> rmqBrokerMap = new HashMap<>();
	private final Map<String, JmsBrokerSource> jmsBrokerMap = new HashMap<>();
	private final Map<String, PolledSource> kafkaBrokerMap = new HashMap<>();
	private final Map<String, PolledSource> emailBrokerMap = new HashMap<>();
	private final Map<String, PolledSource> databaseServerMap = new HashMap<>();
	private final Map<String, PolledSource> fileServerMap = new HashMap<>();
	private final Map<String, PolledSource> modbusSlaveMap = new HashMap<>();
	private final Map<String, MqttBrokerSource> mqttBrokerMap = new HashMap<>();
	private final Map<String, CronSource> cronSchedulerMap = new HashMap<>();
	private final Map<String, PolledSource> proficyMap = new HashMap<>();
	private final Map<String, WebSocketServerSource> webSocketServerMap = new HashMap<>();

	// equipment by name cache
	private final ConcurrentMap<String, Equipment> equipmentCache = new ConcurrentHashMap<>();

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
		kafkaBrokerMap.clear();
		emailBrokerMap.clear();
		databaseServerMap.clear();
		fileServerMap.clear();
		modbusSlaveMap.clear();
		cronSchedulerMap.clear();
		mqttBrokerMap.clear();
		proficyMap.clear();
		webSocketServerMap.clear();

		gson = new Gson();
		appContext = new OeeContext();
		equipmentResolver = new EquipmentEventResolver();
		collectors = new ArrayList<>();
	}

	public OeeEvent createEvent(String sourceId, OeeEventType type, Equipment equipment, OffsetDateTime startTime,
			OffsetDateTime endTime) throws Exception {
		OeeEvent event = new OeeEvent(equipment);
		event.setEventType(type);
		event.setStartTime(startTime);
		event.setEndTime(endTime);
		event.setSourceId(sourceId);

		// get the shift from the work schedule
		WorkSchedule schedule = equipment.findWorkSchedule();

		if (schedule != null) {
			List<ShiftInstance> shifts = schedule.getShiftInstancesForTime(startTime.toLocalDateTime());

			if (!shifts.isEmpty()) {
				event.setShift(shifts.get(0).getShift());
				event.setTeam(shifts.get(0).getTeam());
			}
		}

		// material being produced
		if (!type.equals(OeeEventType.MATL_CHANGE)) {
			OeeEvent setup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

			if (setup != null) {
				event.setMaterial(setup.getMaterial());
			}
		}

		return event;
	}

	public boolean isManual() {
		return this.isManual;
	}

	public OeeContext getAppContext() {
		return appContext;
	}

	// collect all HTTP server info
	private void buildHttpServers(EventResolver resolver) {
		HttpSource source = (HttpSource) resolver.getDataSource();
		String id = source.getId();

		HttpServerSource serverSource = httpServerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found HTTP server specified for host " + source.getHost() + " on HTTP port "
						+ source.getPort() + " and HTTPS port " + source.getHttpsPort());
			}
			serverSource = new HttpServerSource(source);
			httpServerMap.put(id, serverSource);
		}
	}

	// collect all web socket server info
	private void buildWebSocketServers(EventResolver resolver) {
		WebSocketSource source = (WebSocketSource) resolver.getDataSource();
		String id = source.getId();

		WebSocketServerSource serverSource = webSocketServerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found web socket server specified for host " + source.getHost() + " on port "
						+ source.getPort());
			}
			serverSource = new WebSocketServerSource(source);
			webSocketServerMap.put(id, serverSource);
		}
	}

	// collect all RMQ broker info
	private void buildRMQBrokers(EventResolver resolver) {
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
	private void buildJMSBrokers(EventResolver resolver) {
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
	private void buildMQTTBrokers(EventResolver resolver) {
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
	private void buildFileServers(EventResolver resolver) {
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

	// collect Kafka server info
	private void buildKafkaServers(EventResolver resolver) {
		KafkaSource eventSource = (KafkaSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod() != null ? resolver.getUpdatePeriod()
				: KafkaOeeClient.DEFAULT_POLLING_INTERVAL;

		String id = eventSource.getId();

		PolledSource serverSource = kafkaBrokerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found Kafka server specified for host " + eventSource.getHost() + " on port "
						+ eventSource.getPort());
			}

			serverSource = new PolledSource(eventSource);
			kafkaBrokerMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	// collect email events
	private void buildEmailHosts(EventResolver resolver) {
		EmailSource eventSource = (EmailSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod() != null ? resolver.getUpdatePeriod()
				: EmailClient.DEFAULT_POLLING_INTERVAL;

		String id = eventSource.getId();

		PolledSource serverSource = emailBrokerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found Email server specified for host " + eventSource.getHost() + " on port "
						+ eventSource.getPort());
			}

			serverSource = new PolledSource(eventSource);
			emailBrokerMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	// collect all cron scheduler info
	private void buildCronSchedulers(EventResolver resolver) {
		CronEventSource eventSource = (CronEventSource) resolver.getDataSource();
		String sourceId = resolver.getSourceId();
		String cronExpression = eventSource.getCronExpression();
		String id = eventSource.getId();

		CronSource serverSource = cronSchedulerMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found cron source specified for job " + eventSource.getHost());
			}

			serverSource = new CronSource(eventSource);
			cronSchedulerMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(sourceId);
		serverSource.getCronExpressions().add(cronExpression);
	}

	// collect all Modbus slave info
	private void buildModbusSlaves(EventResolver resolver) {
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

	// collect all Proficy slave info
	private void buildProficyHosts(EventResolver resolver) {
		ProficySource eventSource = (ProficySource) resolver.getDataSource();
		String tagName = resolver.getSourceId();
		Integer pollingMillis = resolver.getUpdatePeriod();
		String id = eventSource.getId();

		PolledSource serverSource = proficyMap.get(id);

		if (serverSource == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Found Proficy historian specified for host " + eventSource.getHost());
			}

			serverSource = new PolledSource(eventSource);
			proficyMap.put(id, serverSource);
		}
		serverSource.getSourceIds().add(tagName);
		serverSource.getPollingIntervals().add(pollingMillis);
	}

	private void connectToRmqBrokers(Map<String, RmqBrokerSource> brokerSources) throws Exception {
		for (Entry<String, RmqBrokerSource> entry : brokerSources.entrySet()) {
			RmqSource source = entry.getValue().getSource();

			RmqClient rmqClient = new RmqClient();
			rmqClient.setShouldNotify(false);

			String brokerHostName = source.getHost();
			Integer brokerPort = source.getPort();
			String brokerUser = source.getUserName();
			String brokerPassword = source.getUserPassword();

			// queue on each RMQ broker
			String queueName = "EVT_" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

			List<RoutingKey> routingKeys = new ArrayList<>();
			routingKeys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);

			// also can accept commands
			routingKeys.add(RoutingKey.COMMAND_MESSAGE);

			rmqClient.startUp(brokerHostName, brokerPort, brokerUser, brokerPassword, queueName, routingKeys, this);

			// add to context
			appContext.getRmqClients().add(rmqClient);

			if (logger.isInfoEnabled()) {
				logger.info("Started RMQ event pubsub: " + source.getId());
			}
		}
	}

	private void connectToJmsBrokers(Map<String, JmsBrokerSource> brokerSources) throws Exception {
		for (Entry<String, JmsBrokerSource> entry : brokerSources.entrySet()) {
			JmsSource source = entry.getValue().getSource();

			JmsClient jmsClient = new JmsClient();
			jmsClient.setShouldNotify(false);

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

	private void connectToKafkaBrokers(Map<String, PolledSource> brokerSources) throws Exception {
		for (Entry<String, PolledSource> entry : brokerSources.entrySet()) {
			KafkaSource kafkaSource = (KafkaSource) entry.getValue().getEventSource();

			// create the Kafka client
			KafkaOeeClient kafkaClient = new KafkaOeeClient();
			kafkaClient.setShouldNotify(false);

			// connect to server
			kafkaClient.createConsumer(kafkaSource, KafkaOeeClient.EVENT_TOPIC);

			// add to context
			appContext.getKafkaClients().add(kafkaClient);

			// subscribe to event messages
			kafkaClient.registerListener(this);

			// find minimum polling interval
			List<Integer> intervals = entry.getValue().getPollingIntervals();

			int min = KafkaOeeClient.DEFAULT_POLLING_INTERVAL;

			for (Integer interval : intervals) {
				if (interval < min) {
					min = interval;
				}
			}

			kafkaClient.setPollingInterval(min);

			kafkaClient.startPolling();

			if (logger.isInfoEnabled()) {
				logger.info("Started Kafka client: " + kafkaSource.getId());
			}
		}
	}

	private void connectToEmailServers(Map<String, PolledSource> emailServers) {
		for (Entry<String, PolledSource> entry : emailServers.entrySet()) {
			EmailSource emailSource = (EmailSource) entry.getValue().getEventSource();

			// create the Email client
			EmailClient emailClient = new EmailClient(emailSource);
			emailClient.setShouldNotify(false);

			// add to context
			appContext.getEmailClients().add(emailClient);

			// receive event messages
			emailClient.registerListener(this);

			// find minimum polling interval
			List<Integer> intervals = entry.getValue().getPollingIntervals();

			int min = EmailClient.DEFAULT_POLLING_INTERVAL;

			for (Integer interval : intervals) {
				if (interval < min) {
					min = interval;
				}
			}

			emailClient.setPollingInterval(min);

			emailClient.startPolling();

			if (logger.isInfoEnabled()) {
				logger.info("Started Email client: " + emailSource.getId());
			}
		}
	}

	private void connectToMqttBrokers(Map<String, MqttBrokerSource> brokerSources) throws Exception {
		for (Entry<String, MqttBrokerSource> entry : brokerSources.entrySet()) {
			MqttSource source = entry.getValue().getSource();

			MqttOeeClient mqttClient = new MqttOeeClient();
			mqttClient.setShouldNotify(false);

			mqttClient.setAuthenticationConfiguration(source.getUserName(), source.getUserPassword());
			mqttClient.setSSLConfiguration(source.getKeystore(), source.getKeystorePassword(), source.getKeyPassword());

			mqttClient.startUp(source.getHost(), source.getPort(), this);
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

	private void startProficyPolling(Map<String, PolledSource> proficySources) {
		for (Entry<String, PolledSource> entry : proficySources.entrySet()) {
			// source of Proficy events
			PolledSource polledSource = entry.getValue();
			ProficySource proficySource = (ProficySource) polledSource.getEventSource();
			List<String> sourceIds = polledSource.getSourceIds();
			List<Integer> pollingIntervals = polledSource.getPollingIntervals();

			ProficyClient proficyClient = new ProficyClient(this, proficySource, sourceIds, pollingIntervals);

			// add to context
			appContext.getProficyClients().add(proficyClient);

			proficyClient.startPolling();

			if (logger.isInfoEnabled()) {
				logger.info("Polling Proficy historian: " + proficySource.getId());
			}
		}
	}

	private void startFilePolling(Map<String, PolledSource> fileSources) {
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

	private void startCronSchedulers(Map<String, CronSource> cronSources) throws Exception {
		for (Entry<String, CronSource> entry : cronSources.entrySet()) {
			// source of file events
			CronSource cronSource = entry.getValue();
			CronEventSource cronEventSource = cronSource.getEventSource();
			List<String> sourceIds = cronSource.getSourceIds();
			List<String> expressions = cronSource.getCronExpressions();

			CronEventClient cronClient = new CronEventClient(this, cronEventSource, sourceIds, expressions);

			// add to context
			appContext.getCronEventClients().add(cronClient);

			cronClient.scheduleJobs();

			if (logger.isInfoEnabled()) {
				logger.info("Jobs scheduled on server: " + cronEventSource.getHost() + " for job "
						+ cronEventSource.getName());
			}
		}
	}

	private void startHttpServers(Map<String, HttpServerSource> httpServerSources) throws Exception {
		for (Entry<String, HttpServerSource> entry : httpServerSources.entrySet()) {
			HttpSource source = entry.getValue().getSource();

			Integer port = source.getPort();
			Integer httpsPort = source.getHttpsPort();

			if (logger.isInfoEnabled()) {
				logger.info("Starting embedded HTTP server on HTTP port " + port + " and HTTPS port " + httpsPort);
			}

			OeeHttpServer httpServer = new OeeHttpServer(port);

			if (httpsPort != null) {
				httpServer.setHttpsPort(httpsPort);
			}

			OeeHttpServer.setDataChangeListener(this);
			httpServer.setAcceptingEventRequests(true);
			httpServer.startup();

			// add to context
			appContext.getHttpServers().add(httpServer);

			if (logger.isInfoEnabled()) {
				logger.info("Started embedded HTTP server on HTTP port " + port + " and HTTPS port " + httpsPort);
			}
		}
	}

	private void startWebSocketServers(Map<String, WebSocketServerSource> webSocketServerSources) throws Exception {
		for (Entry<String, WebSocketServerSource> entry : webSocketServerSources.entrySet()) {
			WebSocketSource source = entry.getValue().getSource();

			WebSocketOeeServer wsServer = new WebSocketOeeServer(source);
			wsServer.registerListener(this);
			wsServer.startup();

			// add to context
			appContext.getWebSocketServers().add(wsServer);

			if (logger.isInfoEnabled()) {
				logger.info("Started embedded web socket server on HTTP port " + source.getPort());
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

	private void buildOpcDaSubscriptions(EventResolver resolver) {
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
		hostNames.add("localhost");

		// fetch script resolvers from database
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.DEV);
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);
		List<EventResolver> resolvers = PersistenceService.instance().fetchEventResolversByHost(hostNames, states);

		if (resolvers.isEmpty() && logger.isWarnEnabled()) {
			logger.warn("No resolvers found for hosts " + hostNames);
		}

		for (EventResolver resolver : resolvers) {
			// build list of runnable collectors
			DataCollector collector = resolver.getCollector();

			// check if a specific collector is named
			if (collectorName != null && !collector.getName().equals(collectorName)) {
				if (logger.isInfoEnabled()) {
					logger.info("Collector named " + collectorName + " is specified.  Skipping collector "
							+ collector.getName());
				}
				continue;
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
					buildRMQBrokers(resolver);
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

				case CRON: {
					buildCronSchedulers(resolver);
					break;
				}

				case MODBUS: {
					buildModbusSlaves(resolver);
					break;
				}

				case KAFKA: {
					buildKafkaServers(resolver);
					break;
				}

				case EMAIL: {
					buildEmailHosts(resolver);
					break;
				}

				case PROFICY: {
					buildProficyHosts(resolver);
					break;
				}

				case WEB_SOCKET: {
					buildWebSocketServers(resolver);
					break;
				}

				default:
					break;
				}
			}
		} // end resolvers

		checkForStandaloneServers();
	}

	private void checkForStandaloneServers() throws Exception {
		// check for HTTP sources for external apps
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.HTTP);

		for (CollectorDataSource source : sources) {
			HttpSource httpSource = (HttpSource) source;
			if (httpSource.isStandalone()) {
				if (!httpServerMap.containsKey(httpSource.getId())) {
					if (logger.isInfoEnabled()) {
						logger.info("Found standalone HTTP server specified for host " + httpSource.getHost()
								+ " on HTTP port " + httpSource.getPort() + " and HTTPS port "
								+ httpSource.getHttpsPort());
					}

					// server is not being used for a resolver, so add it
					HttpServerSource serverSource = new HttpServerSource(httpSource);
					httpServerMap.put(httpSource.getId(), serverSource);
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

		// collect equipment events for Kafka
		connectToKafkaBrokers(kafkaBrokerMap);

		// collect equipment events from email servers
		connectToEmailServers(emailBrokerMap);

		// poll database servers for events in the interface table
		connectToDatabaseServers(databaseServerMap);

		// collect equipment events for MQTT
		connectToMqttBrokers(mqttBrokerMap);

		// poll file servers
		startFilePolling(fileServerMap);

		// start cron schedulers
		startCronSchedulers(cronSchedulerMap);

		// connect to Modbus slaves
		startModbusPolling(modbusSlaveMap);

		// connect to Proficy historians
		startProficyPolling(proficyMap);

		// collect data for web socket
		startWebSocketServers(webSocketServerMap);

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
			String name = collectorName != null ? collectorName : "";
			logger.info("Beginning startup for collector " + name + ", " + DomainUtils.getVersionInfo());
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

		// start collecting data
		startDataCollection();

		// connect to broker for notifications and commands
		startPublishingNotifications();

		// notify monitors
		onInformation("Collector " + collectorName + " started on host " + getId());
	}

	private void connectToRMQ(RmqSource server) throws Exception {
		// first look in RMQ data brokers
		boolean rmqClientExists = false;

		for (RmqClient anRmqClient : appContext.getRmqClients()) {
			if (anRmqClient.getHostName().equals(server.getHost()) && anRmqClient.getHostPort() == server.getPort()) {
				anRmqClient.setShouldNotify(true);
				rmqClientExists = true;
				break;
			}
		}

		if (!rmqClientExists) {
			// not in the app context already as a data source too
			RmqClient rmqClient = new RmqClient();
			rmqClient.setShouldNotify(true);

			// queue
			String queueName = "CMD_" + getClass().getSimpleName() + "_" + System.currentTimeMillis();

			// connect to broker and subscribe for commands
			List<RoutingKey> routingKeys = new ArrayList<>();
			routingKeys.add(RoutingKey.COMMAND_MESSAGE);

			rmqClient.startUp(server.getHost(), server.getPort(), server.getUserName(), server.getUserPassword(),
					queueName, routingKeys, this);

			// add to context
			appContext.getRmqClients().add(rmqClient);
		}
	}

	private void connectToJMS(JmsSource server) throws Exception {
		// first look in JMS data brokers
		boolean jmsClientExists = false;

		for (JmsClient aJmsClient : appContext.getJmsClients()) {
			if (aJmsClient.getHostName().equals(server.getHost()) && aJmsClient.getHostPort() == server.getPort()) {
				aJmsClient.setShouldNotify(true);
				jmsClientExists = true;
				break;
			}
		}

		if (!jmsClientExists) {
			// not in the app context already as a data source
			JmsClient jmsClient = new JmsClient();
			jmsClient.setShouldNotify(true);

			jmsClient.startUp(server.getHost(), server.getPort(), server.getUserName(), server.getUserPassword(), this);

			// add to context
			appContext.getJmsClients().add(jmsClient);
		}
	}

	private void connectToMQTT(MqttSource server) throws Exception {
		// first look in MQTT data brokers
		boolean mqttClientExists = false;

		for (MqttOeeClient anMqttClient : appContext.getMqttClients()) {
			if (anMqttClient.getHostName().equals(server.getHost()) && anMqttClient.getHostPort() == server.getPort()) {
				anMqttClient.setShouldNotify(true);
				mqttClientExists = true;
				break;
			}
		}

		if (!mqttClientExists) {
			// not in the app context already as a data source
			MqttOeeClient mqttClient = new MqttOeeClient();
			mqttClient.setShouldNotify(true);

			mqttClient.setAuthenticationConfiguration(server.getUserName(), server.getUserPassword());
			mqttClient.setSSLConfiguration(server.getKeystore(), server.getKeystorePassword(), server.getKeyPassword());

			mqttClient.startUp(server.getHost(), server.getPort(), this);

			// add to context
			appContext.getMqttClients().add(mqttClient);
		}
	}

	private void connectToKafka(KafkaSource server) throws Exception {
		// first look in Kafka servers for a producer
		boolean kafkaClientExists = false;

		for (KafkaOeeClient aKafkaClient : appContext.getKafkaClients()) {
			KafkaSource producerSource = aKafkaClient.getProducerServer();
			if (producerSource != null && producerSource.getHost().equals(server.getHost())
					&& producerSource.getPort().equals(server.getPort())) {
				aKafkaClient.setShouldNotify(true);
				kafkaClientExists = true;
				break;
			}
		}

		if (!kafkaClientExists) {
			// not in the app context already as a data source
			KafkaOeeClient producerClient = new KafkaOeeClient();
			producerClient.setShouldNotify(true);

			// connect as a producer
			producerClient.createProducer(server, KafkaOeeClient.NOTIFICATION_TOPIC);

			// add to context
			appContext.getKafkaClients().add(producerClient);
		}
	}

	private synchronized void startPublishingNotifications() throws Exception {
		// connect to notification brokers
		for (DataCollector collector : collectors) {
			CollectorDataSource server = collector.getNotificationServer();

			if (server == null) {
				continue;
			}

			DataSourceType brokerType = server.getDataSourceType();

			// new publisher
			if (brokerType.equals(DataSourceType.RMQ)) {
				connectToRMQ((RmqSource) server);

			} else if (brokerType.equals(DataSourceType.JMS)) {
				connectToJMS((JmsSource) server);

			} else if (brokerType.equals(DataSourceType.KAFKA)) {
				connectToKafka((KafkaSource) server);

			} else if (brokerType.equals(DataSourceType.MQTT)) {
				connectToMQTT((MqttSource) server);

			} else {
				// ignore others
				continue;
			}

			if (logger.isInfoEnabled()) {
				logger.info("Publishing notifications to server " + server + " of type " + brokerType
						+ " for collector " + collector.getName());
			}
		}

		// start status publishing
		if (heartbeatTimer == null) {
			// create timer and task
			heartbeatTimer = new Timer();
			HeartbeatTask heartbeatTask = new HeartbeatTask();
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

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType() + " for RMQ "
						+ pubsub);
			}
		}

		for (JmsClient pubsub : appContext.getJmsClients()) {
			pubsub.sendNotificationMessage(message);

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType() + " for JMS "
						+ pubsub);
			}
		}

		for (KafkaOeeClient pubsub : appContext.getKafkaClients()) {
			pubsub.sendNotificationMessage(message);

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType() + " for Kafka "
						+ pubsub);
			}
		}

		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			pubsub.sendNotificationMessage(message);

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType() + " for MQTT "
						+ pubsub);
			}
		}

		for (EmailClient emailClient : appContext.getEmailClients()) {
			emailClient.sendEvent(emailClient.getSource().getUserName(),
					DomainLocalizer.instance().getLangString("email.notification.subject"), message);

			if (logger.isInfoEnabled()) {
				logger.info("Sent message for host " + getId() + " of type " + message.getMessageType()
						+ " for email server " + emailClient.getSource().getId());
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
				if (logger.isWarnEnabled()) {
					logger.warn("Invalid state " + state + " from state " + currentState);
				}
			}
		}
		collectors = savedCollectors;
	}

	public synchronized void stopNotifications() throws Exception {
		for (RmqClient pubsub : appContext.getRmqClients()) {
			pubsub.disconnect();
		}
		appContext.getRmqClients().clear();

		for (JmsClient pubsub : appContext.getJmsClients()) {
			pubsub.disconnect();
		}
		appContext.getJmsClients().clear();

		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			pubsub.disconnect();
		}
		appContext.getMqttClients().clear();

		for (KafkaOeeClient pubsub : appContext.getKafkaClients()) {
			pubsub.disconnect();
		}
		appContext.getKafkaClients().clear();
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
			onInformation("Shutdown HTTP server on host " + httpServer.getHost());
		}
		appContext.getHttpServers().clear();

		// shutdown web socket servers
		for (WebSocketOeeServer wsServer : appContext.getWebSocketServers()) {
			wsServer.shutdown();
			onInformation("Shutdown web socket server on port " + wsServer.getPort());
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
			onInformation("Disconnecting from RMQ server " + pubsub);
			pubsub.disconnect();
		}
		appContext.getRmqClients().clear();

		// disconnect from JMS servers
		for (JmsClient pubsub : appContext.getJmsClients()) {
			onInformation("Disconnecting from JMS broker " + pubsub);
			pubsub.disconnect();
		}
		appContext.getJmsClients().clear();

		// disconnect from Kafka servers
		for (KafkaOeeClient pubsub : appContext.getKafkaClients()) {
			onInformation("Disconnecting from Kafka server " + pubsub);
			pubsub.disconnect();
		}
		appContext.getKafkaClients().clear();

		// disconnect from MQTT servers
		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			onInformation("Disconnecting from MQTT server " + pubsub);
			pubsub.disconnect();
		}
		appContext.getMqttClients().clear();

		// stop polling Modbus slaves
		for (ModbusMaster master : appContext.getModbusMasters()) {
			master.stopPolling();
			onInformation("Stopped polling for Modbus master " + master.getDataSource().getHost());
		}
		appContext.getModbusMasters().clear();

		// stop cron jobs
		for (CronEventClient client : appContext.getCronEventClients()) {
			client.shutdownScheduler();
			onInformation("Shutdown cron job scheduler for job " + client.getCronEventSource().getName());
		}
		appContext.getCronEventClients().clear();

		// stop polling for email
		for (EmailClient client : appContext.getEmailClients()) {
			client.stopPolling();
			onInformation("Shutdown email client for server " + client.getSource().getId());
		}
		appContext.getEmailClients().clear();

		// stop polling Proficy historians
		for (ProficyClient proficyClient : appContext.getProficyClients()) {
			proficyClient.stopPolling();
			onInformation("Stopped polling for Proficy historian " + proficyClient.getDataSource().getHost());
		}
		appContext.getProficyClients().clear();

		// set back to ready
		saveCollectorState(CollectorState.READY);
	}

	public void shutdown() {
		try {
			// notify monitors
			onInformation("Collector " + collectorName + " shutting down on host " + getId());

			// stop data collection
			stopDataCollection();

			// stop messaging server notifications
			stopNotifications();

			// stop heartbeat
			if (heartbeatTimer != null) {
				heartbeatTimer.cancel();
				heartbeatTimer = null;
			}
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

			// Restore interrupted state...
			Thread.currentThread().interrupt();
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

		// cron clients
		for (CronEventClient client : appContext.getCronEventClients()) {
			client.unscheduleJobs();
		}

		// HTTP servers
		for (OeeHttpServer httpServer : appContext.getHttpServers()) {
			httpServer.shutdown();
		}

		// web socket servers
		for (WebSocketOeeServer wsServer : appContext.getWebSocketServers()) {
			if (wsServer.isStarted()) {
				wsServer.shutdown();
			}
		}

		// Kafka consumer polling
		for (KafkaOeeClient consumer : appContext.getKafkaClients()) {
			consumer.stopPolling();
		}

		// email polling
		for (EmailClient emailClient : appContext.getEmailClients()) {
			emailClient.stopPolling();
		}

		// Proficy polling
		for (ProficyClient proficyClient : appContext.getProficyClients()) {
			proficyClient.stopPolling();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Unsubscribed from data sources.");
		}
	}

	public void subscribeToDataSource() throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Subscribing to data sources");
		}

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

		// web socket servers
		for (WebSocketOeeServer wsServer : appContext.getWebSocketServers()) {
			if (!wsServer.isStarted()) {
				wsServer.startup();
			}
		}

		// Modbus slave polling
		for (ModbusMaster master : appContext.getModbusMasters()) {
			master.startPolling();
		}

		// cron clients
		for (CronEventClient client : appContext.getCronEventClients()) {
			client.scheduleJobs();
		}

		// Kafka consumer polling
		for (KafkaOeeClient consumer : appContext.getKafkaClients()) {
			consumer.startPolling();
		}

		// email polling
		for (EmailClient emailClient : appContext.getEmailClients()) {
			emailClient.startPolling();
		}

		// Proficy polling
		for (ProficyClient proficyClient : appContext.getProficyClients()) {
			proficyClient.startPolling();
		}
	}

	protected ExecutorService getExecutorService() {
		return this.executorService;
	}

	// HTTP request
	@Override
	public void onHttpEquipmentEvent(EquipmentEventRequestDto dto) throws Exception {
		if (!dto.getImmediate()) {
			// execute in task pool
			getExecutorService().execute(new HttpTask(dto));
		} else {
			// execute it now
			processHttpEquipmentEvent(dto);
		}
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

		// purge database tables
		PersistenceService.instance().purge(equipment, days);
	}

	private OeeEvent saveOeeEvent(OeeEvent event) throws Exception {
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
		purgeRecords(event);

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
	public void onKafkaMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new KafkaTask(message));
	}

	@Override
	public void onMqttMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new MqttTask(message));
	}

	@Override
	public void onWebSocketMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new WebSocketTask(message));
	}

	@Override
	public void resolveDatabaseEvents(DatabaseEventClient databaseClient, List<DatabaseEvent> events) {
		for (DatabaseEvent event : events) {
			// execute on worker thread
			DatabaseEventTask task = new DatabaseEventTask(databaseClient, event);
			executorService.execute(task);
		}
	}

	@Override
	public void resolveCronEvent(JobExecutionContext context) {
		getExecutorService().execute(new CronTask(context));
	}

	@Override
	public void onEmailMessage(ApplicationMessage message) {
		// execute on worker thread
		executorService.execute(new EmailTask(message));
	}

	@Override
	public void onProficyEvent(TagData tagData) {
		// execute on worker thread
		executorService.execute(new ProficyTask(tagData));
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

	// web socket servers
	private class WebSocketServerSource {
		private final WebSocketSource source;

		WebSocketServerSource(WebSocketSource source) {
			this.source = source;
		}

		private WebSocketSource getSource() {
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

	// cron event source
	private class CronSource {
		private final CronEventSource eventSource;
		private final List<String> sourceIds = new ArrayList<>();
		private final List<String> cronExpressions = new ArrayList<>();

		private CronSource(CronEventSource eventSource) {
			this.eventSource = eventSource;
		}

		private CronEventSource getEventSource() {
			return eventSource;
		}

		private List<String> getSourceIds() {
			return sourceIds;
		}

		private List<String> getCronExpressions() {
			return cronExpressions;
		}
	}

	public void processHttpEquipmentEvent(EquipmentEventRequestDto dto) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("HTTP event: " + dto);
		}

		// create event
		OffsetDateTime start = null;

		try {
			// first try zone offset
			start = DomainUtils.offsetDateTimeFromString(dto.getTimestamp(), DomainUtils.OFFSET_DATE_TIME_8601);
		} catch (Exception e) {
			// now try a local time
			LocalDateTime ldt = DomainUtils.localDateTimeFromString(dto.getTimestamp(),
					DomainUtils.LOCAL_DATE_TIME_8601);
			start = DomainUtils.fromLocalDateTime(ldt);
		}

		OeeEquipmentEvent event = new OeeEquipmentEvent(dto.getSourceId(), dto.getValue(), start);

		// end
		OffsetDateTime end = null;

		if (dto.getEndTimestamp() != null) {
			try {
				// first try zone offset
				end = DomainUtils.offsetDateTimeFromString(dto.getEndTimestamp(), DomainUtils.OFFSET_DATE_TIME_8601);
			} catch (Exception e) {
				// now try a local time
				LocalDateTime ldt = DomainUtils.localDateTimeFromString(dto.getEndTimestamp(),
						DomainUtils.LOCAL_DATE_TIME_8601);
				end = DomainUtils.fromLocalDateTime(ldt);
			}
			event.setEndTimestamp(end);
		}

		if (end != null && start.isAfter(end)) {
			throw new Exception(DomainLocalizer.instance().getErrorString("print.end.earlier.than.start", start, end));
		}

		// duration
		Duration period = (dto.getDuration() != null) ? Duration.ofSeconds(Long.parseLong(dto.getDuration())) : null;
		event.setDuration(period);

		// equipment
		Equipment equipment = (dto.getEquipmentName() != null) ? fetchEquipment(dto.getEquipmentName()) : null;
		event.setEquipment(equipment);

		// production reason
		Reason reason = (dto.getReason() != null) ? fetchReason(dto.getReason()) : null;
		event.setReason(reason);

		// event type
		OeeEventType eventType = (dto.getEventType() != null) ? OeeEventType.valueOf(dto.getEventType()) : null;
		event.setEventType(eventType);

		// job
		event.setJob(dto.getJob());

		// resolve event
		resolveEvent(event);
	}

	// handle the HTTP callback
	private class HttpTask implements Runnable {
		private final EquipmentEventRequestDto dto;

		HttpTask(EquipmentEventRequestDto dto) {
			this.dto = dto;
		}

		@Override
		public void run() {
			try {
				processHttpEquipmentEvent(dto);
			} catch (Exception e) {
				onException(DomainLocalizer.instance().getErrorString("unable.to.resolve"), e);
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
				Object dataValue = appContext.getOpcUaClient().getJavaObject(uaValue.getValue());

				String sourceId = item.getReadValueId().getNodeId().toParseableString();
				DateTime dt = uaValue.getServerTime();

				if (dt == null) {
					dt = DateTime.now();
				}

				OffsetDateTime startTimestamp = DomainUtils.localTimeFromDateTime(dt);

				if (logger.isInfoEnabled()) {
					logger.info("OPC UA subscription, node: " + sourceId + ", value: " + dataValue + ", timestamp: "
							+ startTimestamp);
				}

				// resolve event
				OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, dataValue, startTimestamp);
				resolveEvent(event);

			} catch (Exception e) {
				onException("Unable to invoke OPC UA script resolver.", e);
			}
		}
	}

	public synchronized void recordResolution(OeeEvent resolvedEvent) throws Exception {
		// only for equipment
		if (resolvedEvent.getEquipment() == null) {
			return;
		}

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

		for (KafkaOeeClient pubsub : appContext.getKafkaClients()) {
			pubsub.sendNotificationMessage(message);
		}

		for (MqttOeeClient pubsub : appContext.getMqttClients()) {
			pubsub.sendNotificationMessage(message);
		}

		for (EmailClient emailClient : appContext.getEmailClients()) {
			emailClient.sendEvent(emailClient.getSource().getUserName(),
					DomainLocalizer.instance().getLangString("email.notification.subject"), message);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Sent message for host " + getId() + " of type " + message.getMessageType()
					+ " for resolver type " + message.getResolverType());
		}
	}

	private Equipment fetchEquipment(String equipmentName) throws Exception {
		Equipment equipment = null;

		if (equipmentName == null) {
			return equipment;
		}

		equipment = equipmentCache.get(equipmentName);

		if (equipment == null) {
			// fetch from database
			equipment = PersistenceService.instance().fetchEquipmentByName(equipmentName);

			// cache it
			if (equipment != null) {
				equipmentCache.put(equipmentName, equipment);
			} else {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.equipment", equipmentName));
			}
		}
		return equipment;
	}

	private void checkDuration(OeeEquipmentEvent event) throws Exception {
		if (event.getEndTimestamp() != null) {
			if (event.getDuration() == null || event.getDuration().isZero()) {
				throw new Exception(DomainLocalizer.instance().getErrorString("zero.duration"));
			}

			Duration delta = Duration.between(event.getStartTimestamp(), event.getEndTimestamp());
			if (event.getDuration().compareTo(delta) > 0) {
				throw new Exception(DomainLocalizer.instance().getErrorString("duration.too.long",
						DomainUtils.formatDuration(event.getDuration())));
			}
		}
	}

	private void resolveEvent(OeeEquipmentEvent event) throws Exception {
		OeeEvent resolvedEvent = null;
		boolean isWatchMode = false;

		if (event.getSourceId() != null) {
			// pre-defined event
			EventResolver eventResolver = equipmentResolver.getResolver(event.getSourceId());

			if (eventResolver == null) {
				logger.error("No event resolver found for source id: " + event.getSourceId());
				return;
			}
			isWatchMode = eventResolver.isWatchMode();

			// check to see if we are collecting this data
			String eventCollector = eventResolver.getCollector().getName();
			if (collectorName != null && !eventCollector.equals(collectorName)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Ignoring event.  It is assigned to collector " + eventCollector);
				}
				return;
			}

			// event
			resolvedEvent = equipmentResolver.invokeResolver(eventResolver, getAppContext(), event.getDataValue(),
					event.getStartTimestamp());

			if (resolvedEvent == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Resolver script returned a null result.");
				}
				return;
			}

			// reason could have been set in the resolver script code
			Reason eventReason = resolvedEvent.getReason();
			if (eventResolver.getReason() != null) {
				eventReason = fetchReason(eventResolver.getReason());
			}

			// reason
			resolvedEvent.setReason(eventReason);

		} else {
			// anonymous event via HTTP API
			resolvedEvent = createEvent("API", event.getEventType(), event.getEquipment(), event.getStartTimestamp(),
					event.getEndTimestamp());

			OeeEventType eventType = event.getEventType();

			if (eventType.equals(OeeEventType.AVAILABILITY)) {
				// reason
				Reason availabilityReason = fetchReason((String) event.getDataValue());

				resolvedEvent.setReason(availabilityReason);

				// availability duration
				checkDuration(event);
				resolvedEvent.setDuration(event.getDuration());

			} else if (eventType.equals(OeeEventType.PROD_GOOD) || eventType.equals(OeeEventType.PROD_REJECT)
					|| eventType.equals(OeeEventType.PROD_STARTUP)) {
				// produced amount
				if (event.getDataValue() == null) {
					throw new Exception(DomainLocalizer.instance().getErrorString("missing.amount"));
				}
				Double amount = Double.valueOf((String) event.getDataValue());

				if (amount.equals(0.0)) {
					throw new Exception(DomainLocalizer.instance().getErrorString("missing.amount"));
				}

				// production unit of measure
				Material producedMaterial = resolvedEvent.getMaterial();
				UnitOfMeasure uom = resolvedEvent.getEquipment().getUOM(producedMaterial, eventType);

				resolvedEvent.setAmount(amount);
				resolvedEvent.setUOM(uom);

				// reason
				Reason productionReason = event.getReason();
				resolvedEvent.setReason(productionReason);

			} else if (eventType.equals(OeeEventType.MATL_CHANGE) || eventType.equals(OeeEventType.JOB_CHANGE)) {
				// material
				String materialId = (String) event.getDataValue();

				if (materialId != null) {
					Material material = fetchMaterial(materialId);
					resolvedEvent.setMaterial(material);
				}

				// job
				resolvedEvent.setJob(event.getJob());
			}
		}

		if (!isWatchMode) {
			recordResolution(resolvedEvent);
		}
	}

	private Reason fetchReason(String reasonName) throws Exception {
		Reason eventReason = null;
		if (reasonName != null && reasonName.trim().length() > 0) {
			eventReason = PersistenceService.instance().fetchReasonByName(reasonName);

			if (eventReason == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("undefined.reason", reasonName));
			}
		}
		return eventReason;
	}

	private Material fetchMaterial(String materialName) throws Exception {
		Material material = null;
		if (materialName != null && materialName.trim().length() > 0) {
			material = PersistenceService.instance().fetchMaterialByName(materialName);

			if (material == null) {
				throw new Exception(DomainLocalizer.instance().getErrorString("no.material", materialName));
			}
		}
		return material;
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
				OffsetDateTime startTimestamp = item.getLocalTimestamp();

				if (logger.isInfoEnabled()) {
					logger.info("OPC DA data change, group: " + item.getGroup().getName() + ", item: " + sourceId
							+ ", value: " + item.getValueString() + ", timestamp: " + startTimestamp);
				}

				// resolve event
				OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, dataValue, startTimestamp);

				resolveEvent(event);

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
			String startTimestamp = eventMessage.getTimestamp();
			String reason = eventMessage.getReason();

			if (logger.isInfoEnabled()) {
				logger.info("Equipment event for collector " + collectorName + ", source: " + sourceId + ", value: "
						+ dataValue + ", timestamp: " + startTimestamp + ", reason: " + reason);
			}

			// resolve event
			OffsetDateTime start = DomainUtils.offsetDateTimeFromString(startTimestamp,
					DomainUtils.OFFSET_DATE_TIME_8601);

			OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, dataValue, start);
			event.setReason(fetchReason(reason));

			resolveEvent(event);

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

	/********************* Web Socket Handler ***********************************/
	private class WebSocketTask implements Runnable {

		private final ApplicationMessage message;

		WebSocketTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
			} catch (Exception e) {
				// processing failed
				onException("Unable to process Web Socket equipment event ", e);
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

	/********************* Kafka Handler ***********************************/
	private class KafkaTask implements Runnable {

		private final ApplicationMessage message;

		KafkaTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
			} catch (Exception e) {
				// processing failed
				onException("Unable to process Kafka equipment event ", e);
			}
		}
	}

	/********************* Email Handler ***********************************/
	private class EmailTask implements Runnable {

		private final ApplicationMessage message;

		EmailTask(ApplicationMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			try {
				handleMessage(message);
			} catch (Exception e) {
				// processing failed
				onException("Unable to process email equipment event ", e);
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

				// send status message to each message server
				CollectorServerStatusMessage message = new CollectorServerStatusMessage(hostname, ip);

				String timeStamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(),
						DomainUtils.OFFSET_DATE_TIME_8601);
				message.setTimestamp(timeStamp);

				// publish to RMQ brokers
				for (RmqClient pubsub : appContext.getRmqClients()) {
					if (pubsub.shouldNotify()) {
						pubsub.sendNotificationMessage(message);

						if (logger.isInfoEnabled()) {
							logger.info("Sent RMQ " + pubsub.toString() + " status message for host " + getId());
						}
					}
				}

				// publish to JMS brokers
				for (JmsClient pubsub : appContext.getJmsClients()) {
					if (pubsub.shouldNotify()) {
						pubsub.sendNotificationMessage(message);

						if (logger.isInfoEnabled()) {
							logger.info("Sent JMS " + pubsub.toString() + " status message for host " + getId());
						}
					}
				}
				// publish to Kafka brokers
				for (KafkaOeeClient pubsub : appContext.getKafkaClients()) {
					if (pubsub.shouldNotify()) {
						pubsub.sendNotificationMessage(message);

						if (logger.isInfoEnabled()) {
							logger.info("Sent to Kafka " + pubsub.toString() + " status message for host " + getId());
						}
					}
				}

				// publish to MQTT brokers
				for (MqttOeeClient pubsub : appContext.getMqttClients()) {
					if (pubsub.shouldNotify()) {
						pubsub.sendNotificationMessage(message);

						if (logger.isInfoEnabled()) {
							logger.info("Sent MQTT " + pubsub.toString() + " status message for host " + getId());
						}
					}
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

				databaseClient.save(databaseEvent);

				// resolve event
				OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, dataValue, timestamp);
				event.setReason(fetchReason(reason));

				resolveEvent(event);

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

	// handle the cron event callback
	private class CronTask implements Runnable {
		private final JobExecutionContext context;

		CronTask(JobExecutionContext context) {
			this.context = context;
		}

		@Override
		public void run() {
			try {
				OffsetDateTime timestamp = OffsetDateTime.now();
				JobDataMap jobData = context.getJobDetail().getJobDataMap();
				String sourceId = (String) jobData.get(CronEventClient.SOURCE_ID_KEY);

				if (logger.isInfoEnabled()) {
					logger.info("Cron event, job: " + context.getJobDetail().getKey().getName() + ", source: "
							+ sourceId + ", timestamp: " + timestamp);
				}

				// resolve event
				OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, timestamp, timestamp);
				resolveEvent(event);
			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
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
					OeeEquipmentEvent event = new OeeEquipmentEvent(sourceId, fileContent, timestamp);
					resolveEvent(event);

					// move to pass folder
					fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.PASS_FOLDER);

				} catch (Exception e) {
					onException("Unable to invoke script resolver.", e);

					// fail
					try {
						fileClient.moveFile(file, FileEventClient.PROCESSING_FOLDER, FileEventClient.FAIL_FOLDER, e);
					} catch (Exception ex) {
						onException("Unable to move file.", ex);
					}
				}
			}
		}
	}

	// handle the Modbus event callback
	private class ModbusTask implements Runnable {
		private final ModbusEvent modbusEvent;

		ModbusTask(ModbusEvent event) {
			this.modbusEvent = event;
		}

		@Override
		public void run() {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Modbus event, source: " + modbusEvent.getSource());
				}

				// resolve event
				OeeEquipmentEvent event = new OeeEquipmentEvent(modbusEvent.getSourceId(), modbusEvent.getValues(),
						modbusEvent.getEventTime());
				event.setReason(fetchReason(modbusEvent.getReason()));
				resolveEvent(event);

			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
			}
		}
	}

	// handle the Proficy event callback
	private class ProficyTask implements Runnable {
		private final TagData tagData;

		ProficyTask(TagData tagData) {
			this.tagData = tagData;
		}

		@Override
		public void run() {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Proficy event, tag: " + tagData.getTagName());
				}

				// resolve event, tag name is source id
				// samples in chronological order
				TagDataType dataType = tagData.getEnumeratedType();

				for (TagSample sample : tagData.getSamples()) {
					// skip bad data
					if (sample.getEnumeratedQuality().equals(TagQuality.Good)) {
						// resolve event
						OeeEquipmentEvent event = new OeeEquipmentEvent(tagData.getTagName(),
								sample.getTypedValue(dataType), sample.getTimeStampTime());
						resolveEvent(event);
					}
				}
			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
			}
		}
	}
}
