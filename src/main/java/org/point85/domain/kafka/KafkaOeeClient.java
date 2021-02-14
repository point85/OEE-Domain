package org.point85.domain.kafka;

import java.io.File;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.point85.domain.DomainUtils;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KafkaOeeClient class has methods for communicating with a Kafka server.
 *
 */
public class KafkaOeeClient extends BaseMessagingClient {
	// logger
	private Logger logger = LoggerFactory.getLogger(KafkaOeeClient.class);

	// default polling interval
	public static final int DEFAULT_POLLING_INTERVAL = 2500;

	// timeout for server ack on a send
	private static final int SEND_TIMEOUT_SEC = 10;

	public static final String EVENT_TOPIC = "Point85_Event";
	public static final String NOTIFICATION_TOPIC = "Point85_Notification";

	// producer and consumer properties
	private Properties producerProperties = new Properties();
	private Properties consumerProperties = new Properties();

	// topic names
	private String producerTopic = NOTIFICATION_TOPIC;
	private Collection<String> consumerTopics = new HashSet<>();

	// producer
	private Producer<String, String> producer;

	// producer source
	private KafkaSource producerServer;

	// consumer
	private Consumer<String, String> consumer;

	// consumer source
	private KafkaSource consumerServer;

	// listeners for received messages
	private KafkaMessageListener listener;

	// polling interval in milliseconds
	private int pollingInterval = DEFAULT_POLLING_INTERVAL;

	/**
	 * Create a Kafka consumer for this server and topic
	 * 
	 * @param server {@link KafkaSource}
	 * @param topic  Kafka topic
	 * @throws Exception Exception
	 */
	public void createConsumer(KafkaSource server, String topic) throws Exception {
		this.consumerServer = server;

		setDefaultConsumerProperties();
		setConsumerTopic(topic);

		if (server.getTruststore() != null && !server.getTruststore().isEmpty()) {
			setSSLConfiguration(buildCanonicalPath(server.getTruststore()), server.getTruststorePassword(), true);

			if (logger.isInfoEnabled()) {
				logger.info("Configured consumer for SSL with truststore " + server.getTruststore());
			}
		}

		if (server.getKeystore() != null && !server.getKeystore().isEmpty()) {
			setMutualSSLConfiguration(buildCanonicalPath(server.getKeystore()), server.getKeystorePassword(),
					server.getKeyPassword(), true);

			if (logger.isInfoEnabled()) {
				logger.info("Configured consumer for mutual SSL with keystore " + server.getKeystore());
			}
		}

		if (server.getUserName() != null && !server.getUserName().isEmpty()) {
			setSASLConfiguration(server.getUserName(), server.getUserPassword(), true);

			if (logger.isInfoEnabled()) {
				logger.info("Configured consumer for SASL for user " + server.getUserName());
			}
		}

		consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, server.getHost() + ":" + server.getPort());

		// consumer
		if (logger.isInfoEnabled()) {
			logger.info("Connecting as a consumer to server at "
					+ consumerProperties.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
		}
		consumer = new KafkaConsumer<>(consumerProperties);

		// validate the connection
		ConnectionValidator validator = new ConnectionValidator();

		try {
			validator.checkConnection(true).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new Exception(DomainLocalizer.instance().getErrorString("kafka.unable.to.connect",
					consumerProperties.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)));
		}
	}

	/**
	 * Create a Kafka producer for this server and topic
	 * 
	 * @param server {@link KafkaSource}
	 * @param topic  Topic
	 * @throws Exception Exception
	 */
	public void createProducer(KafkaSource server, String topic) throws Exception {
		this.producerServer = server;

		setDefaultProducerProperties();
		setProducerTopic(topic);

		if (server.getTruststore() != null && !server.getTruststore().isEmpty()) {
			setSSLConfiguration(buildCanonicalPath(server.getTruststore()), server.getTruststorePassword(), false);

			if (logger.isInfoEnabled()) {
				logger.info("Configured producer for SSL with truststore " + server.getTruststore());
			}
		}

		if (server.getKeystore() != null && !server.getKeystore().isEmpty()) {
			setMutualSSLConfiguration(buildCanonicalPath(server.getKeystore()), server.getKeystorePassword(),
					server.getKeyPassword(), false);

			if (logger.isInfoEnabled()) {
				logger.info("Configured producer for mutual SSL with keystore " + server.getKeystore());
			}
		}

		if (server.getUserName() != null && !server.getUserName().isEmpty()) {
			setSASLConfiguration(server.getUserName(), server.getUserPassword(), false);

			if (logger.isInfoEnabled()) {
				logger.info("Configured consumer for SASL for user " + server.getUserName());
			}
		}

		// server URLs
		producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server.getHost() + ":" + server.getPort());

		// producer
		if (logger.isInfoEnabled()) {
			logger.info("Connecting as a producer to server(s) at "
					+ producerProperties.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
		}
		producer = new KafkaProducer<>(producerProperties);

		// validate the connection
		ConnectionValidator validator = new ConnectionValidator();

		try {
			validator.checkConnection(false).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new Exception(DomainLocalizer.instance().getErrorString("kafka.unable.to.connect",
					producerProperties.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)));
		}
	}

	private String buildCanonicalPath(String storeName) throws Exception {
		String storePath = DomainUtils.SECURITY_DIR + storeName;
		File storeFile = new File(storePath);

		if (!storeFile.exists()) {
			throw new Exception(DomainLocalizer.instance().getErrorString("kafka.no.store", storePath));
		}
		return storeFile.getCanonicalPath();
	}

	/**
	 * Register a listener for messages
	 * 
	 * @param listener {@link KafkaMessageListener}
	 */
	public void registerListener(KafkaMessageListener listener) {
		this.listener = listener;
	}

	/**
	 * Unregister a previous message listener
	 */
	public void unregisterListener() {
		this.listener = null;
	}

	private void setDefaultConsumerProperties() {
		consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "Point85_OEE");
		consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		consumerProperties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30000);
	}

	private void setDefaultProducerProperties() {
		producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
	}

	/**
	 * Set the client's security configuration to use SSL
	 * 
	 * @param clientTruststore   Client's Java truststore name
	 * @param truststorePassword Truststore password
	 * @param forConsumer        If true, this is the consumer's configuration
	 */
	public void setSSLConfiguration(String clientTruststore, String truststorePassword, boolean forConsumer) {
		Properties properties = forConsumer ? consumerProperties : producerProperties;

		properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
		properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
		properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientTruststore);
		properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
	}

	/**
	 * Set the client's security configuration to use SASL SSL and SCRAM-SHA-512
	 * 
	 * @param userName    Principal's name
	 * @param password    Principal's password
	 * @param forConsumer If true, this is the consumer's configuration
	 */
	public void setSASLConfiguration(String userName, String password, boolean forConsumer) {
		Properties properties = forConsumer ? consumerProperties : producerProperties;

		final String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required"
				+ " username=\"%s\" password=\"%s\";";
		final String jaasCfg = String.format(jaasTemplate, userName, password);

		properties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
		properties.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
		properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
	}

	/**
	 * Set the client's security configuration to use two-way SSL/TLS
	 * 
	 * @param clientKeystore   Client's keystore name
	 * @param keystorePassword Keystore's password
	 * @param keyPassword      Private key's password
	 * @param forConsumer      If true, this is the consumer's configuration
	 */
	public void setMutualSSLConfiguration(String clientKeystore, String keystorePassword, String keyPassword,
			boolean forConsumer) {
		Properties properties = forConsumer ? consumerProperties : producerProperties;

		properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, clientKeystore);
		properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
		properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
	}

	public String getProducerTopic() {
		return this.producerTopic;
	}

	public void setProducerTopic(String topic) {
		this.producerTopic = topic;
	}

	private void sendMessage(ApplicationMessage message) throws Exception {
		if (producer == null) {
			return;
		}

		String key = message.getMessageType().toString();

		// send the message synchronously
		Future<RecordMetadata> future = producer.send(new ProducerRecord<>(producerTopic, key, serialize(message)));

		// wait for response
		RecordMetadata data = future.get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);

		if (logger.isInfoEnabled()) {
			logger.info("Sent " + message.getMessageType() + " message to topic " + producerTopic + " with key " + key
					+ " to partition " + data.partition() + ", offset " + data.offset());
		}
	}

	/**
	 * Start polling to consume notification messages
	 */
	public void consumeNotifications() {
		if (!consumerTopics.contains(NOTIFICATION_TOPIC)) {
			consumerTopics.add(NOTIFICATION_TOPIC);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Consuming notification messages");
		}

		startPolling();
	}

	/**
	 * Start polling to consume messages
	 */
	public void startPolling() {
		Thread poller = new Thread() {
			@Override
			public void run() {
				try {
					if (consumerTopics.isEmpty()) {
						throw new Exception(DomainLocalizer.instance().getErrorString("kafka.no.consumer.topics"));
					}

					// subscribe to topics
					consumer.subscribe(consumerTopics);

					// print the topic name
					consumerTopics.forEach(topic -> logger
							.info("Subscribed to topic " + topic + ", polling at " + pollingInterval + " msec"));

					// polling loop
					while (true) {
						// collection of records
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollingInterval));

						records.forEach(record -> {
							if (listener != null) {
								// serialize to ApplicationMessage
								String json = record.value();

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
								} else if (json.contains(MessageType.STATUS.name())) {
									// command
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
								listener.onKafkaMessage(appMessage);
							}
						});

						// commits the records assuming that the listeners are
						// successful in handling the messages
						consumer.commitAsync();
					}
				} catch (WakeupException e) {
					// expected
				} catch (Exception e) {
					logger.error("Unexpected error", e);
				} finally {
					try {
						if (consumer != null) {
							consumer.commitSync();
						}
					} finally {
						if (consumer != null) {
							consumer.close();
						}

						if (logger.isInfoEnabled()) {
							logger.info("Closed consumer");
						}
					}
				}
			}
		};
		poller.start();
	}

	/**
	 * Stop polling for messages
	 */
	public void stopPolling() {
		consumer.wakeup();
		logger.info("Stopped polling");
	}

	/**
	 * Stop polling for messages and close the producer
	 */
	public void disconnect() {
		if (producer != null) {
			producer.close();
		}
		producer = null;

		if (consumer != null) {
			stopPolling();
		}
		consumer = null;
	}

	private void setConsumerTopic(String consumerTopic) {
		this.consumerTopics.clear();
		this.consumerTopics.add(consumerTopic);
	}

	public String getProducerProperty(String key) {
		return producerProperties.getProperty(key);
	}

	public void setProducerProperty(String key, String value) {
		producerProperties.put(key, value);
	}

	public String getConsumerProperty(String key) {
		return consumerProperties.getProperty(key);
	}

	public void setConsumerProperty(String key, String value) {
		consumerProperties.put(key, value);
	}

	/**
	 * Send a notification message
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
			sendNotificationMessage(message);
		} catch (Exception e) {
			logger.error("Unable to publish notification.", e);
		}
	}

	/**
	 * Send an event application message to the event topic
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendEventMessage(ApplicationMessage message) throws Exception {
		if (producerTopic.equals(EVENT_TOPIC)) {
			sendMessage(message);
		}
	}

	/**
	 * Send a notification application message to the notification topic
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendNotificationMessage(ApplicationMessage message) throws Exception {
		if (producerTopic.equals(NOTIFICATION_TOPIC)) {
			sendMessage(message);
		}
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(int pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	public KafkaSource getProducerServer() {
		return producerServer;
	}

	public KafkaSource getConsumerServer() {
		return consumerServer;
	}

	private class ConnectionValidator {
		private ExecutorService executor = Executors.newSingleThreadExecutor();

		private Future<Boolean> checkConnection(boolean asConsumer) {
			return executor.submit(() -> {
				boolean value = false;

				try {
					if (asConsumer) {
						if (consumerTopics.isEmpty()) {
							logger.error("No consumer topics have been defined.");
						}
						Object[] topics = consumerTopics.toArray();

						if (logger.isInfoEnabled()) {
							logger.info("Checking consumer topic " + topics[0]);
						}

						value = consumer.partitionsFor((String) topics[0]).isEmpty();
					} else {
						if (logger.isInfoEnabled()) {
							logger.info("Checking producer topic " + producerTopic);
						}

						value = producer.partitionsFor(producerTopic).isEmpty();
					}
				} finally {
					if (consumer != null) {
						consumer.close();
					}

					if (producer != null) {
						producer.close();
					}
				}
				return value;
			});
		}
	}
}
