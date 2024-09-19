package org.point85.domain.http;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class embeds a Jetty server for servicing HTTP requests
 * 
 *
 */
public class OeeHttpServer {
	// default HTTP port
	public static final int DEFAULT_PORT = 8182;

	// query string attributes
	public static final String EQUIP_ATTRIB = "equipment";
	public static final String DS_TYPE_ATTRIB = "sourceType";
	public static final String MATERIAL_ATTRIB = "material";
	public static final String EVENT_TYPE_ATTRIB = "type";
	public static final String FROM_ATTRIB = "from";
	public static final String TO_ATTRIB = "to";
	
	public static final String EVENT_KEY = "eventData";

	// root mapping
	public static final String ROOT_MAPPING = "/";

	// endpoints
	public static final String ENTITY_EP = "entity";
	public static final String REASON_EP = "reason";
	public static final String MATERIAL_EP = "material";
	public static final String EVENT_EP = "event";
	public static final String EVENTS_EP = "events";
	public static final String SOURCE_ID_EP = "source_id";
	public static final String DATA_SOURCE_EP = "data_source";
	public static final String STATUS_EP = "status";
	public static final String OEE_EP = "oee";

	// thread pool
	private static final int MAX_THREADS = 200;
	private static final int MIN_THREADS = 5;
	private static final int IDLE_TIMEOUT = 120;

	private static final String KEYSTORE_PASSWORD = "Point85";

	private static final String POINT85_KEYSTORE = "config/security/point85-keystore.jks";
	
	// REST URL
	public static final String REST_URL = "rest";
	
	// REST resources
	public static final String ENTITIES_RESOURCE = "entities";
	public static final String REASONS_RESOURCE = "reasons";
	public static final String MATERIALS_RESOURCE = "materials";
	public static final String EVENT_RESOURCE = "event";
	public static final String STATUS_RESOURCE = "status";

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeHttpServer.class);

	// wrapped Jetty server
	private Server jettyServer;

	// HTTP port
	private Integer httpPort;

	// HTTPS port
	private Integer httpsPort;

	// servlet
	private OeeHttpServlet servlet = new OeeHttpServlet();

	// state
	private ServerState state = ServerState.STOPPED;

	public OeeHttpServer(int httpPort) {
		this.httpPort = httpPort;
	}

	public Integer getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(Integer httpsPort) {
		this.httpsPort = httpsPort;
	}

	public ServerState getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getHost(), getListeningPort());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof OeeHttpServer)) {
			return false;
		}
		OeeHttpServer otherServer = (OeeHttpServer) other;

		return getHost().equals(otherServer.getHost()) && getListeningPort() == otherServer.getListeningPort();
	}

	public HttpEventListener getDataChangeListener() {
		return servlet.getDataChangeListener();
	}

	public static void setDataChangeListener(HttpEventListener dataChangeListener) {
		OeeHttpServlet.setDataChangeListener(dataChangeListener);
	}

	public boolean isAcceptingEventRequests() {
		return servlet.isAcceptingEventRequests();
	}

	public void setAcceptingEventRequests(boolean acceptingEventRequests) {
		OeeHttpServlet.setAcceptingEventRequests(acceptingEventRequests);
	}

	private void configureHTTPS(HttpConfiguration httpConfig) throws Exception {
		// HTTPS configuration
		final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

		// Point85 keystore
		File keystore = new File(POINT85_KEYSTORE);
		String keystorePath = keystore.getCanonicalPath();

		sslContextFactory.setKeyStorePath(keystorePath);
		sslContextFactory.setKeyStorePassword(KEYSTORE_PASSWORD);
		sslContextFactory.setKeyManagerPassword(KEYSTORE_PASSWORD);

		// SSL HTTP Configuration
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		SecureRequestCustomizer customizer = new SecureRequestCustomizer();
		customizer.setStsMaxAge(2000);
		customizer.setStsIncludeSubDomains(true);
		httpsConfig.addCustomizer(customizer);

		// SSL Connector
		ServerConnector sslConnector = new ServerConnector(jettyServer,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(httpsConfig));
		sslConnector.setPort(httpsPort);
		sslConnector.setIdleTimeout(500000);

		jettyServer.addConnector(sslConnector);

		if (logger.isInfoEnabled()) {
			logger.info("Added HTTPS connector, HTTPS keystore: " + keystorePath);
		}
	}

	/**
	 * Start the Jetty server
	 * 
	 * @throws Exception Exception
	 */
	public void startup() throws Exception {
		// servlet context
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");

		// create Jetty server in a queued thread pool
		QueuedThreadPool threadPool = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, IDLE_TIMEOUT);

		// server
		jettyServer = new Server(threadPool);

		// Extra options
		jettyServer.setDumpAfterStart(false);
		jettyServer.setDumpBeforeStop(false);
		jettyServer.setStopAtShutdown(true);

		// HTTP configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setOutputBufferSize(32768);
		httpConfig.setRequestHeaderSize(8192);
		httpConfig.setResponseHeaderSize(8192);
		httpConfig.setSendServerVersion(true);
		httpConfig.setSendDateHeader(false);

		ServerConnector httpConnection = new ServerConnector(jettyServer, new HttpConnectionFactory(httpConfig));
		httpConnection.setPort(httpPort);
		httpConnection.setIdleTimeout(30000);
		jettyServer.addConnector(httpConnection);

		if (httpsPort != null) {
			configureHTTPS(httpConfig);
		}

		// create servlet
		ServletHolder servletHolder = new ServletHolder(servlet);
		context.addServlet(servletHolder, ROOT_MAPPING);
		jettyServer.setHandler(context);

		// start Jetty server
		jettyServer.start();

		state = ServerState.STARTED;

		String baseUrl = "http://" + InetAddress.getLocalHost().getHostName() + ":" + httpPort + ROOT_MAPPING;

		if (logger.isInfoEnabled()) {
			logger.info("OPC HTTP server started at URL " + baseUrl + " and HTTPS port " + httpsPort);
		}
	}

	/**
	 * Stop the Jetty server
	 * 
	 * @throws Exception Exception
	 */
	public void shutdown() throws Exception {
		jettyServer.stop();
		state = ServerState.STOPPED;

		if (logger.isInfoEnabled()) {
			logger.info("OPC HTTP server stopped on port " + httpPort);
		}
	}

	public String getHost() {
		String host = null;

		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			host = "localhost";
		}
		return host;
	}

	public int getListeningPort() {
		return this.httpPort;
	}

	@Override
	public String toString() {
		return "Host: " + getHost() + ":" + getListeningPort();
	}
}
