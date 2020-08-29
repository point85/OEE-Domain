package org.point85.domain.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class embeds a Jetty server for servicing HTTP requests
 * 
 *
 */
public class OeeHttpServer {
	// default port
	public static final int DEFAULT_PORT = 8182;

	// query string attributes
	public static final String EQUIP_ATTRIB = "equipment";
	public static final String DS_TYPE_ATTRIB = "sourceType";
	public static final String MATERIAL_ATTRIB = "material";
	public static final String EVENT_KEY = "eventData";

	// root mapping
	public static final String ROOT_MAPPING = "/";

	// endpoints
	public static final String ENTITY_EP = "entity";
	public static final String REASON_EP = "reason";
	public static final String MATERIAL_EP = "material";
	public static final String EVENT_EP = "event";
	public static final String SOURCE_ID_EP = "source_id";
	public static final String DATA_SOURCE_EP = "data_source";
	public static final String STATUS_EP = "status";

	// thread pool
	private static final int MAX_THREADS = 100;
	private static final int MIN_THREADS = 5;
	private static final int IDLE_TIMEOUT = 120;

	// logger
	private static final Logger logger = LoggerFactory.getLogger(OeeHttpServer.class);

	// wrapped Jetty server
	private Server jettyServer;

	// port
	private int listeningPort;

	// servlet
	private OeeHttpServlet servlet = new OeeHttpServlet();

	// state
	private ServerState state = ServerState.STOPPED;

	public OeeHttpServer(int port) {
		listeningPort = port;
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

	/**
	 * Start the Jetty server
	 * 
	 * @throws Exception Exception
	 */
	public void startup() throws Exception {
		// servlet context
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");

		// create Jetty server
		QueuedThreadPool threadPool = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, IDLE_TIMEOUT);

		// server
		jettyServer = new Server(threadPool);

		// connector
		try (ServerConnector connector = new ServerConnector(jettyServer)) {
			connector.setPort(listeningPort);
			jettyServer.setConnectors(new Connector[] { connector });
		}

		// create servlet
		ServletHolder servletHolder = new ServletHolder(servlet);
		context.addServlet(servletHolder, ROOT_MAPPING);
		jettyServer.setHandler(context);

		// start Jetty server
		jettyServer.start();

		state = ServerState.STARTED;

		String baseUrl = "http://" + InetAddress.getLocalHost().getHostName() + ":" + listeningPort + ROOT_MAPPING;

		if (logger.isInfoEnabled()) {
			logger.info("OPC HTTP server started at URL " + baseUrl);
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
			logger.info("OPC HTTP server stopped on port " + listeningPort);
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
		return this.listeningPort;
	}

	@Override
	public String toString() {
		return "Host: " + getHost() + ":" + getListeningPort();
	}
}
