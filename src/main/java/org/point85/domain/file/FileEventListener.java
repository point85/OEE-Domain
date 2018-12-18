package org.point85.domain.file;

import java.io.File;
import java.util.List;

public interface FileEventListener {
	void resolveFileEvents(FileEventClient client, String sourceId, List<File> files);
}
