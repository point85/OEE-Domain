package org.point85.domain.collector;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.plant.Reason;
import org.point85.domain.script.ResolvedEvent;

@Entity
@Table(name = "EVENT_HISTORY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class EventHistory extends BaseEvent {

	@OneToOne
	@JoinColumn(name = "REASON_KEY")
	private Reason reason;

	public EventHistory() {
		super();
	}

	public EventHistory(ResolvedEvent event) {
		super(event);
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}
}
