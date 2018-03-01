package org.point85.domain.plant;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;

@MappedSuperclass
public abstract class KeyedObject {
	public static final String SEQ_GEN = "SEQ_GEN";
	public static final String JPA_SEQ = "JPA_SEQ";

	// database primary key, allocationSize values per call
	@SequenceGenerator(name = SEQ_GEN, sequenceName = JPA_SEQ, allocationSize = 10)
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQ_GEN)
	private Long primaryKey;

	protected KeyedObject() {

	}

	/**
	 * Get the database record's primary key
	 * 
	 * @return Key
	 */
	public Long getKey() {
		return primaryKey;
	}

	/**
	 * Set the database record's primary key
	 * 
	 * @param key
	 *            Key
	 */
	public void setKey(Long key) {
		this.primaryKey = key;
	}
}
