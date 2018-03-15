package org.point85.domain.collector;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.point85.domain.persistence.OffsetDateTimeConverter;
import org.point85.domain.persistence.EventResolverTypeConverter;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.script.EventResolverType;

@MappedSuperclass
public abstract class BaseEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EVENT_KEY")
	private Long primaryKey;
	
	@OneToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;
	
	@OneToOne
	@JoinColumn(name = "MATL_KEY")
	private Material material;
	
	@Column(name = "JOB")
	private String job;
	
	@Column(name = "EVENT_TIME")
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime sourceTimestamp;
	
	@Column(name = "TYPE")
	@Convert(converter = EventResolverTypeConverter.class)
	private EventResolverType type;
	
	public BaseEvent() {
	}
	
	public BaseEvent(ResolvedEvent item) {
		this.equipment = item.getEquipment();
		this.material = item.getMaterial();
		this.job = item.getJob();
		this.sourceTimestamp = item.getTimestamp();
		this.type = item.getResolverType();
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
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
	
	public EventResolverType getType() {
		return type;
	}

	public void setType(EventResolverType type) {
		this.type = type;
	}

}
