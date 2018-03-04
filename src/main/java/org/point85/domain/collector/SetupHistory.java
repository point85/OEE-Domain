package org.point85.domain.collector;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.point85.domain.script.ResolvedEvent;

@Entity
@Table(name = "SETUP_HISTORY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)

@NamedQueries({
		@NamedQuery(name = SetupHistory.LAST_RECORD, query = "SELECT hist FROM SetupHistory hist WHERE hist.equipment = :equipment AND hist.type = :type ORDER BY hist.sourceTimestamp DESC"), })

public class SetupHistory extends BaseEvent {
	public static final String LAST_RECORD = "Setup.Last";

	public SetupHistory() {
		super();
	}

	public SetupHistory(ResolvedEvent event) {
		super(event);
	}
}
