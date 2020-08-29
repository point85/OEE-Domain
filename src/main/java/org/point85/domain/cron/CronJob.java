package org.point85.domain.cron;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * The cron job. The business logic is handled in the job listener by invoking
 * the java script in associated the resolver.
 *
 */
public class CronJob implements Job {
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// The business logic is handled in the job listener by invoking the java script
		// in associated the resolver.
	}
}
