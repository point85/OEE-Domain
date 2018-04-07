package org.point85.domain.collector;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.point85.domain.plant.Material;
import org.point85.domain.script.ResolvedEvent;

@Entity
@Table(name = "SETUP")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AttributeOverride(name = "primaryKey", column = @Column(name = "SETUP_KEY"))

public class SetupRecord extends BaseRecord {
	@OneToOne
	@JoinColumn(name = "MATL_KEY")
	private Material material;

	@Column(name = "JOB")
	private String job;

	public SetupRecord() {
		super();
	}

	public SetupRecord(ResolvedEvent event) {
		super(event);
		this.material = event.getMaterial();
		this.job = event.getJob();
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
