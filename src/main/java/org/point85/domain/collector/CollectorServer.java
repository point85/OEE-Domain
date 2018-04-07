package org.point85.domain.collector;

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
import org.point85.domain.http.HttpEventListener;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.messaging.RoutingKey;
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
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class CollectorServer
		implements HttpEventListener, OpcDaDataChangeListener, OpcUaAsynchListener, MessageListener {
	// msec between status checks
	private static final long HEARTBEAT_SEC = 60;

	// sec for heartbeat message to live in the queue
	private static final int HEARTBEAT_TTL_SEC = 3600;

	// sec for a status message to live in the queue
	private static final int STATUS_TTL_SEC = 3600;

	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// thread pool service
	private ExecutorService executorService = Executors.newCachedThreadPool();

	// timer to broadcast status
	private Timer heartbeatTimer;

	// status task
	private HeartbeatTask heartbeatTask;

	// serializer
	protected Gson gson;

	// counter for pubsub queues
	private int queueCounter = 0;

	// script execution context
	private OeeContext appContext;

	private EquipmentEventResolver equipmentResolver;

	// data collectors
	private List<DataCollector> collectors;

	// JVM host name
	private String hostname;

	// JVM host IP address
	private String ip;

	private Map<String, OpcDaInfo> opcDaSubscriptionMap = new HashMap<>();
	private Map<String, OpcUaInfo> opcUaSubscriptionMap = new HashMap<>();
	private Map<String, HttpServerSource> httpServerMap = new HashMap<>();
	private Map<String, MessageBrokerSource> messageBrokerMap = new HashMap<>();

	// exception listener
	private CollectorExceptionListener exceptionListener;

	public CollectorServer() {
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

	private void connectToEventBrokers(Map<String, MessageBrokerSource> brokerSources) throws Exception {
		for (Entry<String, MessageBrokerSource> entry : brokerSources.entrySet()) {
			MessagingSource source = entry.getValue().getSource();

			PublisherSubscriber pubsub = new PublisherSubscriber();
			appContext.getPublisherSubscribers().add(pubsub);

			String brokerHostName = source.getHost();
			Integer port = source.getPort();

			String queueName = getClass().getSimpleName() + "_" + queueCounter++;
			List<RoutingKey> routingKeys = new ArrayList<>();
			routingKeys.add(RoutingKey.EQUIPMENT_SOURCE_EVENT);

			pubsub.connectToBroker(brokerHostName, port, queueName, false, routingKeys, this);

			if (logger.isInfoEnabled()) {
				logger.info("Started RMQ event pubsub: " + source.getId());
			}
		}
	}

	private void startHttpServers(Map<String, HttpServerSource> httpServerSources) throws Exception {
		for (Entry<String, HttpServerSource> entry : httpServerSources.entrySet()) {
			HttpSource source = entry.getValue().getSource();

			Integer port = source.getPort();
			OeeHttpServer httpServer = new OeeHttpServer(port);
			appContext.getHttpServers().add(httpServer);
			httpServer.setDataChangeListener(this);
			httpServer.setAcceptingEventRequests(true);
			httpServer.startup();

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

		if (publishingInterval < uaInfo.getPublishingInteval()) {
			uaInfo.setPublishingInteval(publishingInterval);
		}
	}

	public void subscribeToOpcUaSources(Map<String, OpcUaInfo> uaSubscriptions) throws Exception {
		for (Entry<String, OpcUaInfo> entry : uaSubscriptions.entrySet()) {
			OpcUaInfo uaInfo = entry.getValue();

			UaOpcClient uaClient = new UaOpcClient();
			uaClient.connect(uaInfo.getSource());

			appContext.getOpcUaClients().add(uaClient);

			uaClient.registerAsynchListener(this);

			double publishingInterval = uaInfo.getPublishingInteval();

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

		// fetch runnable script resolvers from database
		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);
		List<EventResolver> resolvers = PersistenceService.instance().fetchEventResolversByHost(hostNames, states);

		if (resolvers.size() == 0) {
			if (logger.isInfoEnabled()) {
				logger.info("No resolvers found for hosts " + hostNames + " in the " + CollectorState.READY + " state");
			}
		}

		for (EventResolver resolver : resolvers) {
			// build list of runnable collectors
			DataCollector collector = resolver.getCollector();

			if (!collectors.contains(collector)) {
				collectors.add(collector);

				if (logger.isInfoEnabled()) {
					logger.info("Found data collector " + collector.getName() + ", for host " + collector.getHost()
							+ ", resolver: " + resolver);
				}
			}

			// gather data for each data source
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

			default:
				break;
			}
		}
	}

	public void startDataCollection() throws Exception {

		// collect data for OPC DA
		monitorOpcDaTags(opcDaSubscriptionMap);

		// collect data for OPC UA
		subscribeToOpcUaSources(opcUaSubscriptionMap);

		// collect data for HTTP
		startHttpServers(httpServerMap);

		// collect data for RMQ
		connectToEventBrokers(messageBrokerMap);

		if (logger.isInfoEnabled()) {
			logger.info("Startup finished.");
		}

		// update collector state to running
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

		buildDataSources();

		// connect to notification broker
		startNotifications();

		// start collecting data
		startDataCollection();

		// notify monitors
		onInformation("Collector server started on host " + getId());
	}

	private void startNotifications() throws Exception {
		// connect to notification brokers for publishing only
		Map<String, PublisherSubscriber> pubSubs = new HashMap<>();

		for (DataCollector collector : collectors) {
			String brokerHostName = collector.getBrokerHost();
			Integer brokerPort = collector.getBrokerPort();

			if (brokerHostName != null) {
				String key = brokerHostName + ":" + brokerPort;

				if (pubSubs.get(key) == null) {
					// new publisher
					PublisherSubscriber pubsub = new PublisherSubscriber();
					pubSubs.put(key, pubsub);

					// connect to broker
					pubsub.connect(brokerHostName, brokerPort);

					appContext.getPublisherSubscribers().add(pubsub);

					if (logger.isInfoEnabled()) {
						logger.info("Connected to RMQ broker " + key + " for collector " + collector.getName());
					}
				}
			}
		}

		// maybe start status publishing
		if (appContext.getPublisherSubscribers().size() > 0 && heartbeatTimer == null) {
			// create timer and task
			heartbeatTimer = new Timer();
			heartbeatTask = new HeartbeatTask();
			heartbeatTimer.schedule(heartbeatTask, HEARTBEAT_SEC * 1000, HEARTBEAT_SEC * 1000);
		}
	}

	private void sendNotification(String text, NotificationSeverity severity) {
		if (appContext.getPublisherSubscribers().size() == 0) {
			return;
		}

		CollectorNotificationMessage message = new CollectorNotificationMessage(hostname, ip);
		message.setText(text);
		message.setSeverity(severity);

		Integer ttl = null;

		if (severity.equals(NotificationSeverity.INFO)) {
			ttl = CollectorServer.STATUS_TTL_SEC;
		}

		for (PublisherSubscriber pubSub : appContext.getPublisherSubscribers()) {
			try {
				pubSub.publish(message, RoutingKey.NOTIFICATION_MESSAGE, ttl);
			} catch (Exception e) {
				logger.error("Unable to publish notification.", e);
			}
		}
	}

	private void saveCollectorState(CollectorState state) throws Exception {
		List<DataCollector> savedCollectors = new ArrayList<>();

		for (DataCollector collector : collectors) {
			collector.setCollectorState(state);
			DataCollector saved = (DataCollector) PersistenceService.instance().save(collector);

			if (logger.isInfoEnabled()) {
				logger.info("Saved collector " + collector.getName());
			}
			savedCollectors.add(saved);
		}
		collectors = savedCollectors;
	}

	public void stopNotifications() throws Exception {
		for (PublisherSubscriber pubsub : appContext.getPublisherSubscribers()) {
			pubsub.disconnect();
		}
		appContext.getPublisherSubscribers().clear();
	}

	public void stopDataCollection() throws Exception {
		// set back to ready
		saveCollectorState(CollectorState.READY);

		// clear resolution caches
		equipmentResolver.clearCache();

		// disconnect from RMQ brokers
		for (PublisherSubscriber pubsub : appContext.getPublisherSubscribers()) {
			pubsub.disconnect();
			onInformation("Disconnected from pubsub with binding key " + pubsub.getBindingKey());
		}
		appContext.getPublisherSubscribers().clear();

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
			logger.error(e.getMessage());
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
			logger.info("Shutdown finished, exiting now");
		}

		System.exit(0);
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
		if (logger.isInfoEnabled()) {
			logger.info("Unsubscribed from data sources.");
		}
	}

	public void subscribeToDataSource() {
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

	@Override
	public void onOpcDaDataChange(OpcDaMonitoredItem item) {
		// execute on separate thread
		getExecutorService().execute(new OpcDaTask(item));
	}

	public void saveAvailabilityRecord(ResolvedEvent event) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Availability reason " + event.getReason().getName() + ", Loss Category: "
					+ event.getReason().getLossCategory());
		}

		// next availability
		AvailabilityRecord nextRecord = new AvailabilityRecord(event);

		// close off last availability
		AvailabilityRecord lastRecord = PersistenceService.instance().fetchLastAvailability(event.getEquipment());

		if (lastRecord != null) {
			lastRecord.setEndTime(nextRecord.getStartTime());
			Duration duration = Duration.between(lastRecord.getStartTime(), lastRecord.getEndTime());
			lastRecord.setDuration(duration);
		}

		PersistenceService.instance().save(lastRecord, nextRecord);
	}

	public void saveSetupRecord(ResolvedEvent event) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Job change " + event.getJob());
		}

		// next setup
		SetupRecord nextRecord = new SetupRecord(event);

		// close off last setup
		SetupRecord lastRecord = PersistenceService.instance().fetchLastSetup(event.getEquipment());

		if (lastRecord != null) {
			lastRecord.setEndTime(nextRecord.getStartTime());
		}

		PersistenceService.instance().save(lastRecord, nextRecord);
	}

	public void saveProductionRecord(ResolvedEvent event) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Production " + event.getQuantity() + " for type " + event.getResolverType());
		}

		// next production
		ProductionRecord nextRecord = new ProductionRecord(event);

		// close off last production
		ProductionRecord lastRecord = PersistenceService.instance().fetchLastProduction(event.getEquipment());

		if (lastRecord != null) {
			lastRecord.setEndTime(nextRecord.getStartTime());
		}

		PersistenceService.instance().save(lastRecord, nextRecord);
	}

	protected void onOtherResolution(ResolvedEvent resolvedItem) {
		if (logger.isInfoEnabled()) {
			logger.info("Other ");
		}
	}

	@Override
	public void onOpcUaRead(List<DataValue> dataValues) {
	}

	@Override
	public void onOpcUaWrite(List<StatusCode> statusCodes) {
	}

	@Override
	public void onOpcUaSubscription(DataValue dataValue, UaMonitoredItem item) {
		getExecutorService().execute(new OpcUaTask(dataValue, item));
	}

	protected void sendMessage(PublisherSubscriber pubsub, ApplicationMessage message, RoutingKey routingKey) {
		// we publish only to the data recorder
		MessageSender sender = new MessageSender(pubsub, message, routingKey);
		this.executorService.execute(sender);
	}

	public void onException(String preface, Exception any) {
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

		// callback listener
		if (exceptionListener != null) {
			exceptionListener.onException(any);
		}
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
		if (message == null) {
			return;
		}

		// ack it now
		try {
			channel.basicAck(envelope.getDeliveryTag(), PublisherSubscriber.ACK_MULTIPLE);
		} catch (Exception e) {
			onException("Failed to ack message.", e);
			return;
		}
		// execute on worker thread
		MessageTask task = new MessageTask(channel, envelope, message);
		executorService.execute(task);
	}

	// subscribed OPC DA items by source
	private class OpcDaInfo {
		private OpcDaSource source;

		private Map<String, List<TagItemInfo>> subscribedItems = new HashMap<>();

		private OpcDaInfo(OpcDaSource source) {
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
		private OpcUaSource source;

		private List<NodeId> monitoredNodes = new ArrayList<>();

		private double publishingInteval = Double.MAX_VALUE;

		private OpcUaInfo(OpcUaSource source) {
			this.source = source;
		}

		private List<NodeId> getMonitoredNodes() {
			return monitoredNodes;
		}

		private double getPublishingInteval() {
			return publishingInteval;
		}

		private void setPublishingInteval(double publishingInteval) {
			this.publishingInteval = publishingInteval;
		}

		private OpcUaSource getSource() {
			return source;
		}
	}

	// RMQ brokers
	private class MessageBrokerSource {
		private MessagingSource source;

		private MessageBrokerSource(MessagingSource source) {
			this.source = source;
		}

		private MessagingSource getSource() {
			return source;
		}
	}

	// HTTP servers
	private class HttpServerSource {
		private HttpSource source;

		private HttpServerSource(HttpSource source) {
			this.source = source;
		}

		private HttpSource getSource() {
			return source;
		}
	}

	// handle the HTTP callback
	private class HttpTask implements Runnable {
		private String sourceId;
		private String dataValue;
		private OffsetDateTime timestamp;

		private HttpTask(String sourceId, String dataValue, OffsetDateTime timestamp) {
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

				// find resolver
				EventResolver eventResolver = equipmentResolver.getResolver(sourceId);

				ResolvedEvent resolvedDataItem = equipmentResolver.invokeResolver(eventResolver, getAppContext(),
						dataValue, timestamp);

				recordResolution(resolvedDataItem);

			} catch (Exception e) {
				onException("Unable to invoke script resolver.", e);
			}
		}
	}

	// handle the OPC UA callback
	private class OpcUaTask implements Runnable {
		private DataValue dataValue;
		private UaMonitoredItem item;

		private OpcUaTask(DataValue dataValue, UaMonitoredItem item) {
			this.dataValue = dataValue;
			this.item = item;
		}

		@Override
		public void run() {
			try {
				Object javaValue = dataValue.getValue().getValue();
				String itemId = item.getReadValueId().getNodeId().toParseableString();
				DateTime dt = dataValue.getServerTime();
				OffsetDateTime odt = DomainUtils.localTimeFromDateTime(dt);

				if (logger.isInfoEnabled()) {
					logger.info(
							"OPC UA subscription, node: " + itemId + ", value: " + javaValue + ", timestamp: " + odt);
				}

				EventResolver eventResolver = equipmentResolver.getResolver(itemId);

				ResolvedEvent resolvedEvent = equipmentResolver.invokeResolver(eventResolver, getAppContext(),
						javaValue, odt);

				recordResolution(resolvedEvent);

			} catch (Exception e) {
				onException("Unable to invoke OPC UA script resolver.", e);
			}
		}
	}

	private synchronized void recordResolution(ResolvedEvent resolvedEvent) throws Exception {
		EventResolverType type = resolvedEvent.getResolverType();

		// first in database
		switch (type) {
		case AVAILABILITY:
			saveAvailabilityRecord(resolvedEvent);
			break;

		case JOB_CHANGE:
		case MATL_CHANGE:
			saveSetupRecord(resolvedEvent);
			break;

		case OTHER:
			onOtherResolution(resolvedEvent);
			break;

		case PROD_GOOD:
		case PROD_REJECT:
		case PROD_STARTUP:
			saveProductionRecord(resolvedEvent);
			break;

		default:
			throw new Exception("Invalid resolver type " + type);
		}

		// send event message
		sendResolutionMessage(resolvedEvent);
	}

	private void sendResolutionMessage(ResolvedEvent resolvedEvent) {
		try {
			if (appContext.getPublisherSubscribers().size() == 0) {
				return;
			}

			// send resolution message to each subscriber
			CollectorResolvedEventMessage message = new CollectorResolvedEventMessage(hostname, ip);
			message.fromResolvedEvent(resolvedEvent);

			for (PublisherSubscriber pubsub : appContext.getPublisherSubscribers()) {
				pubsub.publish(message, RoutingKey.RESOLVED_EVENT, HEARTBEAT_TTL_SEC);
			}

			if (logger.isInfoEnabled()) {
				logger.info("Sent resolved event message for host " + getId());
			}
		} catch (Exception e) {
			onException("Sending resolved event message failed.", e);
		}
	}

	/*
	 * public void onWebEquipmentEvent(Equipment equipment, EventResolverType
	 * resolverType, Object sourceValue, OffsetDateTime timestamp) {
	 * getExecutorService().execute(new WebTask(equipment, resolverType,
	 * sourceValue, timestamp)); }
	 */

	public void registerExceptionLisener(CollectorExceptionListener listener) {
		this.exceptionListener = listener;
	}

	/********************* OPC DA ***********************************/

	// handle the OPC DA callback
	private class OpcDaTask implements Runnable {
		private OpcDaMonitoredItem item;

		private OpcDaTask(OpcDaMonitoredItem item) {
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

				if (logger.isInfoEnabled()) {
					logger.info("OPC DA data change, group: " + item.getGroup().getName() + ", item: " + sourceId
							+ ", value: " + item.getValueString() + ", timestamp: " + item.getLocalTimestamp());
				}

				// get resolver to process data value
				EventResolver eventResolver = equipmentResolver.getResolver(sourceId);

				if (eventResolver == null) {
					throw new Exception("The OPC DA script resolver is undefined for source id " + sourceId);
				}

				// resolver the data
				ResolvedEvent resolvedDataItem = equipmentResolver.invokeResolver(eventResolver, getAppContext(),
						dataValue, item.getLocalTimestamp());

				// save resolution
				recordResolution(resolvedDataItem);

			} catch (Exception e) {
				onException("Unable to invoke OPC DA script resolver.", e);
			}
		}
	}

	/********************* MessageHandler ***********************************/
	private class MessageTask implements Runnable {

		private Channel channel;
		private Envelope envelope;
		private ApplicationMessage message;

		private MessageTask(Channel channel, Envelope envelope, ApplicationMessage message) {
			this.envelope = envelope;
			this.message = message;
			this.channel = channel;
		}

		@Override
		public void run() {
			try {
				MessageType type = message.getMessageType();

				if (type.equals(MessageType.EQUIPMENT_EVENT)) {
					EquipmentEventMessage eventMessage = (EquipmentEventMessage) message;
					String dataValue = eventMessage.getValue();
					String sourceId = eventMessage.getSourceId();
					OffsetDateTime timestamp = DomainUtils.offsetDateTimeFromString(eventMessage.getTimestamp());

					if (logger.isInfoEnabled()) {
						logger.info("Equipment event, source: " + sourceId + ", value: " + dataValue + ", timestamp: "
								+ timestamp);
					}

					// find resolver
					EventResolver eventResolver = equipmentResolver.getResolver(sourceId);

					if (eventResolver != null) {
						ResolvedEvent resolvedDataItem = equipmentResolver.invokeResolver(eventResolver,
								getAppContext(), dataValue, timestamp);

						recordResolution(resolvedDataItem);
					}
				}
			} catch (Exception e) {
				// processing failed
				onException("Unable to invoke messaging script resolver.", e);
			} finally {
				// ack message
				try {
					if (channel.isOpen()) {
						channel.basicAck(envelope.getDeliveryTag(), PublisherSubscriber.ACK_MULTIPLE);
					}
				} catch (Exception ex) {
					// ack failed
					onException("Unable to ack message.", ex);
				}
			}
		}
	}

	/********************* MessageHandler ***********************************/
	private class MessageSender implements Runnable {

		private PublisherSubscriber pubsub;
		private ApplicationMessage message;
		private RoutingKey routingKey;

		MessageSender(PublisherSubscriber pubsub, ApplicationMessage message, RoutingKey routingKey) {
			this.message = message;
			this.routingKey = routingKey;
			this.pubsub = pubsub;
		}

		@Override
		public void run() {
			try {
				if (message != null) {
					// send message
					pubsub.publish(message, routingKey);
				}
			} catch (Exception e) {
				// sending failed
				onException("Unable to send message.", e);
			}
		}
	}

	/********************* Status Task ***********************************/
	private class HeartbeatTask extends TimerTask {
		@Override
		public void run() {
			try {
				if (appContext.getPublisherSubscribers().size() == 0) {
					return;
				}

				// send status message to each PubSub
				CollectorServerStatusMessage message = new CollectorServerStatusMessage(hostname, ip);

				for (PublisherSubscriber pubsub : appContext.getPublisherSubscribers()) {
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

	/*
	 * // data from a manual user interface private class WebTask implements
	 * Runnable { private Equipment equipment; private EventResolverType
	 * resolverType; private Object sourceValue; private OffsetDateTime timestamp;
	 * 
	 * private WebTask(Equipment equipment, EventResolverType resolverType, Object
	 * sourceValue, OffsetDateTime timestamp) { this.equipment = equipment;
	 * this.resolverType = resolverType; this.sourceValue = sourceValue;
	 * this.timestamp = timestamp; }
	 * 
	 * @Override public void run() { try { if (logger.isInfoEnabled()) {
	 * logger.info("Web event, equipment: " + equipment.getName() + ", type: " +
	 * resolverType + ", value: " + sourceValue + ", timestamp: " + timestamp); }
	 * 
	 * EquipmentEventResolver equipmentResolver = new EquipmentEventResolver();
	 * 
	 * // find resolver by type List<EventResolver> resolvers =
	 * equipmentResolver.getResolvers(equipment);
	 * 
	 * EventResolver configuredResolver = null; for (EventResolver resolver :
	 * resolvers) { if (resolver.getType().equals(resolverType)) {
	 * configuredResolver = resolver; break; } }
	 * 
	 * if (configuredResolver == null) { throw new
	 * Exception("No script resolver found for equipment " + equipment.getName() +
	 * " with type " + resolverType); }
	 * 
	 * ResolvedEvent event = equipmentResolver.invokeResolver(configuredResolver,
	 * appContext, sourceValue, timestamp);
	 * 
	 * recordResolution(event);
	 * 
	 * } catch (Exception e) { onException("Unable to invoke script resolver.", e);
	 * } } }
	 */
}
