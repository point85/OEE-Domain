package org.point85.domain.socket;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.messaging.ApplicationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A web socket client for OEE messages
 */
public class WebSocketOeeClient extends WebSocketClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(WebSocketOeeClient.class);

	// connection timeout
	private static final int CONNECT_TIMEOUT = 10;

	// SSL keystore
	private static final String STORETYPE = "JKS";
	private static final String KEY_ALG = "SunX509";
	private static final String SSL_PROTOCOL = "TLS";

	// listener to call back when a message is received
	private WebSocketMessageListener eventListener;

	// web socket source definition
	private WebSocketSource source = null;

	/**
	 * Constructor from a web socket source definition
	 * 
	 * @param source {@link WebSocketSource}
	 * @throws Exception Exception
	 */
	public WebSocketOeeClient(WebSocketSource source) throws Exception {
		super(buildURI(source));
		this.source = source;
	}

	private static URI buildURI(WebSocketSource source) throws Exception {
		if (source == null) {
			return null;
		}

		String protocol = source.getKeystore() == null || source.getKeystore().isEmpty() ? "ws" : "wss";
		String url = protocol + "://" + source.getHost() + ":" + source.getPort();

		if (logger.isInfoEnabled()) {
			logger.info("Client URL: " + url);
		}
		return new URI(url);
	}

	/**
	 * Connect to the web socket server
	 * 
	 * @return true if succeeded
	 * @throws Exception Exception
	 */
	public boolean openConnection() throws Exception {
		String msg = null;
		if (source == null || source.getKeystore() == null || source.getKeystore().isEmpty()) {
			msg = "Connected to server";
		} else {
			msg = "Connected to server with keystore " + source.getKeystore();
			setSSLSocketFactory(source);
		}
		if (logger.isInfoEnabled()) {
			logger.info(msg);
		}
		return connectBlocking(CONNECT_TIMEOUT, TimeUnit.SECONDS);
	}

	/**
	 * Close the connection to the web socket server
	 * 
	 * @throws Exception Exception
	 */
	public void closeConnection() throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Closing connection to server");
		}
		closeBlocking();
	}

	/**
	 * Callback when the connection is opened
	 * 
	 * @param handshakedata {@link ServerHandshake}
	 */
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		if (logger.isInfoEnabled()) {
			logger.info("onOpen(): " + handshakedata.getHttpStatus());
		}
	}

	/**
	 * Callback when the message is received
	 * 
	 * @param message Serialized {@link ApplicationMessage}
	 */
	@Override
	public void onMessage(String message) {
		if (logger.isInfoEnabled()) {
			logger.info("received message: " + message);
		}

		// call the listener
		try {
			if (eventListener != null) {
				ApplicationMessage request = WebSocketUtils.deserialize(message);
				eventListener.onWebSocketMessage(request);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Callback when socket is closed
	 * 
	 * @param code   Close code
	 * @param reason Reason for close. The codes are documented in class
	 *               org.java_websocket.framing.CloseFrame.
	 * @param remote true if remote
	 */
	@Override
	public void onClose(int code, String reason, boolean remote) {
		//
		if (logger.isInfoEnabled()) {
			logger.info("onClose():  Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code
					+ " Reason: " + reason);
		}
	}

	/**
	 * Callback on an error
	 * 
	 * @param e Exception
	 */
	@Override
	public void onError(Exception e) {
		logger.error("onError(): " + e.getMessage());
	}

	/**
	 * Add a listener for messages from the server
	 * 
	 * @param listener {@link WebSocketMessageListener}
	 */
	public void registerListener(WebSocketMessageListener listener) {
		this.eventListener = listener;
	}

	/**
	 * Remove the event listener
	 */
	public void unregisterListener() {
		this.eventListener = null;
	}

	private void validate(ApplicationMessage message) throws Exception {
		if (message.getMessageType() == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("null.type"));
		}
	}

	/**
	 * Send an application message
	 * 
	 * @param message {@link ApplicationMessage}
	 * @throws Exception Exception
	 */
	public void sendEventMessage(ApplicationMessage message) throws Exception {
		// validate message
		validate(message);

		// send message
		String json = WebSocketUtils.serialize(message);
		send(json);
	}

	private void setSSLSocketFactory(WebSocketSource source) throws Exception {
		// load up the key store
		String keystore = "config/security/" + source.getKeystore();
		String keyStorePassword = source.getKeystorePassword();
		String keyPassword = source.getKeyPassword();

		KeyStore ks = KeyStore.getInstance(STORETYPE);
		File kf = new File(keystore);
		ks.load(new FileInputStream(kf), keyStorePassword.toCharArray());

		if (logger.isInfoEnabled()) {
			logger.info("Loaded keystore " + keystore);
		}

		// set the socket factory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_ALG);
		kmf.init(ks, keyPassword.toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_ALG);
		tmf.init(ks);

		SSLContext sslContext = null;
		sslContext = SSLContext.getInstance(SSL_PROTOCOL);
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		SSLSocketFactory factory = sslContext.getSocketFactory();
		setSocketFactory(factory);
	}
}
