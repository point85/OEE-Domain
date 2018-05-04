package org.point85.domain.opc.ua;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
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
import org.eclipse.milo.opcua.stack.core.UaException;
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
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
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
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class UaOpcClient implements SessionActivityListener {
	// logging utility
	private static Logger logger = LoggerFactory.getLogger(UaOpcClient.class);

	private static final String APP_NAME = "Point85 OEE OPC UA Client";
	private static final String APP_URI = "urn:point85:oee:client";
	private static final String APP_ORG = "Point85";
	private static final String APP_UNIT = "dev";
	private static final String APP_CITY = "Los Altos";
	private static final String APP_STATE = "CA";
	private static final String APP_COUNTRY = "US";

	private KeyPair keyPair;
	private X509Certificate certificate;

	// request timeout (msec)
	private static final int REQUEST_TIMEOUT = 5000;

	private static final TimeUnit REQUEST_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

	private static final double MAX_AGE = 0.0d;

	private static final int SUBSCRIPTION_QUEUE_SIZE = 1;

	private static final double SAMPLING_INTERVAL = 0.0d;

	// wrapped UA client
	private OpcUaClient opcUaClient;

	// security policy
	private SecurityPolicy securityPolicy;

	// message security mode
	private MessageSecurityMode messageSecurityMode;

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

	private final AtomicReference<BiConsumer<Boolean, Throwable>> listener = new AtomicReference<>();

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

	private EndpointDescription chooseEndpoint(EndpointDescription[] endpoints, SecurityPolicy minSecurityPolicy,
			MessageSecurityMode minMessageSecurityMode) {
		EndpointDescription bestFound = null;
		SecurityPolicy bestFoundSecurityPolicy = null;
		for (EndpointDescription endpoint : endpoints) {
			SecurityPolicy endpointSecurityPolicy;
			try {
				endpointSecurityPolicy = SecurityPolicy.fromUri(endpoint.getSecurityPolicyUri());
			} catch (UaException e) {
				continue;
			}
			if (minSecurityPolicy.compareTo(endpointSecurityPolicy) <= 0) {
				if (minMessageSecurityMode.compareTo(endpoint.getSecurityMode()) <= 0) {
					// Found endpoint which fulfills minimum requirements
					if (bestFound == null) {
						bestFound = endpoint;
						bestFoundSecurityPolicy = endpointSecurityPolicy;
					} else {
						if (bestFoundSecurityPolicy.compareTo(endpointSecurityPolicy) < 0) {
							// Found endpoint that has higher security than previously found one
							bestFound = endpoint;
							bestFoundSecurityPolicy = endpointSecurityPolicy;
						}
					}
				}
			}
		}
		if (bestFound == null) {
			throw new RuntimeException("no desired endpoints returned");
		} else {
			return bestFound;
		}
	}

	private IdentityProvider getIdentityProvider() {
		return new AnonymousProvider();
	}

	private X509Certificate getClientCertificate() {
		if (certificate == null) {
			generateSelfSignedCertificate();
		}
		return certificate;
	}

	KeyPair getKeyPair() {
		if (keyPair == null) {
			generateSelfSignedCertificate();
		}
		return keyPair;
	}

	protected void generateSelfSignedCertificate() {
		// Generate self-signed certificate
		try {
			keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
		} catch (NoSuchAlgorithmException n) {
			logger.error("Could not generate RSA Key Pair.", n);
			System.exit(1);
		}

		SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair).setCommonName(APP_NAME)
				.setOrganization(APP_ORG).setOrganizationalUnit(APP_UNIT).setLocalityName(APP_CITY)
				.setStateName(APP_STATE).setCountryCode(APP_COUNTRY).setApplicationUri(APP_URI);

		try {
			certificate = builder.build();
		} catch (Exception e) {
			logger.error("Could not build certificate.", e);
			System.exit(1);
		}
	}

	// log into the server and connect
	public synchronized void connect(OpcUaSource source)
			// String protocol, String host, int port, String path, String user,
			// String password, SecurityPolicy policy)
			throws Exception {
		String endpointUrl = source.getEndpointUrl();
		logger.info("Connecting to: " + endpointUrl);

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

			securityPolicy = source.getSecurityPolicy();
			messageSecurityMode = source.getMessageSecurityMode();

			// create the OpcUaClient
			EndpointDescription[] endpointDescriptions = UaTcpStackClient.getEndpoints(endpointUrl).get();

			logger.info("Available endpoints:");
			for (EndpointDescription endpointDescription : endpointDescriptions) {
				logger.info("URL: " + endpointDescription.getEndpointUrl() + ",  Policy: "
						+ endpointDescription.getSecurityPolicyUri() + ", Mode: "
						+ endpointDescription.getSecurityMode());
			}

			EndpointDescription endpointDescription = null;
			OpcUaClientConfig config = null;

			final boolean simpleConnect = false;
			final OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
			
			securityPolicy = SecurityPolicy.Basic256Sha256;
			messageSecurityMode = MessageSecurityMode.Sign;

			if (simpleConnect) {
				endpointDescription = endpointDescriptions[1];

				// just one endpoint
				cfg.setEndpoint(endpointDescriptions[0]);

				config = cfg.build();
			} else {
				/*
				 * endpoint = Arrays.stream(endpoints) .filter(e ->
				 * e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri())).
				 * findFirst() .orElseThrow(() -> new
				 * Exception("no desired endpoints returned"));
				 */

				/*
				 * endpoint = Arrays.stream(endpoints) .sorted((e1, e2) ->
				 * e2.getSecurityLevel().intValue() - e1.getSecurityLevel().intValue())
				 * .findFirst().orElseThrow(() -> new Exception("no endpoints returned"));
				 */


				// endpoint = chooseEndpoint(endpoints, securityPolicy, messageSecurityMode);
				for (EndpointDescription description : endpointDescriptions) {
					String uri = description.getSecurityPolicyUri();
					MessageSecurityMode mode = description.getSecurityMode();

					if (uri.equals(securityPolicy.getSecurityPolicyUri()) && mode.equals(messageSecurityMode)) {
						endpointDescription = description;
						break;
					}
				}

				if (endpointDescription == null) {
					String msg = "Unable to find a matching endpoint for security policy " + securityPolicy
							+ " and mode " + messageSecurityMode;
					logger.error(msg);
					throw new Exception(msg);
				}

				cfg.setApplicationName(LocalizedText.english(APP_NAME)).setApplicationUri(APP_URI)
						.setEndpoint(endpointDescription).setIdentityProvider(getIdentityProvider())
						.setRequestTimeout(uint(REQUEST_TIMEOUT));

				if (!securityPolicy.equals(SecurityPolicy.None)) {
					cfg.setCertificate(getClientCertificate()).setKeyPair(getKeyPair());
				}

				// logger.info("Using endpoint: " + endpoint.getEndpointUrl() + ", Security
				// Policy: " + securityPolicy);

				// keyStoreLoader.load();
				/*
				 * config = OpcUaClientConfig.builder()
				 * .setApplicationName(LocalizedText.english("Point85 OEE OPC UA Client"))
				 * .setApplicationUri(APP_URI)
				 * .setCertificate(keyStoreLoader.getClientCertificate())
				 * .setKeyPair(keyStoreLoader.getClientKeyPair()).setEndpoint(endpoint)
				 * .setIdentityProvider(identityProvider).setRequestTimeout(uint(REQUEST_TIMEOUT
				 * )).build();
				 */
			}
			logger.info("Using endpoint: {} [{}, {}]", endpointDescription.getEndpointUrl(),
					endpointDescription.getSecurityPolicyUri(), endpointDescription.getSecurityMode());

			config = cfg.build();

			opcUaClient = new OpcUaClient(config);

			// synchronous connect
			opcUaClient.connect().get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);
			
			if (logger.isInfoEnabled()) {
				logger.info("Connected to server.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to log in to server " + endpointUrl + ": " + e.getMessage());
		}
	}

	public synchronized void disconnect() throws Exception {
		if (opcUaClient != null) {
			try {
				opcUaClient.disconnect().get(REQUEST_TIMEOUT, REQUEST_TIMEOUT_UNIT);
				Stack.releaseSharedResources();
				// clientFuture.complete(opcUaClient);
				opcUaClient = null;
			} catch (Exception e) {
				String msg = e.getMessage();
				if (msg == null) {
					msg = e.getClass().getSimpleName();
				}
				throw new Exception("Error disconnecting: " + msg);
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

			// clientFuture.complete(opcUaClient);
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
			// clientFuture.complete(opcUaClient);
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
		// clientFuture.complete(opcUaClient);
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

		// clientFuture.complete(opcUaClient);
	}

	public synchronized void readAsynch(List<NodeId> nodeIds) throws Exception {
		checkPreconditions();

		CompletableFuture<List<DataValue>> cf = opcUaClient.readValues(MAX_AGE, TimestampsToReturn.Both, nodeIds);
		cf.thenAccept(values -> {
			for (OpcUaAsynchListener listener : asynchListeners) {
				listener.onOpcUaRead(values);
			}
		});

		// clientFuture.complete(opcUaClient);
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

			// clientFuture.complete(opcUaClient);
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
			// clientFuture.complete(opcUaClient);
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
			// clientFuture.complete(opcUaClient);
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

			// clientFuture.complete(opcUaClient);

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

			// clientFuture.complete(opcUaClient);

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

		// clientFuture.complete(opcUaClient);

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

			// clientFuture.complete(opcUaClient);

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

			// Get a typed reference to the ServerStatus variable
			// component and read value attributes individually
			ServerStatusNode serverStatusNode = serverNode.getServerStatusNode().get();

			DateTime startTime = serverStatusNode.getStartTime().get();
			ServerState state = serverStatusNode.getState().get();

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

	@Override
	public void onSessionActive(UaSession session) {
		logger.info("active session id: {}", session.getSessionId());
		BiConsumer<Boolean, Throwable> consumer = listener.get();
		if (consumer != null) {
			consumer.accept(Boolean.TRUE, null);
		}
	}

	@Override
	public void onSessionInactive(UaSession session) {
		logger.info("inactive session id: {}", session.getSessionId());
		BiConsumer<Boolean, Throwable> consumer = listener.get();
		if (consumer != null) {
			consumer.accept(Boolean.FALSE, null);
		}
	}
}
