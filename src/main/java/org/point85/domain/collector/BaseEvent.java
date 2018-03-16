package org.point85.domain.collector;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.point85.domain.persistence.OffsetDateTimeConverter;
import org.point85.domain.plant.Material;
import org.point85.domain.script.ResolvedEvent;

@MappedSuperclass
public class BaseEvent extends BaseRecord {

	@OneToOne
	@JoinColumn(name = "MATL_KEY")
	private Material material;

	@Column(name = "JOB")
	private String job;

	@Column(name = "EVENT_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime sourceTimestamp;

	protected BaseEvent() {
		super();
	}

	protected BaseEvent(ResolvedEvent event) {
		super(event.getEquipment());
		this.material = event.getMaterial();
		this.job = event.getJob();
		this.sourceTimestamp = event.getTimestamp();
	}

	public OffsetDateTime getSourceTimestamp() {
		return sourceTimestamp;
	}

	public void setSourceTimestamp(OffsetDateTime sourceTimestamp) {
		this.sourceTimestamp = sourceTimestamp;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

}
