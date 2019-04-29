package org.point85.domain.jms;

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
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to a JMS broker, publish and consume messages
 *
 */
public class JMSClient extends BaseMessagingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(JMSClient.class);

	// message priorities
	public static final int LOW_PRIORITY = 2;
	public static final int MEDIUM_PRIORITY = 4;
	public static final int HIGH_PRIORITY = 8;

	// JMS objects
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Queue consumerQueue;

	// name of the message queue
	public static final String DEFAULT_QUEUE = "Point85";
	public static final String DEFAULT_TOPIC = "Point85";

	// listener to call back when a message is received
	private JMSEquipmentEventListener eventListener;

	public JMSClient() {

	}

	public void registerListener(JMSEquipmentEventListener listener) {
		this.eventListener = listener;
	}

	public void unregisterListener() {
		this.eventListener = null;
	}

	public void startUp(String brokerHostName, int port, String userName, String password,
			JMSEquipmentEventListener listener) throws Exception {
		// connect to broker
		connect(brokerHostName, port, userName, password);

		// add listener
		registerListener(listener);

		// subscribe to messages
		consume();
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
			logger.info("Connecting to AMQ broker host " + brokerHostName + " on port " + port + ", user " + userName);
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

	private void consume() throws JMSException {
		consumerQueue = session.createQueue(DEFAULT_QUEUE);

		// consumer
		MessageConsumer consumer = session.createConsumer(consumerQueue);

		// listener for received messages
		MessageListener listener = new MessageListener() {
			public void onMessage(Message message) {
				if (message instanceof TextMessage) {
					String json;
					try {
						json = ((TextMessage) message).getText();

						if (logger.isInfoEnabled()) {
							logger.info("Received message: \n" + json);
						}

						if (!json.contains(MessageType.EQUIPMENT_EVENT.name())) {
							logger.error("Unable to handle message!");
							return;
						}

						// equipment event
						EquipmentEventMessage appMessage = (EquipmentEventMessage) deserialize(
								MessageType.EQUIPMENT_EVENT, json);

						if (eventListener != null) {
							eventListener.onJMSEquipmentEvent(appMessage);
						}
					} catch (JMSException e) {
						logger.error(e.getMessage());
						return;
					}
				} else {
					logger.error("Received unknown message " + message.getClass().getSimpleName());
				}
			}
		};

		consumer.setMessageListener(listener);

		if (logger.isInfoEnabled()) {
			logger.info("Consuming from queue " + DEFAULT_QUEUE);
		}
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

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof JMSClient)) {
			return false;
		}
		JMSClient otherClient = (JMSClient) other;

		return connectionFactory.getBrokerURL().equals(otherClient.connectionFactory.getBrokerURL());
	}

	@Override
	public String toString() {
		return connectionFactory != null ? connectionFactory.getBrokerURL() : "";
	}
}
