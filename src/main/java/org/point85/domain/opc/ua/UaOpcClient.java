package org.point85.domain.opc.ua;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.CompositeProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.client.model.nodes.variables.ServerStatusNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class UaOpcClient {
	// logging utility
	private static Logger logger = LoggerFactory.getLogger(UaOpcClient.class);

	// request timeout (msec)
	private static final int REQUEST_TIMEOUT = 5000;

	private static final TimeUnit REQUEST_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

	private static final double MAX_AGE = 0.0d;

	private static final int SUBSCRIPTION_QUEUE_SIZE = 1;

	private static final double SAMPLING_INTERVAL = 0.0d;

	// future to complete the Milo client
	private final CompletableFuture<OpcUaClient> clientFuture = new CompletableFuture<>();

	// wrapped UA client
	private OpcUaClient opcUaClient;

	// security policy
	private SecurityPolicy securityPolicy;

	// identity provider
	private IdentityProvider identityProvider;

	// loader for certificates
	private final KeyStoreLoader keyStoreLoader = new KeyStoreLoader();

	// asynch callback listeners
	private List<OpcUaAsynchListener> asynchListeners = new ArrayList<>();

	// client handle counter for subscriptions
	private final AtomicLong clientHandles = new AtomicLong(1L);

	// registry of subscriptions
	private ConcurrentMap<NodeId, UaSubscription> subscriptionMap = new ConcurrentHashMap<>();

	public UaOpcClient() {
	}

	public OpcUaClient getNativeClient() {
		return this.opcUaClient;
	}

	private UaSubscription getSubscription(NodeId nodeId) {
		return subscriptionMap.get(nodeId);
	}

	private void registerSubscription(NodeId nodeId, UaSubscription subscription) {
		this.subscriptionMap.putIfAbsent(nodeId, subscription);
	}

	private void unregisterSubscription(NodeId nodeId) {
		this.subscriptionMap.remove(nodeId);
	}

	public boolean isSubscribed(NodeId nodeId) {
		return this.subscriptionMap.containsKey(nodeId);
	}

	public void registerAsynchListener(OpcUaAsynchListener listener) {
		if (!asynchListeners.contains(listener)) {
			asynchListeners.add(listener);
		}
	}

	public void unregisterAsynchListener(OpcUaAsynchListener listener) {
		asynchListeners.remove(listener);
	}

	// log into the server and connect
	public synchronized void connect(OpcUaSource source)
			// String protocol, String host, int port, String path, String user,
			// String password, SecurityPolicy policy)
			throws Exception {
		String endpointUrl = source.getEndpointUrl();
		logger.info("Connecting to: " + endpointUrl);

		final boolean simpleConnect = false;

		try {
			// identity provider
			String user = source.getUserName();
			String password = source.getPassword();
			if (user != null && user.length() > 0) {
				/*
				 * Authentication is handled by setting an appropriate IdentityProvider when
				 * building the UaTcpStackClientConfig.
				 */
				identityProvider = new CompositeProvider(new UsernameProvider(user, password), new AnonymousProvider());
			} else {
				identityProvider = new AnonymousProvider();
			}

			SecurityPolicy policy = source.getSecurityPolicy();
			if (policy != null) {
				securityPolicy = policy;
			} else {
				securityPolicy = SecurityPolicy.None;
			}

			// create the OpcUaClient
			EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints(endpointUrl).get();

			EndpointDescription endpoint = null;
			OpcUaClientConfig config = null;

			if (simpleConnect) {
				endpoint = endpoints[0];

				final OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();

				// just one endpoint
				cfg.setEndpoint(endpoints[0]);

				config = cfg.build();
			} else {
				endpoint = Arrays.stream(endpoints)
						.filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri())).findFirst()
						.orElseThrow(() -> new Exception("no desired endpoints returned"));

				logger.info("Using endpoint: " + endpoint.getEndpointUrl() + ", Security Policy: " + securityPolicy);

				keyStoreLoader.load();

				config = OpcUaClientConfig.builder()
						.setApplicationName(LocalizedText.english("Point85 OEE OPC UA Client"))
						.setApplicationUri("urn:point85:oee:client")
						.setCertificate(keyStoreLoader.getClientCertificate())
						.setKeyPair(keyStoreLoader.getClientKeyPair()).setEndpoint(endpoint)
						.setIdentityProvider(identityProvider).setRequestTimeout(uint(REQUEST_TIMEOUT)).build();
			}

			opcUaClient = new OpcUaClient(config);

			// synchronous connect
			opcUaClient.connect().get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

			clientFuture.complete(opcUaClient);

		} catch (Exception e) {
			throw new Exception("Unable to log in to server " + endpointUrl + ": " + e.getMessage());
		}
	}

	public synchronized void disconnect() throws Exception {
		if (opcUaClient != null) {
			try {
				opcUaClient.disconnect().get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);
				Stack.releaseSharedResources();
				clientFuture.complete(opcUaClient);
				opcUaClient = null;
			} catch (Exception e) {
				throw new Exception("Error disconnecting: " + e.getMessage());
			}
		} else {
			Stack.releaseSharedResources();
		}
	}

	private void checkPreconditions() throws Exception {
		if (opcUaClient == null) {
			throw new Exception("Not connected to an OPC UA server.");
		}
	}

	public synchronized List<DataValue> readSynch(List<NodeId> nodeIds) throws Exception {
		checkPreconditions();

		// synchronous read request
		try {
			List<DataValue> values = opcUaClient.readValues(MAX_AGE, TimestampsToReturn.Both, nodeIds)
					.get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

			clientFuture.complete(opcUaClient);
			return values;

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized List<StatusCode> writeSynch(List<NodeId> nodeIds, List<Variant> values) throws Exception {
		checkPreconditions();

		try {
			List<DataValue> dataValues = new ArrayList<>(values.size());

			for (Variant variant : values) {
				dataValues.add(new DataValue(variant, null, null));
			}
			List<StatusCode> codes = opcUaClient.writeValues(nodeIds, dataValues).get(REQUEST_TIMEOUT,
					REQUEST_TIMEOUT_UNIT);
			clientFuture.complete(opcUaClient);
			return codes;

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized void writeAsynch(List<NodeId> nodeIds, List<Variant> values) throws Exception {
		checkPreconditions();

		List<DataValue> dataValues = new ArrayList<>(values.size());

		for (Variant variant : values) {
			dataValues.add(new DataValue(variant, null, null));
		}

		CompletableFuture<List<StatusCode>> cf = opcUaClient.writeValues(nodeIds, dataValues);
		cf.thenAccept(statusCodes -> {
			for (OpcUaAsynchListener listener : asynchListeners) {
				listener.onOpcUaWrite(statusCodes);
			}
		});
		clientFuture.complete(opcUaClient);
	}

	public synchronized void readAsynch(NodeId nodeId) throws Exception {
		checkPreconditions();

		CompletableFuture<DataValue> cf = opcUaClient.readValue(MAX_AGE, TimestampsToReturn.Both, nodeId);
		cf.thenAccept(value -> {
			List<DataValue> values = new ArrayList<>(1);
			values.add(value);

			for (OpcUaAsynchListener listener : asynchListeners) {
				listener.onOpcUaRead(values);
			}
		});

		clientFuture.complete(opcUaClient);
	}

	public synchronized void readAsynch(List<NodeId> nodeIds) throws Exception {
		checkPreconditions();

		CompletableFuture<List<DataValue>> cf = opcUaClient.readValues(MAX_AGE, TimestampsToReturn.Both, nodeIds);
		cf.thenAccept(values -> {
			for (OpcUaAsynchListener listener : asynchListeners) {
				listener.onOpcUaRead(values);
			}
		});

		clientFuture.complete(opcUaClient);
	}

	public UInteger[] getArrayDimensions(NodeId nodeId) throws Exception {
		try {
			VariableNode node = opcUaClient.getAddressSpace().createVariableNode(nodeId);

			return node.getArrayDimensions().get();
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized DataValue readSynch(NodeId nodeId) throws Exception {
		checkPreconditions();

		try {
			VariableNode node = opcUaClient.getAddressSpace().createVariableNode(nodeId);
			DataValue value = node.readValue().get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

			// maybe not an array
			try {
				UInteger[] dims = node.getArrayDimensions().get();
				if (dims != null) {
					for (int i = 0; i < dims.length; i++) {
						logger.info("[" + i + "] " + dims[i]);
					}
				}
			} catch (Exception e) {

			}

			clientFuture.complete(opcUaClient);
			return value;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized StatusCode writeSynch(NodeId nodeId, Variant newValue) throws Exception {
		checkPreconditions();

		try {
			CompletableFuture<StatusCode> cf = opcUaClient.writeValue(nodeId, new DataValue(newValue, null, null));

			StatusCode statusCode = cf.get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);
			clientFuture.complete(opcUaClient);
			return statusCode;

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized void writeAsynch(NodeId nodeId, Variant value) throws Exception {
		checkPreconditions();

		try {
			DataValue dataValue = new DataValue(value, null, null);
			CompletableFuture<StatusCode> cf = opcUaClient.writeValue(nodeId, dataValue);

			cf.thenAccept(statusCode -> {
				List<StatusCode> statusCodes = new ArrayList<>(1);
				statusCodes.add(statusCode);

				for (OpcUaAsynchListener listener : asynchListeners) {
					listener.onOpcUaWrite(statusCodes);
				}
			});
			clientFuture.complete(opcUaClient);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public static Class<?> getJavaDataType(Variant value) {
		Class<?> javaClass = null;
		NodeId nodeId = value.getDataType().get();
		Class<?> clazz = BuiltinDataType.getBackingClass(nodeId);

		if (nodeId.getType().equals(IdType.Numeric)) {
			if (clazz.equals(Integer.class) || clazz.equals(UInteger.class)) {
				javaClass = Integer.class;
			} else if (clazz.equals(Short.class) || clazz.equals(UShort.class)) {
				javaClass = Short.class;
			} else if (clazz.equals(Boolean.class)) {
				javaClass = Boolean.class;
			} else if (clazz.equals(UByte.class) || clazz.equals(Byte.class)) {
				javaClass = Byte.class;
			} else if (clazz.equals(ULong.class) || clazz.equals(Long.class)) {
				javaClass = Long.class;
			} else if (clazz.equals(Float.class)) {
				javaClass = Float.class;
			} else if (clazz.equals(Double.class)) {
				javaClass = Double.class;
			} else if (clazz.equals(DateTime.class)) {
				javaClass = DateTime.class;
			} else if (clazz.equals(java.lang.String.class)) {
				javaClass = String.class;
			}
		} else if (nodeId.getType().equals(IdType.String)) {
			javaClass = String.class;
		} else if (nodeId.getType().equals(IdType.Guid)) {
			javaClass = UUID.class;
		}
		return javaClass;
	}

	public static NodeId getNodeId(ExpandedNodeId expandedId) {
		UShort ns = expandedId.getNamespaceIndex();
		Class<?> clazz = expandedId.getIdentifier().getClass();
		Object identifier = expandedId.getIdentifier();

		NodeId nodeId = NodeId.NULL_VALUE;

		if (clazz.equals(UInteger.class)) {
			nodeId = new NodeId(ns, (UInteger) identifier);
		} else if (clazz.equals(String.class)) {
			nodeId = new NodeId(ns, (String) identifier);
		} else if (clazz.equals(UUID.class)) {
			nodeId = new NodeId(ns, (UUID) identifier);
		} else if (clazz.equals(ByteString.class)) {
			nodeId = new NodeId(ns, (ByteString) identifier);
		}
		return nodeId;
	}

	public synchronized List<ReferenceDescription> browseSynch(NodeId parentNode) throws Exception {
		try {
			BrowseDescription browseDescription = new BrowseDescription(parentNode, BrowseDirection.Forward,
					Identifiers.References, true, uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
					uint(BrowseResultMask.All.getValue()));

			BrowseResult browseResult = opcUaClient.browse(browseDescription).get();

			List<ReferenceDescription> referenceDescriptions = toList(browseResult.getReferences());

			clientFuture.complete(opcUaClient);

			return referenceDescriptions;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized List<BrowseResult> browse(List<NodeId> nodeIds) throws Exception {
		try {
			List<BrowseDescription> nodesToBrowse = new ArrayList<>(nodeIds.size());

			for (NodeId nodeId : nodeIds) {
				BrowseDescription browseDescription = new BrowseDescription(nodeId, BrowseDirection.Forward,
						Identifiers.References, true, uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
						uint(BrowseResultMask.All.getValue()));
				nodesToBrowse.add(browseDescription);
			}

			List<BrowseResult> browseResults = opcUaClient.browse(nodesToBrowse).get(REQUEST_TIMEOUT,
					REQUEST_TIMEOUT_UNIT);

			clientFuture.complete(opcUaClient);

			return browseResults;

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
		for (OpcUaAsynchListener listener : this.asynchListeners) {
			listener.onOpcUaSubscription(value, item);
		}
	}

	private UaSubscription establishSubscription(NodeId nodeId, double publishingInterval, ExtensionObject filter)
			throws Exception {
		// node to read
		ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);

		// client handle must be unique per item
		UInteger clientHandle = uint(clientHandles.getAndIncrement());

		// discard oldest value
		MonitoringParameters parameters = new MonitoringParameters(clientHandle, SAMPLING_INTERVAL, filter,
				uint(SUBSCRIPTION_QUEUE_SIZE), true);

		MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting,
				parameters);

		// when creating items in MonitoringMode.Reporting this callback is
		// where each item needs to have its
		// value/event consumer hooked up. The alternative is to create the item
		// in sampling mode, hook up the
		// consumer after the creation call completes, and then change the mode
		// for all items to reporting.
		BiConsumer<UaMonitoredItem, Integer> onItemCreated = (item, id) -> item
				.setValueConsumer(this::onSubscriptionValue);

		// create a subscription
		UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(publishingInterval)
				.get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

		List<UaMonitoredItem> items = subscription
				.createMonitoredItems(TimestampsToReturn.Both, newArrayList(request), onItemCreated)
				.get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

		for (UaMonitoredItem item : items) {
			if (item.getStatusCode().isGood()) {
				logger.info("Monitored item created for nodeId: " + item.getReadValueId().getNodeId());
			} else {
				throw new Exception("Failed to create monitored item for nodeId: " + item.getReadValueId().getNodeId()
						+ ", code: " + item.getStatusCode());
			}
		}

		clientFuture.complete(opcUaClient);

		return subscription;
	}

	public synchronized void subscribe(NodeId nodeId, double publishingInterval, ExtensionObject filter)
			throws Exception {
		checkPreconditions();

		try {
			// check to see if modified
			UaSubscription subscription = getSubscription(nodeId);

			if (subscription != null) {
				// modify it by publishing interval
				opcUaClient.getSubscriptionManager().modifySubscription(subscription.getSubscriptionId(),
						publishingInterval);
			}

			// create subscription
			subscription = establishSubscription(nodeId, publishingInterval, filter);

			// save in map
			registerSubscription(nodeId, subscription);

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized void unsubscribe(NodeId nodeId) throws Exception {
		try {
			if (nodeId == null) {
				return;
			}

			// from the registry
			UaSubscription subscription = getSubscription(nodeId);

			if (subscription == null) {
				return;
			}

			ImmutableList<UaMonitoredItem> items = subscription.getMonitoredItems();

			List<StatusCode> codes = subscription.deleteMonitoredItems(items).get(REQUEST_TIMEOUT,
					REQUEST_TIMEOUT_UNIT);

			for (StatusCode code : codes) {
				if (!code.isGood()) {
					throw new Exception("Unable to unsubscribe from node " + nodeId + ": " + code);
				}
			}

			opcUaClient.getSubscriptionManager().deleteSubscription(subscription.getSubscriptionId());

			// remove from registry
			unregisterSubscription(nodeId);

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public synchronized List<Object> callMethodSynch(NodeId objectId, NodeId methodId, List<Object> inputArguments)
			throws Exception {
		try {
			Variant[] variants = new Variant[inputArguments.size()];
			for (int i = 0; i < inputArguments.size(); i++) {
				variants[i] = new Variant(inputArguments.get(i));
			}

			CallMethodRequest request = new CallMethodRequest(objectId, methodId, variants);

			CompletableFuture<List<Object>> cf = opcUaClient.call(request).thenCompose(result -> {
				StatusCode statusCode = result.getStatusCode();

				if (statusCode.isGood()) {
					Variant[] outputArguments = result.getOutputArguments();

					List<Object> outputs = new ArrayList<>(outputArguments.length);

					for (int i = 0; i < outputArguments.length; i++) {
						outputs.add(outputArguments[i].getValue());
					}

					return CompletableFuture.completedFuture(outputs);
				} else {
					CompletableFuture<List<Object>> f = new CompletableFuture<>();
					f.completeExceptionally(new org.eclipse.milo.opcua.stack.core.UaException(statusCode));
					return f;
				}
			});

			List<Object> results = cf.get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);

			clientFuture.complete(opcUaClient);

			return results;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public DateTime getServerCurrentTime() throws Exception {
		try {
			ServerNode serverNode = opcUaClient.getAddressSpace().getObjectNode(Identifiers.Server, ServerNode.class)
					.get();

			ServerStatusNode serverStatusNode = serverNode.getServerStatusNode().get();
			DateTime currentTime = serverStatusNode.getCurrentTime().get();
			return currentTime;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public OpcUaServerStatus getServerStatus() throws Exception {
		try {
			OpcUaServerStatus serverStatus = new OpcUaServerStatus();

			// get build info separately
			VariableNode buildInfoNode = opcUaClient.getAddressSpace()
					.createVariableNode(Identifiers.Server_ServerStatus_BuildInfo);

			BuildInfo buildInfo = buildInfoNode.getValue().thenApply(ExtensionObject.class::cast)
					.thenApply(ExtensionObject::<BuildInfo>decode).get();

			// Get a typed reference to the Server object: ServerNode
			ServerNode serverNode = opcUaClient.getAddressSpace().getObjectNode(Identifiers.Server, ServerNode.class)
					.get();

			// Read properties of the Server object...
			// String[] serverArray = serverNode.getServerArray().get();
			// String[] namespaceArray = serverNode.getNamespaceArray().get();

			// Read the value of attribute the ServerStatus variable component
			// ServerStatusDataType serverStatusType =
			// serverNode.getServerStatus().get();

			// Get a typed reference to the ServerStatus variable
			// component and read value attributes individually
			ServerStatusNode serverStatusNode = serverNode.getServerStatusNode().get();

			// TODO throws class cast exception
			// BuildInfo buildInfo = serverStatusNode.getBuildInfo().get();
			DateTime startTime = serverStatusNode.getStartTime().get();
			ServerState state = serverStatusNode.getState().get();

			/*
			 * // synchronous read List<NodeId> nodeIds =
			 * newArrayList(Identifiers.Server_ServerStatus_State,
			 * Identifiers.Server_ServerStatus_StartTime,
			 * Identifiers.Server_ServerStatus_BuildInfo_ManufacturerName,
			 * Identifiers.Server_ServerStatus_BuildInfo_ProductName);
			 * 
			 * List<DataValue> dataValues = readSynch(nodeIds);
			 * 
			 * for (DataValue dataValue : dataValues) { StatusCode code =
			 * dataValue.getStatusCode(); if (!code.isGood()) { throw new
			 * Exception("Bad read"); }
			 * 
			 * Class<?> javaClass =
			 * Point85OpcUaClient.getJavaDataType(dataValue.getValue());
			 * logger.info("Synch Read: " + dataValue.getValue().getValue() + " of type: " +
			 * javaClass); }
			 * 
			 * // state Integer i = (Integer) dataValues.get(0).getValue().getValue();
			 * ServerState state = ServerState.from(i); serverStatus.setState(state);
			 * 
			 * // start time DateTime dt = (DateTime)
			 * dataValues.get(1).getValue().getValue();
			 */
			serverStatus.setState(state);
			serverStatus.setBuildInfo(buildInfo);
			serverStatus.setStartTime(startTime);

			return serverStatus;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	public boolean isConnected() {
		return opcUaClient != null ? true : false;
	}
}
