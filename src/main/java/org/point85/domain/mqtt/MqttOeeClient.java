package org.point85.domain.mqtt;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.point85.domain.DomainUtils;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttOeeClient extends BaseMessagingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(MqttOeeClient.class);

	// equipment message topic
	private static final String EVENT_TOPIC = "Point85";

	// status topic
	private static final String STATUS_TOPIC = "Point85_Status";

	// true = non durable subscriptions
	private static final boolean CLEAN_SESSION = true;

	// temporary directory for in flight messages
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

	// protocol
	private static final String TCP_PROTOCOL = "tcp://";
	private static final String SSL_PROTOCOL = "ssl://";

	// native client
	private MqttClient mqttClient;

	// listener to call back when a message is received
	private MqttMessageListener eventListener;

	// connection options
	private MqttConnectOptions connectionOptions = new MqttConnectOptions();

	public MqttOeeClient() {
		connectionOptions.setAutomaticReconnect(true);
		connectionOptions.setCleanSession(CLEAN_SESSION);
		connectionOptions.setConnectionTimeout(10);
	}

	public void registerListener(MqttMessageListener listener) {
		this.eventListener = listener;
	}

	public void unregisterListener() {
		this.eventListener = null;
	}

	public void startUp(String hostName, int port, MqttMessageListener listener) throws Exception {
		// connect to server
		connect(hostName, port);

		// add listener
		registerListener(listener);

		if (logger.isInfoEnabled()) {
			logger.info("Started up MQTT client");
		}
	}

	/**
	 * Connect to the MQTT server
	 * 
	 * @param hostName Host name
	 * @param port     Host port
	 * @param userName User name
	 * @param password User password
	 * @throws Exception Exception
	 */
	public void connect(String hostName, int port) throws Exception {
		String protocol = connectionOptions.getSocketFactory() == null ? TCP_PROTOCOL : SSL_PROTOCOL;
		String url = protocol + hostName + ":" + port;

		if (logger.isInfoEnabled()) {
			logger.info("Connecting to MQTT server " + url);

			if (connectionOptions.getUserName() != null) {
				logger.info("   as user " + connectionOptions.getUserName());
			}
		}

		// persistent data store (closed when client disconnects)
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(TEMP_DIR);

		// create client
		mqttClient = new MqttClient(url, MqttClient.generateClientId(), dataStore);

		// connect to server
		mqttClient.connect(connectionOptions);

		setHostName(hostName);
		setHostPort(port);

		if (logger.isInfoEnabled()) {
			logger.info("Connected to MQTT server " + url);
		}
	}

	public void subscribeToEvents(QualityOfService qos) throws Exception {
		mqttClient.subscribe(EVENT_TOPIC, (topic, msg) -> {
			String json = new String(msg.getPayload());

			if (logger.isInfoEnabled()) {
				logger.info("MQTT event message received, topic: " + topic + ", JSON:\n\t" + json);
			}

			ApplicationMessage appMessage = null;

			if (json.contains(MessageType.EQUIPMENT_EVENT.name())) {
				// equipment event
				appMessage = deserialize(MessageType.EQUIPMENT_EVENT, json);
			} else if (json.contains(MessageType.COMMAND.name())) {
				// command
				appMessage = deserialize(MessageType.COMMAND, json);
			} else {
				throw new Exception(DomainLocalizer.instance().getErrorString("bad.message") + "\n\t" + json);
			}

			if (eventListener != null) {
				eventListener.onMqttMessage(appMessage);
			}
		});

		if (logger.isInfoEnabled()) {
			logger.info("Subscribed to topic " + EVENT_TOPIC + " at QoS " + qos);
		}
	}

	public void subscribeToNotifications(QualityOfService qos) throws Exception {
		mqttClient.subscribe(STATUS_TOPIC, (topic, msg) -> {
			String json = new String(msg.getPayload());

			if (logger.isInfoEnabled()) {
				logger.info("MQTT notification message received, topic: " + topic + ", JSON:\n\t" + json);
			}

			ApplicationMessage appMessage = null;

			if (json.contains(MessageType.STATUS.name())) {
				// status
				appMessage = deserialize(MessageType.STATUS, json);
			} else if (json.contains(MessageType.NOTIFICATION.name())) {
				// notification
				appMessage = deserialize(MessageType.NOTIFICATION, json);
			} else if (json.contains(MessageType.RESOLVED_EVENT.name())) {
				// resolved event
				appMessage = deserialize(MessageType.RESOLVED_EVENT, json);
			} else {
				throw new Exception(DomainLocalizer.instance().getErrorString("bad.message") + "\n\t" + json);
			}

			if (eventListener != null) {
				eventListener.onMqttMessage(appMessage);
			}
		});

		if (logger.isInfoEnabled()) {
			logger.info("Subscribed to topic " + STATUS_TOPIC + " at QoS " + qos);
		}
	}

	public void publish(String topic, ApplicationMessage message, QualityOfService qos) throws Exception {
		String text = serialize(message);

		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setQos(qos.getQos());
		mqttMessage.setRetained(false);

		mqttMessage.setPayload(text.getBytes());
		mqttClient.publish(topic, mqttMessage);

		if (logger.isInfoEnabled()) {
			logger.info("Message published to topic " + topic + ".  QoS: " + qos + "\n\t" + text);
		}
	}

	/**
	 * Disconnect from the MQTT server
	 * 
	 * @throws Exception Exception
	 */
	public void disconnect() throws Exception {
		mqttClient.unsubscribe(EVENT_TOPIC);
		mqttClient.disconnect();
		mqttClient.close(true);

		if (logger.isInfoEnabled()) {
			logger.info("Shut down MQTT client");
		}
	}

	/**
	 * Send a collector notification message to the status topic
	 * 
	 * @param text     Message content
	 * @param severity {@link NotificationSeverity}
	 * @throws Exception Exception
	 */
	public void sendNotification(String text, NotificationSeverity severity) throws Exception {
		InetAddress address = InetAddress.getLocalHost();

		CollectorNotificationMessage message = new CollectorNotificationMessage(address.getHostName(),
				address.getHostAddress());
		message.setText(text);
		message.setSeverity(severity);

		publish(STATUS_TOPIC, message, QualityOfService.EXACTLY_ONCE);
	}

	/**
	 * Send a collector notification message to the status topic
	 * 
	 * @param message {@link ApplicationMessage} Notification message
	 * @throws Exception Exception
	 */
	public void sendNotificationMessage(ApplicationMessage message) throws Exception {
		publish(STATUS_TOPIC, message, QualityOfService.EXACTLY_ONCE);
	}

	/**
	 * Send an event message to the event topic
	 * 
	 * @param message {@link ApplicationMessage} Event message
	 * @throws Exception Exception
	 */
	public void sendEventMessage(ApplicationMessage message) throws Exception {
		publish(EVENT_TOPIC, message, QualityOfService.EXACTLY_ONCE);
	}

	public String getServerURI() {
		return mqttClient != null ? mqttClient.getServerURI() : "";
	}

	@Override
	public String toString() {
		return mqttClient != null ? mqttClient.getServerURI() : "";
	}

	/**
	 * Set the client's security configuration to use two-way SSL/TLS
	 * 
	 * @param clientKeystore   Client's keystore name
	 * @param keystorePassword Keystore's password
	 * @param keyPassword      Private key's password
	 * @param forConsumer      If true, this is the consumer's configuration
	 */
	public void setSSLConfiguration(String clientKeystore, String keystorePassword, String keyPassword)
			throws Exception {
		if (clientKeystore == null || clientKeystore.isEmpty()) {
			return;
		}

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		// truststore
		File truststoreFile = new File(DomainUtils.SECURITY_DIR + clientKeystore);
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(new FileInputStream(truststoreFile), keystorePassword.toCharArray());
		tmf.init(trustStore);

		// keystore
		File keystoreFile = new File(DomainUtils.SECURITY_DIR + clientKeystore);
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, keyPassword.toCharArray());

		KeyManager[] km = kmf.getKeyManagers();
		TrustManager[] tm = tmf.getTrustManagers();
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(km, tm, null);

		connectionOptions.setSocketFactory(sslContext.getSocketFactory());

		if (logger.isInfoEnabled()) {
			logger.info("Truststore: " + truststoreFile.getCanonicalPath());
			logger.info("Keystore: " + keystoreFile.getCanonicalPath());
		}
	}

	/**
	 * Set the client's security configuration to use SASL SSL and SCRAM-SHA-512
	 * 
	 * @param userName    Principal's name
	 * @param password    Principal's password
	 * @param forConsumer If true, this is the consumer's configuration
	 */
	public void setAuthenticationConfiguration(String userName, String password) {
		if (userName != null && userName.trim().length() > 0) {
			connectionOptions.setUserName(userName);
			connectionOptions.setPassword(password.toCharArray());
		}
	}
}
