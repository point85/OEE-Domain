package org.point85.domain.db;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import org.point85.domain.persistence.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to a database server with the event interface table and poll
 * for new records
 *
 */
public class DatabaseEventClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(DatabaseEventClient.class);

	// time between polling queries
	private static final int DEFAULT_POLLING_SEC = 60;

	// polling interval in sec
	private int pollingMillis = DEFAULT_POLLING_SEC;

	// polling timer
	private Timer pollingTimer;

	// polling task
	private PollingTask pollingTask;

	// service handling the queried data
	private DatabaseEventListener eventListener;

	// persistence service
	private PersistenceService persistenceService;

	// JDBC connection URL
	private String jdbcUrl;

	// the source id of interest
	private String sourceId;

	public DatabaseEventClient(DatabaseEventListener eventListener, int pollingSeconds) {
		this.eventListener = eventListener;
		this.pollingMillis = pollingSeconds;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void connectToServer(String jdbcUrl, String userName, String password) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting with JDBC URL " + jdbcUrl + " with user " + userName);
		}

		persistenceService = PersistenceService.create();
		persistenceService.connectToDatabaseEventServer(jdbcUrl, userName, password);
		this.jdbcUrl = jdbcUrl;
	}

	public void disconnect() {
		if (logger.isInfoEnabled()) {
			logger.info("Disconnecting from database");
		}

		pollingTimer.cancel();

		if (persistenceService != null) {
			persistenceService.close();
		}
	}

	public void startPolling() {
		if (logger.isInfoEnabled()) {
			logger.info("Starting to poll for events every " + pollingMillis + " msec.");
		}

		startPollingTimer();
	}

	public void stopPolling() {
		stopPollingTimer();

		if (logger.isInfoEnabled()) {
			logger.info("Stopped polling for events");
		}
	}

	private void initializePollingTimer() {
		// create timer and task
		pollingTimer = new Timer();
		pollingTask = new PollingTask();
	}

	private void startPollingTimer() {
		if (pollingTimer == null) {
			initializePollingTimer();
		}
		pollingTimer.schedule(pollingTask, 1000, pollingMillis);
	}

	private void stopPollingTimer() {
		pollingTimer.cancel();
		pollingTimer = null;
	}

	private void onPoll() {
		if (logger.isInfoEnabled()) {
			String msg = "Querying for READY events";

			if (getSourceId() != null) {
				msg += " for sourcee " + getSourceId();
			}
			logger.info(msg);
		}

		// query database interface table for new records
		List<DatabaseEvent> events = null;

		if (getSourceId() != null) {
			events = persistenceService.fetchDatabaseEvents(DatabaseEventStatus.READY, getSourceId());
		} else {
			events = persistenceService.fetchDatabaseEvents(DatabaseEventStatus.READY);
		}
		eventListener.resolveDatabaseEvents(this, events);
	}

	private class PollingTask extends TimerTask {
		@Override
		public void run() {
			onPoll();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(jdbcUrl);
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof DatabaseEventClient)) {
			return false;
		}
		DatabaseEventClient otherClient = (DatabaseEventClient) other;

		return jdbcUrl.equals(otherClient.jdbcUrl);
	}

	public synchronized DatabaseEvent save(DatabaseEvent event) throws Exception {
		return (DatabaseEvent) persistenceService.save(event);
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
}
