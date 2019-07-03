package org.point85.domain.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.point85.domain.polling.PollingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect to a file share server and poll for new files for the
 * specified source id.
 *
 */
public class FileEventClient extends PollingClient {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(FileEventClient.class);

	// not localizable
	public static final String READY_FOLDER = "ready";
	public static final String PROCESSING_FOLDER = "processing";
	public static final String PASS_FOLDER = "pass";
	public static final String FAIL_FOLDER = "fail";

	private static final String ERROR_EXT = ".error";

	// service handling the queried data
	private FileEventListener eventListener;

	// file service
	private FileService fileService;

	// files being worked on
	private List<String> inProcessFiles = new ArrayList<>();
	
	public FileEventClient() {
		super();
		this.fileService = new FileService();
	}

	public FileEventClient(FileEventListener eventListener, FileEventSource eventSource, List<String> sourceIds,
			List<Integer> pollingPeriods) {
		super(eventSource, sourceIds, pollingPeriods);
		this.fileService = new FileService();
		this.eventListener = eventListener;
	}

	public FileService getFileService() {
		return fileService;
	}

	public String readFile(File file) throws Exception {
		return fileService.readFile(file);
	}

	@Override
	protected void onPoll(String sourceId) {
		if (logger.isInfoEnabled()) {
			logger.info("Querying for new files for source " + sourceId);
		}

		// query file server for new files
		String filePath = getFileEventSource().getNetworkPath(sourceId) + File.separator + READY_FOLDER;
		List<File> files = fileService.getFiles(filePath);

		eventListener.resolveFileEvents(this, sourceId, files);
	}

	public FileEventSource getFileEventSource() {
		return (FileEventSource) dataSource;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataSource.getId());
	}

	@Override
	public boolean equals(Object other) {

		if (!(other instanceof FileEventClient)) {
			return false;
		}
		FileEventClient otherClient = (FileEventClient) other;

		return getFileEventSource().getId().equals(otherClient.getFileEventSource().getId());
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
