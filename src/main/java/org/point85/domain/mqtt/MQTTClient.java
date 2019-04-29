package org.point85.domain.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTClient extends BaseMessagingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(MQTTClient.class);

	// message topic
	private static final String POINT85_TOPIC = "Point85";

	// true = non durable subscriptions
	private static final boolean CLEAN_SESSION = true;

	// temporary directory for in flight messages
	private static String TEMP_DIR = System.getProperty("java.io.tmpdir");

	// protocol
	private static final String TCP_PROTOCOL = "tcp://";

	// native client
	private MqttClient mqttClient;

	// listener to call back when a message is received
	private MQTTEquipmentEventListener eventListener;

	public MQTTClient() {
	}

	public void registerListener(MQTTEquipmentEventListener listener) {
		this.eventListener = listener;
	}

	public void unregisterListener() {
		this.eventListener = null;
	}

	public void startUp(String hostName, int port, String userName, String password,
			MQTTEquipmentEventListener listener) throws Exception {
		// connect to server
		connect(hostName, port, userName, password);

		// add listener
		registerListener(listener);

		// subscribe with max QoS= 0
		subscribe(QualityOfService.AT_MOST_ONCE);

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
	public void connect(String hostName, int port, String userName, String password) throws Exception {
		// use TCP protocol
		String url = TCP_PROTOCOL + hostName + ":" + port;

		if (logger.isInfoEnabled()) {
			logger.info("Connecting to MQTT server " + url + " with user " + userName);
		}

		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(TEMP_DIR);

		// create client
		mqttClient = new MqttClient(url, MqttClient.generateClientId(), dataStore);

		// connection options
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(CLEAN_SESSION);
		options.setConnectionTimeout(10);

		if (userName != null && userName.trim().length() > 0) {
			options.setUserName(userName);
			options.setPassword(password.toCharArray());
		}

		// connect to server
		mqttClient.connect(options);

		if (logger.isInfoEnabled()) {
			logger.info("Connected to MQTT server " + url + " with user " + userName);
		}
	}

	public void subscribe(QualityOfService qos) throws Exception {
		mqttClient.subscribe(POINT85_TOPIC, (topic, msg) -> {
			String json = new String(msg.getPayload());

			if (logger.isInfoEnabled()) {
				logger.info("MQTT message received, topic: " + topic + ", JSON:\n\t" + json);
			}

			if (!json.contains(MessageType.EQUIPMENT_EVENT.name())) {
				throw new Exception(DomainLocalizer.instance().getErrorString("bad.message") + "\n\t" + json);
			}

			// equipment event
			EquipmentEventMessage appMessage = (EquipmentEventMessage) deserialize(MessageType.EQUIPMENT_EVENT, json);

			if (eventListener != null) {
				eventListener.onMQTTEquipmentEvent(appMessage);
			}
		});

		if (logger.isInfoEnabled()) {
			logger.info("Subscribed to topic " + POINT85_TOPIC + " at QoS " + qos);
		}
	}

	public void publish(ApplicationMessage message, QualityOfService qos) throws Exception {
		String text = serialize(message);

		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setQos(qos.getQos());
		mqttMessage.setRetained(false);

		mqttMessage.setPayload(text.getBytes());
		mqttClient.publish(POINT85_TOPIC, mqttMessage);

		if (logger.isInfoEnabled()) {
			logger.info("Message published.  QoS: " + qos + "\n\t" + text);
		}
	}

	/**
	 * Disconnect from the MQTT server
	 * 
	 * @throws Exception Exception
	 */
	public void disconnect() throws Exception {
		mqttClient.unsubscribe(POINT85_TOPIC);
		mqttClient.disconnect();
		mqttClient.close(true);

		if (logger.isInfoEnabled()) {
			logger.info("Shut down MQTT client");
		}
	}
}
