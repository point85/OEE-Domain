package org.point85.domain.db;

import java.util.List;

public interface DatabaseEventListener {
	void resolveDatabaseEvents(DatabaseEventClient client, List<DatabaseEvent> events);
}
