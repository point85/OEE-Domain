package org.point85.domain.cron;

import org.quartz.JobExecutionContext;

/**
 * Listener for cron events
 *
 */
public interface CronEventListener {
	/**
	 * Resolve the script for this event
	 * @param context JobExecutionContext
	 */
	void resolveCronEvent(JobExecutionContext context);
}
