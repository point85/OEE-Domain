package org.point85.domain.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileService {
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	public List<File> getFiles(String directory) {
		List<File> fileList = new ArrayList<>();

		if (directory == null) {
			return fileList;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Retrieving files from directory " + directory);
		}

		File dir = new File(directory);

		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles();

			// sort by last modified time (oldest first)
			Arrays.sort(files, Comparator.comparingLong(File::lastModified));

			fileList = Arrays.asList(files);
		}

		return fileList;
	}

	public String readFile(File file) throws IOException {
		String data = "";
		if (file == null) {
			return data;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Reading file " + file.getCanonicalPath());
		}

		Path fileLocation = Paths.get(file.getCanonicalPath());
		byte[] bytes = Files.readAllBytes(fileLocation);
		return new String(bytes);
	}

	public void writeFile(String filePath, String fileName, String content) throws IOException {
		if (filePath == null || fileName == null || content == null) {
			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("Writing file to " + filePath + " named " + fileName + " with content \n" + content);
		}

		// make sure that directory is there
		if (!createDirectory(filePath)) {
			throw new IOException("Cannot create directory " + filePath);
		}

		String canonicalPath = filePath + File.separator + fileName;

		File existing = new File(canonicalPath);

		if (existing.exists()) {
			// delete it
			if (!deleteFile(existing)) {
				throw new IOException("Cannot delete file " + canonicalPath);
			}
		}

		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(canonicalPath));
			writer.write(content);
		} catch (IOException e) {
			throw e;
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	public boolean deleteFile(File file) {
		return file.delete();
	}

	public boolean createDirectory(String directory) {
		if (directory == null) {
			return false;
		}

		File dir = new File(directory);

		if (dir.exists()) {
			return true;
		}
		return dir.mkdirs();
	}

	public OffsetDateTime extractTimestamp(File file) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

		FileTime ct = attributes.creationTime();
		Instant instant = ct.toInstant();

		// default zone id to localhost's
		return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	public void moveFile(String fromPath, String toPath) throws IOException {
		if (logger.isInfoEnabled()) {
			logger.info("Moving file from " + fromPath + " to " + toPath);
		}

		// make sure that directory is there
		int idx = toPath.lastIndexOf(File.separator);

		if (!createDirectory(toPath.substring(0, idx))) {
			throw new IOException("Cannot create directory " + toPath);
		}

		Files.move(Paths.get(fromPath), Paths.get(toPath), StandardCopyOption.REPLACE_EXISTING);
	}

	public void moveFile(File file, String toPath) throws IOException {
		String fromPath = file.getAbsolutePath();

		if (logger.isInfoEnabled()) {
			logger.info("Moving file " + fromPath + " to " + toPath);
		}

		// make sure that directory is there
		int idx = toPath.lastIndexOf(File.separator);

		if (!createDirectory(toPath.substring(0, idx))) {
			throw new IOException("Cannot create directory " + toPath);
		}

		Files.move(Paths.get(fromPath), Paths.get(toPath), StandardCopyOption.REPLACE_EXISTING);
	}
}
