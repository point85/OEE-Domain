package org.point85.domain.collector;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.plant.Reason;

@Entity
@Table(name = "AVAIL_SUMMARY")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class AvailabilitySummary extends BaseSummary {
	@OneToOne
	@JoinColumn(name = "REASON_KEY")
	private Reason reason;

	public AvailabilitySummary() {
		super();
	}

	public AvailabilitySummary(LossSummary summary) {
		super(summary);
		this.reason = summary.getReason();
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}
}
