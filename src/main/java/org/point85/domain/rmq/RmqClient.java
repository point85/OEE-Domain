package org.point85.domain.rmq;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Class to connect to RMQ, publish and subscribe to message
 *
 */
public class RmqClient extends BaseMessagingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(RmqClient.class);

	// auto-ack on consume
	private static final boolean AUTO_ACK = true;

	// use a topic exchange
	private static final String EXCHANGE_TYPE = "topic";

	private static final String EXCHANGE_NAME = "Point85";

	// durable exchange
	private static final boolean DURABLE_EXCHANGE = true;

	// message TTL
	private static final int TTL_SEC = 3600;

	private String bindingKey;

	// RMQ objects
	private ConnectionFactory factory;
	private Connection connection;
	protected Channel channel;
	private String consumerTag;

	// for blocking (RPC) style calls
	private String replyQueueName;

	// listener to call back when message received
	private RmqMessageListener listener;

	public RmqClient() {
	}

	public String getBindingKey() {
		return bindingKey;
	}

	public void setBindingKey(String bindingKey) {
		this.bindingKey = bindingKey;
	}

	public Channel getChannel() {
		return this.channel;
	}

	public void registerListener(RmqMessageListener listener) {
		this.listener = listener;
	}

	public void unregisterListener(RmqMessageListener listener) {
		this.listener = null;
	}

	public void startUp(String hostName, int port, String userName, String password, String queueName,
			List<RoutingKey> routingKeys, RmqMessageListener listener) throws Exception {
		// connect to broker
		connect(hostName, port, userName, password);

		// add listener
		registerListener(listener);

		// subscribe to messages
		subscribe(queueName, routingKeys);
	}

	/**
	 * Connect to the RMQ broker
	 * 
	 * @param hostName Host name or IP address
	 * @param port     Host port
	 * @param userName User name
	 * @param password User password
	 * @throws Exception Exception
	 */
	public void connect(String hostName, int port, String userName, String password) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting to RMQ broker host " + hostName + " on port " + port + ", user " + userName
					+ ", exchange " + EXCHANGE_NAME);
		}

		// factory
		factory = new ConnectionFactory();
		factory.setHost(hostName);
		factory.setPort(port);
		factory.setUsername(userName);
		factory.setPassword(password);

		// connection
		connection = factory.newConnection();

		// channel
		channel = connection.createChannel();

		// durable exchange
		channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, DURABLE_EXCHANGE);

		if (logger.isInfoEnabled()) {
			logger.info("Connected to broker host " + hostName + " on port " + port + ", exchange " + EXCHANGE_NAME);
		}
	}

	/**
	 * Disconnect from the RMQ broker
	 * 
	 * @throws Exception Exception
	 */
	public void disconnect() throws Exception {
		if (channel != null) {
			if (consumerTag != null) {
				try {
					channel.basicCancel(consumerTag);
				} catch (Exception e) {
				}
			}

			if (channel.isOpen()) {
				try {
					channel.close();
				} catch (Exception e) {
				}
			}
			channel = null;
		}

		if (connection != null) {
			if (connection.isOpen()) {
				connection.close();
			}
			connection = null;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Disconnected from " + factory.getHost() + " on port " + factory.getPort());
		}
	}

	/**
	 * Send an RMQ message of type notification to subscribers.
	 * 
	 * @param text     Text of message.
	 * @param severity {@link NotificationSeverity}
	 * @throws Exception Exception
	 */
	public void sendNotification(String text, NotificationSeverity severity) throws Exception {
		final int STATUS_TTL_SEC = 3600;

		InetAddress address = InetAddress.getLocalHost();

		CollectorNotificationMessage message = new CollectorNotificationMessage(address.getHostName(),
				address.getHostAddress());
		message.setText(text);
		message.setSeverity(severity);

		try {
			publish(message, RoutingKey.NOTIFICATION_MESSAGE, STATUS_TTL_SEC);
		} catch (Exception e) {
			logger.error("Unable to publish notification.", e);
		}
	}

	private void sendMessage(ApplicationMessage message, String routingKey, BasicProperties properties)
			throws Exception {
		if (channel == null) {
			logger.error("Can't send message: " + message + ".  The channel is null.");
			return;
		}

		// payload is JSON string
		String payload = serialize(message);

		// publish with this routing key
		channel.basicPublish(EXCHANGE_NAME, routingKey, properties, payload.getBytes());
	}

	public void publish(ApplicationMessage message, RoutingKey routingKey, int ttlSec) throws Exception {
		// validate
		message.validate();

		// use type field to ID the message
		BasicProperties properties = new BasicProperties.Builder().type(message.getMessageType().toString())
				.correlationId(UUID.randomUUID().toString()).build();

		// TTL in msec
		properties.builder().expiration(String.valueOf(ttlSec * 1000));

		// send the message with these properties
		sendMessage(message, routingKey.getKey(), properties);
	}

	private void subscribe(String queueName, List<RoutingKey> routingKeys) throws Exception {
		// TTL
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("x-message-ttl", QUEUE_TTL_SEC * 1000);

		// not durable, non-exclusive queue, autodelete with TTL
		channel.queueDeclare(queueName, false, false, true, args);

		for (RoutingKey routingKey : routingKeys) {
			// bind to key
			channel.queueBind(queueName, EXCHANGE_NAME, routingKey.getKey());
		}

		// create a message receiver
		Receiver consumer = new Receiver();

		// start consuming messages
		consumerTag = channel.basicConsume(queueName, AUTO_ACK, consumer);

		if (logger.isInfoEnabled()) {
			String keys = "";
			for (RoutingKey routingKey : routingKeys) {
				if (keys.length() > 0) {
					keys += ", ";
				}
				keys += routingKey;
			}
			logger.info("Subscribed to queue " + queueName + " with routing key(s) " + keys);
		}
	}

	public void createRpcQueue() throws IOException {
		replyQueueName = channel.queueDeclare().getQueue();

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
						.correlationId(properties.getCorrelationId()).build();

				String response = new String(body, "UTF-8");

				channel.basicPublish(EXCHANGE_NAME, properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));

				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};

		channel.basicConsume(replyQueueName, true, consumer);
	}

	// RPC-style blocking call. Publish message, then wait for response.
	public ApplicationMessage call(ApplicationMessage message, String routingKey) throws Exception {
		ApplicationMessage replyMessage = null;
		String correlationId = UUID.randomUUID().toString();

		// use type field to ID the message, reply on the reply queue
		BasicProperties properties = new BasicProperties.Builder().type(message.getMessageType().toString())
				.correlationId(correlationId).replyTo(replyQueueName).build();

		// send the message with these properties
		sendMessage(message, routingKey, properties);

		// wait for response
		final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);

		channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				if (properties.getCorrelationId().equals(correlationId)) {
					// put body into the blocking queue
					response.offer(new String(body, "UTF-8"));
				}
			}
		});

		// take the body off of the blocking queue
		String body = response.take();

		// message type
		MessageType type = MessageType.fromString(properties.getType());
		replyMessage = deserialize(type, body);

		return replyMessage;
	}

	/**
	 * Send the notification message
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendNotificationMessage(ApplicationMessage message) throws Exception {
		publish(message, RoutingKey.NOTIFICATION_MESSAGE, TTL_SEC);
	}

	/**
	 * Send the command message
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendCommandMessage(ApplicationMessage message) throws Exception {
		publish(message, RoutingKey.COMMAND_MESSAGE, TTL_SEC);
	}

	/**
	 * Send the event message
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendEquipmentEventMessage(ApplicationMessage message) throws Exception {
		publish(message, RoutingKey.EQUIPMENT_SOURCE_EVENT, TTL_SEC);
	}

	public void sendResolvedEventMessage(ApplicationMessage message) throws Exception {
		publish(message, RoutingKey.RESOLVED_EVENT, TTL_SEC);
	}

	@Override
	public int hashCode() {
		return Objects.hash(factory.getHost(), factory.getPort());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof RmqClient)) {
			return false;
		}
		RmqClient otherPubSub = (RmqClient) other;

		return factory.getHost().equals(otherPubSub.factory.getHost())
				&& factory.getPort() == otherPubSub.factory.getPort();
	}

	@Override
	public String toString() {
		return factory != null ? factory.getHost() + ":" + factory.getPort() : "";
	}

	// ************************* Message Receiver ***************************
	private class Receiver extends DefaultConsumer {
		Receiver() {
			super(channel);
		}

		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
				throws java.io.IOException {

			// message type
			MessageType type = MessageType.fromString(properties.getType());

			String payload = new String(body);
			ApplicationMessage message = deserialize(type, payload);

			if (logger.isInfoEnabled()) {
				logger.info("Received message of type " + type + " from sender " + message.getSenderHostName() + " ("
						+ message.getSenderHostAddress() + ")" + " payload: \n" + payload);
			}

			if (listener != null) {
				try {
					listener.onRmqMessage(message);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
}
