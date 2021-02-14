package org.point85.domain.email;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.BaseMessagingClient;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.MessageType;
import org.point85.domain.messaging.NotificationSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EmailClient class has methods for communicating with an email server. It
 * supports both IMAP and POP3 protocols for receiving messages.
 *
 */
public class EmailClient extends BaseMessagingClient {
	private static final String MAIL_DEBUG = "mail.debug";
	private static final String MAIL_TRUE = "true";
	private static final String MAIL_FALSE = "false";
	private static final String TEXT_PLAIN = "text/plain";
	private static final String MAIL_INBOX = "INBOX";

	private static final String IMAP_STORE = "imap";
	private static final String POP3_STORE = "pop3";

	// SMTP
	private static final String MAIL_SMTP_HOST = "mail.smtp.host";
	private static final String MAIL_SMTP_PORT = "mail.smtp.port";
	private static final String MAIL_SMTP_USER = "mail.smtp.user";
	private static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";
	private static final String MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
	private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	private static final String MAIL_SMTP_START_TLS = "mail.smtp.starttls.enable";

	// IMAP
	private static final String MAIL_IMAP_HOST = "mail.imap.host";
	private static final String MAIL_IMAP_PORT = "mail.imap.port";
	private static final String MAIL_IMAP_USER = "mail.imap.user";
	private static final String MAIL_IMAP_PASSWORD = "mail.imap.password";
	private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";

	// POP3
	private static final String MAIL_POP3_HOST = "mail.pop3.host";
	private static final String MAIL_POP3_PORT = "mail.pop3.port";
	private static final String MAIL_POP3_USER = "mail.pop3.user";
	private static final String MAIL_POP3_PASSWORD = "mail.pop3.password";
	private static final String MAIL_POP3_SSL_ENABLE = "mail.pop3.ssl.enable";

	// logger
	private static final Logger logger = LoggerFactory.getLogger(EmailClient.class);

	// default polling interval
	public static final int DEFAULT_POLLING_INTERVAL = 30000;

	// polling interval in milliseconds
	private int pollingInterval = DEFAULT_POLLING_INTERVAL;

	// SMTP, IMAP and POP3 properties
	private Properties smtpProperties = new Properties();
	private Properties imapProperties = new Properties();
	private Properties pop3Properties = new Properties();

	// listener for received messages
	private EmailMessageListener listener;

	private String storeType;

	// polling timer
	private Timer pollingTimer;

	// polling flag
	private boolean isPolling = false;

	// email server
	private EmailSource source;

	/**
	 * Constructor from an email server source
	 * 
	 * @param source {@link EmailSource}
	 */
	public EmailClient(EmailSource source) {
		if (source != null) {
			configureSMTP(source);
			configureIMAP(source);
			configurePOP3(source);
		}
		this.source = source;
	}

	public boolean isSubscribed() {
		return isPolling;
	}

	public EmailSource getSource() {
		return this.source;
	}

	/**
	 * Check for incoming emails with JSON serialized ApplicationMessage content
	 * 
	 * @return List of {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public List<ApplicationMessage> receiveEmails() throws Exception {
		Properties properties = null;
		String user = null;
		String password = null;
		List<ApplicationMessage> appMessages = new ArrayList<>();

		if (storeType.equals(IMAP_STORE)) {
			properties = imapProperties;
			user = imapProperties.getProperty(MAIL_IMAP_USER);
			password = imapProperties.getProperty(MAIL_IMAP_PASSWORD);
		} else {
			properties = pop3Properties;
			user = pop3Properties.getProperty(MAIL_POP3_USER);
			password = pop3Properties.getProperty(MAIL_POP3_PASSWORD);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Checking for mail for user " + user);
		}

		// create session and store
		Session emailSession = Session.getInstance(properties);
		Store emailStore = emailSession.getStore(storeType);

		try {
			// connect to the server
			emailStore.connect(user, password);
		} catch (Exception e) {
			emailStore.close();
			throw e;
		}

		// create the inbox and open it
		Folder emailFolder = emailStore.getFolder(MAIL_INBOX);
		emailFolder.open(Folder.READ_WRITE);

		// retrieve the messages from the inbox
		Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

		for (int i = 0; i < messages.length; i++) {
			Message message = messages[i];

			if (logger.isInfoEnabled()) {
				logger.info("---------------------------------");
				logger.info("Email Number " + (i + 1));
				logger.info("Subject: " + message.getSubject());
				logger.info("From: " + (message.getFrom() != null ? message.getFrom()[0] : "No From Field"));
			}

			Object content = message.getContent();
			String json = null;

			if (content instanceof MimeMultipart) {
				MimeMultipart mime = (MimeMultipart) content;

				int count = mime.getCount();

				for (int k = 0; k < count; k++) {
					BodyPart part = mime.getBodyPart(k);
					String ct = part.getContentType().toLowerCase();

					if (ct.contains(TEXT_PLAIN)) {
						json = (String) part.getContent();
						break;
					}
				}
			} else if (content instanceof String) {
				json = (String) content;
			}

			if (logger.isInfoEnabled()) {
				logger.info("Email content: \n" + json);
			}

			message.setFlag(Flags.Flag.SEEN, true);

			// serialize to ApplicationMessage
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
			}

			if (appMessage != null) {
				appMessages.add(appMessage);
			}
		}

		// close the store and folder objects
		emailFolder.close(false);
		emailStore.close();

		return appMessages;
	}

	private void configureSMTP(EmailSource source) {
		if (logger.isDebugEnabled()) {
			smtpProperties.put(MAIL_DEBUG, MAIL_TRUE);
		}

		smtpProperties.put(MAIL_SMTP_HOST, source.getSendHost());
		smtpProperties.put(MAIL_SMTP_PORT, source.getSendPort());
		smtpProperties.put(MAIL_SMTP_SSL_ENABLE,
				source.getSendSecurityPolicy().equals(EmailSecurityPolicy.SSL) ? MAIL_TRUE : MAIL_FALSE);

		if (source.getUserName() != null) {
			smtpProperties.put(MAIL_SMTP_USER, source.getUserName());
			smtpProperties.put(MAIL_SMTP_PASSWORD, source.getUserPassword());
			smtpProperties.put(MAIL_SMTP_AUTH, MAIL_TRUE);
		} else {
			smtpProperties.put(MAIL_SMTP_AUTH, MAIL_FALSE);
		}

		smtpProperties.put(MAIL_SMTP_START_TLS, MAIL_TRUE);

		if (logger.isInfoEnabled()) {
			logger.info("SMTP properties:");

			for (Entry<Object, Object> property : smtpProperties.entrySet()) {
				if (!((String) property.getKey()).contains("password")) {
					logger.info(property.getKey() + ": " + property.getValue());
				} else {
					logger.info(property.getKey() + ": <not null>");
				}
			}
		}
	}

	private void configureIMAP(EmailSource source) {
		if (!source.getProtocol().equals(EmailProtocol.IMAP)) {
			return;
		}
		storeType = IMAP_STORE;

		if (logger.isDebugEnabled()) {
			imapProperties.put(MAIL_DEBUG, MAIL_TRUE);
		}

		imapProperties.put(MAIL_IMAP_HOST, source.getReceiveHost());
		imapProperties.put(MAIL_IMAP_PORT, source.getReceivePort());
		imapProperties.put(MAIL_IMAP_SSL_ENABLE,
				source.getReceiveSecurityPolicy().equals(EmailSecurityPolicy.SSL) ? MAIL_TRUE : MAIL_FALSE);

		if (source.getUserName() != null) {
			imapProperties.put(MAIL_IMAP_USER, source.getUserName());
			imapProperties.put(MAIL_IMAP_PASSWORD, source.getUserPassword());
		}

		if (logger.isInfoEnabled()) {
			logger.info("IMAP properties:");

			for (Entry<Object, Object> property : imapProperties.entrySet()) {
				if (!((String) property.getKey()).contains("password")) {
					logger.info(property.getKey() + ": " + property.getValue());
				} else {
					logger.info(property.getKey() + ": <not null>");
				}
			}
		}
	}

	private void configurePOP3(EmailSource source) {
		if (!source.getProtocol().equals(EmailProtocol.POP3)) {
			return;
		}
		storeType = POP3_STORE;

		if (logger.isDebugEnabled()) {
			imapProperties.put(MAIL_DEBUG, MAIL_TRUE);
		}

		pop3Properties.put(MAIL_POP3_HOST, source.getReceiveHost());
		pop3Properties.put(MAIL_POP3_PORT, source.getReceivePort());
		pop3Properties.put(MAIL_POP3_SSL_ENABLE,
				source.getReceiveSecurityPolicy().equals(EmailSecurityPolicy.SSL) ? MAIL_TRUE : MAIL_FALSE);

		if (source.getUserName() != null) {
			pop3Properties.put(MAIL_POP3_USER, source.getUserName());
			pop3Properties.put(MAIL_POP3_PASSWORD, source.getUserPassword());
		}

		if (logger.isInfoEnabled()) {
			logger.info("POP3 properties:");

			for (Entry<Object, Object> property : pop3Properties.entrySet()) {
				if (!((String) property.getKey()).contains("password")) {
					logger.info(property.getKey() + ": " + property.getValue());
				} else {
					logger.info(property.getKey() + ": <not null>");
				}
			}
		}
	}

	/**
	 * Send a MimeMessage email with the specified content
	 * 
	 * @param to      Recipient
	 * @param subject Subject
	 * @param content Content
	 * @throws Exception Exception
	 */
	public void sendMail(String to, String subject, String content) throws Exception {
		if (smtpProperties.isEmpty()) {
			logger.warn("SMTP properties are not defined.");
			return;
		}

		// create the session
		Session session = Session.getInstance(smtpProperties, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpProperties.getProperty(MAIL_SMTP_USER),
						smtpProperties.getProperty(MAIL_SMTP_PASSWORD));
			}
		});

		// create the message
		MimeMessage message = new MimeMessage(session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(subject);
		message.setText(content);
		message.setFrom(smtpProperties.getProperty(MAIL_SMTP_USER));
		message.setContent(content, TEXT_PLAIN);

		// send message
		Transport.send(message);

		if (logger.isInfoEnabled()) {
			logger.info("Sent message to " + to);
		}
	}

	/**
	 * Register a listener for messages
	 * 
	 * @param listener {@link EmailMessageListener}
	 */
	public void registerListener(EmailMessageListener listener) {
		this.listener = listener;
	}

	/**
	 * Unregister a previous message listener
	 */
	public void unregisterListener() {
		this.listener = null;
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(int pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Stop checking for emails
	 */
	public void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer = null;
		}
		isPolling = false;
	}

	/**
	 * Start checking for emails
	 */
	public void startPolling() {
		// delay up to 5 sec
		long delay = (long) (Math.random() * 5000.0d);

		pollingTimer = new Timer();
		pollingTimer.schedule(new PollingTask(), delay, pollingInterval);
		isPolling = true;
	}

	/**
	 * Send a notification message
	 * 
	 * @param to       Recipient
	 * @param subject  Subject
	 * @param text     Content
	 * @param severity {@link NotificationSeverity}
	 * @throws Exception Exception
	 */
	public void sendNotification(String to, String subject, String text, NotificationSeverity severity)
			throws Exception {
		InetAddress address = InetAddress.getLocalHost();

		CollectorNotificationMessage message = new CollectorNotificationMessage(address.getHostName(),
				address.getHostAddress());
		message.setText(text);
		message.setSeverity(severity);

		sendMail(to, subject, serialize(message));
	}

	/**
	 * Send an event application message
	 * 
	 * @param to      Recipient
	 * @param subject Subject
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendEvent(String to, String subject, ApplicationMessage message) throws Exception {
		sendMail(to, subject, serialize(message));
	}

	private class PollingTask extends TimerTask {
		private PollingTask() {
		}

		@Override
		public void run() {
			try {
				onPoll();
			} catch (Exception e) {
				logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}

		private void onPoll() throws Exception {
			List<ApplicationMessage> messages = receiveEmails();

			if (listener != null) {
				for (ApplicationMessage message : messages) {
					listener.onEmailMessage(message);
				}
			}
		}
	}
}
