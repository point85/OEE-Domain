package org.point85.domain.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.point85.domain.collector.CollectorDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileEventClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(FileEventClient.class);

	public static final String READY_FOLDER = "ready";
	public static final String PROCESSING_FOLDER = "processing";
	public static final String PASS_FOLDER = "pass";
	public static final String FAIL_FOLDER = "fail";

	private static final String ERROR_EXT = ".error";

	// polling interval in msec
	private List<Integer> pollingPeriods;

	// polling timer
	private List<Timer> pollingTimers;

	// polling task
	private List<PollingTask> pollingTasks;

	// service handling the queried data
	private FileEventListener eventListener;

	// file service
	private FileService fileService;

	// the source id of interest
	private List<String> sourceIds;

	// the file share
	private FileEventSource fileSource;

	// files being worked on
	private List<String> inProcessFiles = new ArrayList<>();

	public FileEventClient(FileEventListener eventListener, FileEventSource fileSource, List<String> sourceIds,
			List<Integer> pollingPeriods) {
		this.fileService = new FileService();
		this.eventListener = eventListener;
		this.pollingTimers = new ArrayList<>();
		this.pollingTasks = new ArrayList<>();
		this.pollingPeriods = pollingPeriods;
		this.fileSource = fileSource;
		this.sourceIds = sourceIds;
	}

	public FileService getFileService() {
		return fileService;
	}

	public String readFile(File file) throws Exception {
		return fileService.readFile(file);
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

	private void onPoll(String sourceId) {
		if (sourceId == null) {
			logger.error("The file source id is null.");
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Querying for new files for source " + sourceId);
		}

		// query file server for new files
		String filePath = fileSource.getNetworkPath(sourceId) + File.separator + READY_FOLDER;
		List<File> files = fileService.getFiles(filePath);

		eventListener.resolveFileEvents(this, sourceId, files);
	}

	public FileEventSource getFileEventSource() {
		return fileSource;
	}

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

	@Override
	public int hashCode() {
		return Objects.hash(fileSource.getId());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof FileEventClient)) {
			return false;
		}
		FileEventClient otherClient = (FileEventClient) other;

		return fileSource.getId().equals(otherClient.getFileEventSource().getId());
	}

	public void moveFile(File file, FileEventSource source, String sourceId, String folder) throws IOException {
		String toPath = source.getHost() + File.separator + sourceId + File.separator + FileEventClient.READY_FOLDER
				+ File.separator + file.getName();

		fileService.moveFile(file, toPath);
	}

	public void moveFile(File file, String fromFolder, String toFolder) throws IOException {
		this.moveFile(file, fromFolder, toFolder, null);
	}

	public void moveFile(File file, String fromFolder, String toFolder, Exception e) throws IOException {
		// path in ready folder
		String path = file.getCanonicalPath();

		String source = path.replace(READY_FOLDER, fromFolder);
		String destination = path.replace(READY_FOLDER, toFolder);

		// move the file to the destination folder
		fileService.moveFile(source, destination);

		if (e != null) {
			int idx = destination.lastIndexOf(File.separator);
			String errorPath = destination.substring(0, idx);

			// write a file with the error content
			fileService.writeFile(errorPath, file.getName() + ERROR_EXT,
					e.getClass().getSimpleName() + "\n" + e.getMessage());
		}
	}

	public void writeFile(FileEventSource source, String sourceId, String folder, String content) throws IOException {
		String pathName = source.getHost() + File.separator + sourceId + File.separator + folder;
		fileService.writeFile(pathName, UUID.randomUUID().toString(), content);
	}

	public synchronized boolean fileIsProcessing(File file) {
		if (inProcessFiles.contains(file.getName())) {
			// already being worked on
			return true;
		}

		inProcessFiles.add(file.getName());
		return false;
	}

	public synchronized void stopProcessing(File file) {
		inProcessFiles.remove(file.getName());
	}
}
