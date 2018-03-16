package org.point85.domain.collector;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.point85.domain.persistence.EventResolverTypeConverter;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.script.ResolvedEvent;

@Entity
@Table(name = "SETUP_HISTORY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)

public class SetupHistory extends BaseEvent {
	
	@Column(name = "TYPE")
	@Convert(converter = EventResolverTypeConverter.class)
	private EventResolverType type;

	public SetupHistory() {
		super();
	}

	public SetupHistory(ResolvedEvent event) {
		super(event);
		this.type = event.getResolverType();
	}
	
	public EventResolverType getType() {
		return type;
	}

	public void setType(EventResolverType type) {
		this.type = type;
	}
}
