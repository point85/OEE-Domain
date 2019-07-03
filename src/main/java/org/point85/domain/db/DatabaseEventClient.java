package org.point85.domain.db;

import java.util.List;
import java.util.Objects;

import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.polling.PollingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to a database server with the event interface table and poll
 * for new records for the specified source id.
 *
 */
public class DatabaseEventClient extends PollingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(DatabaseEventClient.class);

	// service handling the queried data
	private DatabaseEventListener eventListener;

	// persistence service
	private PersistenceService persistenceService;

	// JDBC connection URL
	private String jdbcUrl;
	
	public DatabaseEventClient() {
		super();
	}

	public DatabaseEventClient(DatabaseEventListener eventListener, DatabaseEventSource eventSource,
			List<String> sourceIds, List<Integer> pollingPeriods) {
		super(eventSource, sourceIds, pollingPeriods);
		this.eventListener = eventListener;
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

		cancelPolling();

		if (persistenceService != null) {
			persistenceService.close();
		}
	}

	@Override
	protected void onPoll(String sourceId) {
		if (logger.isInfoEnabled()) {
			logger.info("Querying for READY events for source " + sourceId);
		}

		// query database interface table for new records
		List<DatabaseEvent> events = persistenceService.fetchDatabaseEvents(DatabaseEventStatus.READY, sourceId);
		eventListener.resolveDatabaseEvents(this, events);
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
}
