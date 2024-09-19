package org.point85.domain.cron;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.point85.domain.i18n.DomainLocalizer;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CronEventClient manages a job scheduler and the associated trigger
 * expression.
 *
 */
public class CronEventClient implements JobListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(CronEventClient.class);

	private static final String OEE_GROUP = "OEE";

	public static final String SOURCE_ID_KEY = "SOURCE_ID";

	private List<String> cronExpressions = new ArrayList<>();

	private List<JobDetailImpl> jobDetails = new ArrayList<>();

	// the source ids of interest
	protected List<String> sourceIds;

	// collector data source
	protected CronEventSource dataSource;

	// Quartz scheduler
	private Scheduler scheduler;

	// listener to call back
	private CronEventListener eventListener;

	/**
	 * Constructor
	 * 
	 * @param eventListener   {@link CronEventListener}
	 * @param cronExpressions List of cron expressions
	 * @throws Exception Exception
	 */
	public CronEventClient(CronEventListener eventListener, List<String> cronExpressions) throws Exception {
		this.cronExpressions = cronExpressions;

		// event callback listener
		setEventListener(eventListener);

		startScheduler();
	}

	/**
	 * Constructor
	 * 
	 * @param eventListener   {@link CronEventListener}
	 * @param eventSource     {@link CronEventSource}
	 * @param sourceIds       List of source identifier
	 * @param cronExpressions List of cron expressions
	 * @throws Exception Exception
	 */
	public CronEventClient(CronEventListener eventListener, CronEventSource eventSource, List<String> sourceIds,
			List<String> cronExpressions) throws Exception {
		this.cronExpressions = cronExpressions;
		this.dataSource = eventSource;
		this.sourceIds = sourceIds;

		// event callback listener
		setEventListener(eventListener);

		startScheduler();
	}

	public CronEventSource getCronEventSource() {
		return this.dataSource;
	}

	private void startScheduler() throws Exception {
		// create the job scheduler
		scheduler = new StdSchedulerFactory().getScheduler();
		scheduler.start();

		// listen for execution events
		scheduler.getListenerManager().addJobListener(this);
	}

	public Scheduler getScheduler() {
		return this.scheduler;
	}

	/**
	 * Schedule the job for the cron expressions
	 * 
	 * @throws Exception Exception
	 */
	public void scheduleJobs() throws Exception {
		for (int i = 0; i < cronExpressions.size(); i++) {
			scheduleJob(dataSource.getName(), cronExpressions.get(i), sourceIds.get(i));
		}
	}

	/**
	 * Unschedule all currently running jobs
	 * 
	 * @throws Exception Exception
	 */
	public void unscheduleJobs() throws Exception {
		for (int i = 0; i < cronExpressions.size(); i++) {
			unscheduleJob(dataSource.getName());
		}
	}

	/**
	 * Schedule this job
	 * 
	 * @param jobName        Job name
	 * @param cronExpression Cron expression
	 * @param sourceId       Source identifier
	 * @throws Exception Exception
	 */
	private void scheduleJob(String jobName, String cronExpression, String sourceId) throws Exception {
		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setName(jobName);
		jobDetail.setGroup(OEE_GROUP);
		jobDetail.setJobClass(CronJob.class);

		JobDataMap jobMap = jobDetail.getJobDataMap();
		jobMap.put(SOURCE_ID_KEY, sourceId);

		jobDetails.add(jobDetail);

		CronTriggerImpl trigger = new CronTriggerImpl();
		trigger.setName(jobName + "_trigger");
		trigger.setGroup(OEE_GROUP);
		trigger.setCronExpression(cronExpression);

		// schedule it
		JobKey jobKey = JobKey.jobKey(jobName, OEE_GROUP);
		if (scheduler.checkExists(jobKey)) {
			unscheduleJob(jobName);
		}
		scheduler.scheduleJob(jobDetail, trigger);
	}

	/**
	 * Unschedule this job
	 * 
	 * @param jobName Job name
	 * @throws Exception Exception
	 */
	public void unscheduleJob(String jobName) throws Exception {
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, OEE_GROUP);
		JobKey jobKey = JobKey.jobKey(jobName, OEE_GROUP);

		scheduler.pauseTrigger(triggerKey);
		scheduler.unscheduleJob(triggerKey);
		scheduler.deleteJob(jobKey);
	}

	/**
	 * Compute the first time this job will run
	 * 
	 * @param cronExpression Cron expression
	 * @return Local date and time job will run
	 * @throws Exception Exception
	 */
	public static LocalDateTime getFirstFireTime(String cronExpression) throws Exception {
		CronTriggerImpl trigger = new CronTriggerImpl();
		trigger.setName("trigger");
		trigger.setCronExpression(cronExpression);

		Date date = trigger.computeFirstFireTime(null);

		if (date == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("invalid.cron.expression", cronExpression));
		}
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public CronEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(CronEventListener eventListener) {
		this.eventListener = eventListener;
	}

	@Override
	public String getName() {
		return "CronEventClientListener";
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		// not applicable
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		// not applicable
	}

	/**
	 * Call the event listener to resolve this event
	 */
	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		if (jobException != null) {
			logger.error(jobException.getMessage());
			return;
		}

		JobDetail jobDetail = context.getJobDetail();

		String jobName = jobDetail.getKey().getName();

		if (logger.isInfoEnabled()) {
			logger.info("Job " + jobName + " was executed");
		}

		if (eventListener != null) {
			eventListener.resolveCronEvent(context);
		}
	}

	/**
	 * Shutdown the job scheduler
	 * 
	 * @throws Exception Exception
	 */
	public void shutdownScheduler() throws Exception {
		scheduler.shutdown(false);
	}

	/**
	 * Start this job executing
	 * 
	 * @param jobName Name of job
	 * @throws Exception Exception
	 */
	public void runJob(String jobName) throws Exception {
		JobKey jobKey = JobKey.jobKey(jobName, OEE_GROUP);
		JobDetail job = JobBuilder.newJob(CronJob.class).withIdentity(jobKey).storeDurably(true).build();

		// Register this job to the scheduler
		scheduler.addJob(job, true);

		// trigger job
		scheduler.triggerJob(jobKey);
	}
}
