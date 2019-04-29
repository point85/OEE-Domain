package org.point85.domain.polling;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.point85.domain.collector.CollectorDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for client that periodically poll a data source.
 *
 */
public abstract class PollingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(PollingClient.class);

	// polling interval in msec
	protected List<Integer> pollingPeriods;

	// polling timer
	protected List<Timer> pollingTimers;

	// polling task
	protected List<PollingTask> pollingTasks;

	// the source ids of interest
	protected List<String> sourceIds;

	// collector data source
	protected CollectorDataSource dataSource;

	protected PollingClient(CollectorDataSource dataSource, List<String> sourceIds, List<Integer> pollingPeriods) {
		this.pollingTimers = new ArrayList<>();
		this.pollingTasks = new ArrayList<>();
		this.pollingPeriods = pollingPeriods;
		this.dataSource = dataSource;
		this.sourceIds = sourceIds;
	}

	public void startPolling() {
		for (int i = 0; i < sourceIds.size(); i++) {
			if (pollingPeriods.get(i) == null) {
				pollingPeriods.set(i, new Integer(CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC));
			}

			if (logger.isInfoEnabled()) {
				logger.info("Starting to poll for new files every " + pollingPeriods.get(i) + " msec. for sourceId "
						+ sourceIds.get(i));
			}

			startPollingTimer(i);
		}
	}

	public void stopPolling() {
		for (int i = 0; i < sourceIds.size(); i++) {
			stopPollingTimer(i);

			if (logger.isInfoEnabled()) {
				logger.info("Stopped polling for new files from sourceId " + sourceIds.get(i));
			}
		}
		pollingTimers.clear();
	}

	public void cancelPolling() {
		for (Timer pollingTimer : pollingTimers) {
			pollingTimer.cancel();
		}
	}

	private void initializePollingTimer(int i) {
		// create timer and task
		pollingTimers.add(i, new Timer());
		pollingTasks.add(i, new PollingTask(sourceIds.get(i)));
	}

	private void startPollingTimer(int i) {
		if (pollingTimers.size() == i) {
			initializePollingTimer(i);
		}

		long delay = (long) (Math.random() * 5000.0d);
		pollingTimers.get(i).schedule(pollingTasks.get(i), delay, pollingPeriods.get(i));
	}

	private void stopPollingTimer(int i) {
		if (pollingTimers.size() > i) {
			pollingTimers.get(i).cancel();
		}
	}

	protected abstract void onPoll(String sourceId);

	private class PollingTask extends TimerTask {
		private String sourceId;

		private PollingTask(String sourceId) {
			this.sourceId = sourceId;
		}

		@Override
		public void run() {
			onPoll(sourceId);
		}
	}
}
