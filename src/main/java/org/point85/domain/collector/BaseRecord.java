package org.point85.domain.collector;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.point85.domain.plant.Equipment;

@MappedSuperclass
public class BaseRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "EVENT_KEY")
	private Long primaryKey;
	
	@OneToOne
	@JoinColumn(name = "ENT_KEY")
	private Equipment equipment;
	
	protected BaseRecord() {
		
	}
	
	protected BaseRecord(Equipment equipment) {
		this.equipment = equipment;
	}
	
	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}
	
}
