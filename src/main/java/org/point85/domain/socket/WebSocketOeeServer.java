package org.point85.domain.socket;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.SSLParametersWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.point85.domain.messaging.ApplicationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A web socket server for OEE messages
 */
public class WebSocketOeeServer extends WebSocketServer {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(WebSocketOeeServer.class);

	// connection lost timeout
	private static final int CONN_LOST_TO_SEC = 30;

	// SSL keystore
	private static final String STORETYPE = "JKS";
	private static final String KEY_ALG = "SunX509";
	private static final String SSL_PROTOCOL = "TLS";

	// listener to call back when a message is received
	private WebSocketMessageListener eventListener;

	// started flag
	private boolean isStarted = false;

	// source
	private WebSocketSource source;

	/**
	 * Constructor from web socket source definition
	 * 
	 * @param source {@link WebSocketSource}
	 * @throws Exception Exception
	 */
	public WebSocketOeeServer(WebSocketSource source) throws Exception {
		super(new InetSocketAddress(source.getHost(), source.getPort()));

		if (source.getKeystore() != null && !source.getKeystore().isEmpty()) {
			setSSLSocketFactory(source);
		}
		this.source = source;
	}

	/**
	 * Add an equipment event listener
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

	private String getSocketInfo(WebSocket socket) {
		String info = "";

		if (socket != null && socket.getLocalSocketAddress() != null) {
			InetAddress address = socket.getLocalSocketAddress().getAddress();
			info = "host: " + address.getHostName() + ", address: " + address.getHostAddress();
		}
		return info;
	}

	/**
	 * Called when socket is opened
	 * 
	 * @param socket    {@link WebSocket}
	 * @param handshake {@link ClientHandshake}
	 */
	@Override
	public void onOpen(WebSocket socket, ClientHandshake handshake) {
		if (logger.isInfoEnabled()) {
			logger.info("onOpen(): Client opened connection on socket " + getSocketInfo(socket) + ", handshake: "
					+ handshake.getResourceDescriptor());
		}
	}

	/**
	 * Callback when socket is closed
	 * 
	 * @param socket {@link WebSocket}
	 * @param code   Close code
	 * @param reason Reason for close. The codes are documented in class
	 *               org.java_websocket.framing.CloseFrame.
	 * @param remote true if remote
	 */
	@Override
	public void onClose(WebSocket socket, int code, String reason, boolean remote) {
		if (logger.isInfoEnabled()) {
			logger.info("onClose(): " + getSocketInfo(socket) + " has disconnected.  Code = " + code + ", reason: "
					+ reason);
		}
	}

	/**
	 * Callback when a message is received
	 * 
	 * @param socket  {@link WebSocket}
	 * @param message Serialized {@link ApplicationMessage}
	 */
	@Override
	public void onMessage(WebSocket socket, String message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Message received from " + getSocketInfo(socket) + "\n" + message);
		}

		try {
			// call the listener
			ApplicationMessage request = WebSocketUtils.deserialize(message);

			if (eventListener != null) {
				eventListener.onWebSocketMessage(request);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Callback on an error
	 * 
	 * @param socket {@link WebSocket}
	 * @param e      Exception
	 */
	@Override
	public void onError(WebSocket socket, Exception e) {
		String msg = e.getMessage();

		if (socket != null) {
			msg = getSocketInfo(socket) + msg;
		}
		logger.error(msg);
	}

	/**
	 * Callback when server started
	 */
	@Override
	public void onStart() {
		setConnectionLostTimeout(CONN_LOST_TO_SEC);

		if (logger.isInfoEnabled()) {
			logger.info("onStart(): Web socket server started on " + source.getHost() + ":" + source.getPort()
					+ ", connection time out: " + CONN_LOST_TO_SEC + " sec.");
		}
		isStarted = true;
	}

	/**
	 * Is this server started?
	 * 
	 * @return true if started, else false
	 */
	public boolean isStarted() {
		return isStarted;
	}

	/**
	 * Stop this server
	 * 
	 * @throws Exception Exception
	 */
	public void shutdown() throws Exception {
		stop();
		isStarted = false;
	}

	/**
	 * Start this server
	 */
	public void startup() {
		start();
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

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_ALG);
		kmf.init(ks, keyPassword.toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(KEY_ALG);
		tmf.init(ks);

		SSLContext sslContext = null;
		sslContext = SSLContext.getInstance(SSL_PROTOCOL);
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		if (source.isClientAuthorization()) {
			// two-way SSL
			SSLParameters sslParameters = new SSLParameters();
			sslParameters.setNeedClientAuth(true);
			setWebSocketFactory(new SSLParametersWebSocketServerFactory(sslContext, sslParameters));
		} else {
			setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
		}
	}
}
