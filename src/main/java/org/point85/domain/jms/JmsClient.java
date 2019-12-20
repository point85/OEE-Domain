package org.point85.domain.jms;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Random;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to a JMS broker, publish and consume messages
 *
 */
public class JmsClient extends BaseMessagingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(JmsClient.class);

	// message priorities
	public static final int LOW_PRIORITY = 2;
	public static final int MEDIUM_PRIORITY = 4;
	public static final int HIGH_PRIORITY = 8;

	// JMS objects
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Queue consumerQueue;
	private Topic consumerTopic;

	// name of the message queue and topic
	public static final String EVENT_QUEUE = "Point85";
	private static final String EVENT_TOPIC = "Point85";
	private static final String STATUS_TOPIC = "Point85_Status";
	private static final String STATUS_QUEUE = "Point85_Status";

	private static final int TTL_SEC = 3600;

	// listener to call back when a message is received
	private JmsMessageListener eventListener;

	public JmsClient() {

	}

	public void registerListener(JmsMessageListener listener) {
		this.eventListener = listener;
	}

	public void unregisterListener() {
		this.eventListener = null;
	}

	public void startUp(String brokerHostName, int port, String userName, String password,
			JmsMessageListener listener) throws Exception {
		// connect to broker
		connect(brokerHostName, port, userName, password);

		// add listener
		registerListener(listener);
	}

	/**
	 * Connect to the JMS broker
	 * 
	 * @param brokerHostName Host name
	 * @param port           Host port
	 * @param userName       User name
	 * @param password       User password
	 * @throws Exception Exception
	 */
	public void connect(String brokerHostName, int port, String userName, String password) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting to JMS broker host " + brokerHostName + " on port " + port + ", user " + userName);
		}

		connectionFactory = new ActiveMQConnectionFactory("tcp://" + brokerHostName + ":" + port);
		connection = connectionFactory.createConnection(userName, password);
		connection.start();

		// non-transacted, auto-ack session
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		if (logger.isInfoEnabled()) {
			logger.info("Connected to broker " + connectionFactory.getBrokerURL());
		}
	}

	/**
	 * Disconnect from the JMS broker
	 * 
	 * @throws Exception Exception
	 */
	public void disconnect() throws Exception {
		unregisterListener();

		if (connection != null) {
			connection.close();
		}

		if (session != null) {
			session.close();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Disconnected from " + connectionFactory.getBrokerURL());
		}
	}

	public void consumeEvents(boolean fromTopic) throws JMSException {
		MessageConsumer consumer = null;

		if (fromTopic) {
			consumerTopic = session.createTopic(EVENT_TOPIC);

			// consumer
			consumer = session.createConsumer(consumerTopic);

			if (logger.isInfoEnabled()) {
				logger.info("Consuming from topic " + EVENT_TOPIC);
			}
		} else {
			// queue
			consumerQueue = session.createQueue(EVENT_QUEUE);

			// consumer
			consumer = session.createConsumer(consumerQueue);

			if (logger.isInfoEnabled()) {
				logger.info("Consuming from queue " + EVENT_QUEUE);
			}
		}

		// listener for received messages
		MessageListener listener = new MessageListener() {
			public void onMessage(Message message) {
				if (!(message instanceof TextMessage)) {
					logger.error("Received unknown message " + message.getClass().getSimpleName());
					return;
				}

				String json = null;
				try {
					json = ((TextMessage) message).getText();

					if (logger.isInfoEnabled()) {
						logger.info("Received message: \n" + json);
					}

					ApplicationMessage appMessage = null;

					if (json.contains(MessageType.EQUIPMENT_EVENT.name())) {
						// equipment event
						appMessage = deserialize(MessageType.EQUIPMENT_EVENT, json);
					} else if (json.contains(MessageType.COMMAND.name())) {
						// command
						appMessage = deserialize(MessageType.COMMAND, json);
					} else {
						logger.error("Unable to handle message!");
						return;
					}

					if (eventListener != null) {
						eventListener.onJmsMessage(appMessage);
					}

				} catch (JMSException e) {
					logger.error(e.getMessage());
					return;
				}
			}
		};

		consumer.setMessageListener(listener);
	}

	public void consumeNotifications(boolean fromTopic) throws JMSException {
		MessageConsumer consumer = null;

		if (fromTopic) {
			consumerTopic = session.createTopic(STATUS_TOPIC);

			// consumer
			consumer = session.createConsumer(consumerTopic);

			if (logger.isInfoEnabled()) {
				logger.info("Consuming from topic " + STATUS_TOPIC);
			}
		} else {
			// queue
			consumerQueue = session.createQueue(STATUS_QUEUE);

			// consumer
			consumer = session.createConsumer(consumerQueue);

			if (logger.isInfoEnabled()) {
				logger.info("Consuming from queue " + STATUS_QUEUE);
			}
		}

		// listener for received messages
		MessageListener listener = new MessageListener() {
			public void onMessage(Message message) {
				if (!(message instanceof TextMessage)) {
					logger.error("Received unknown message " + message.getClass().getSimpleName());
					return;
				}

				String json;
				try {
					json = ((TextMessage) message).getText();

					if (logger.isInfoEnabled()) {
						logger.info("Received message: \n" + json);
					}

					ApplicationMessage appMessage = null;

					if (json.contains(MessageType.STATUS.name())) {
						// status event
						appMessage = deserialize(MessageType.STATUS, json);
					} else if (json.contains(MessageType.NOTIFICATION.name())) {
						// command
						appMessage = deserialize(MessageType.NOTIFICATION, json);
					} else if (json.contains(MessageType.RESOLVED_EVENT.name())) {
						// command
						appMessage = deserialize(MessageType.RESOLVED_EVENT, json);
					} else {
						logger.error("Unable to handle message!");
						return;
					}

					if (eventListener != null) {
						eventListener.onJmsMessage(appMessage);
					}

				} catch (JMSException e) {
					logger.error(e.getMessage());
					return;
				}
			}
		};

		consumer.setMessageListener(listener);
	}

	public void sendToTopic(ApplicationMessage message, String topicName, int ttlSec) throws Exception {
		// validate
		message.validate();

		Topic topic = session.createTopic(topicName);

		MessageProducer producer = session.createProducer(topic);
		send(producer, message, ttlSec);
	}

	public void sendToQueue(ApplicationMessage message, String queueName, int ttlSec) throws Exception {
		// validate
		message.validate();

		Queue queue = session.createQueue(queueName);

		MessageProducer producer = session.createProducer(queue);
		send(producer, message, ttlSec);
	}

	private void send(MessageProducer producer, ApplicationMessage message, int ttlSec) throws JMSException {
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

		TextMessage textMessage = session.createTextMessage();
		textMessage.setText(serialize(message));
		textMessage.setJMSCorrelationID(createCorrelationId());

		producer.send(textMessage, DeliveryMode.NON_PERSISTENT, HIGH_PRIORITY, (long) (ttlSec * 1000));

		if (logger.isInfoEnabled()) {
			logger.info("Sent text message of type " + message.getMessageType());
		}
	}

	private String createCorrelationId() {
		Random random = new Random(System.currentTimeMillis());
		return Long.toHexString(random.nextLong());
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionFactory.getBrokerURL());
	}

	/**
	 * Send a collector notification
	 * 
	 * @param text     Message content
	 * @param severity {@link NotificationSeverity}
	 * @throws Exception exception
	 */
	public void sendNotification(String text, NotificationSeverity severity) throws Exception {
		InetAddress address = InetAddress.getLocalHost();

		CollectorNotificationMessage message = new CollectorNotificationMessage(address.getHostName(),
				address.getHostAddress());
		message.setText(text);
		message.setSeverity(severity);

		try {
			sendToTopic(message, STATUS_TOPIC, TTL_SEC);
		} catch (Exception e) {
			logger.error("Unable to publish notification.", e);
		}
	}

	public void sendNotificationMessage(ApplicationMessage message) {
		try {
			sendToTopic(message, STATUS_TOPIC, TTL_SEC);
		} catch (Exception e) {
			logger.error("Unable to publish notification.", e);
		}
	}

	public void sendEventMessage(ApplicationMessage message) {
		try {
			sendToTopic(message, EVENT_TOPIC, TTL_SEC);
		} catch (Exception e) {
			logger.error("Unable to publish notification.", e);
		}
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof JmsClient)) {
			return false;
		}
		JmsClient otherClient = (JmsClient) other;

		return connectionFactory.getBrokerURL().equals(otherClient.connectionFactory.getBrokerURL());
	}

	@Override
	public String toString() {
		return connectionFactory != null ? connectionFactory.getBrokerURL() : "";
	}
}
