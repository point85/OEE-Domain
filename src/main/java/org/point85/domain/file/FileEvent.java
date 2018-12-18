package org.point85.domain.file;

import java.io.File;

public class FileEvent {
	private File file;
	
	public FileEvent(File file) {
		this.setFile(file);
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
