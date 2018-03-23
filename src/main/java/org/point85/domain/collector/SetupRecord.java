package org.point85.domain.collector;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.point85.domain.script.ResolvedEvent;

@Entity
@Table(name = "SETUP")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AttributeOverride(name = "primaryKey", column = @Column(name = "SETUP_KEY"))

public class SetupRecord extends BaseRecord {

	public SetupRecord() {
		super();
	}

	public SetupRecord(ResolvedEvent event) {
		super(event);
	}
}
