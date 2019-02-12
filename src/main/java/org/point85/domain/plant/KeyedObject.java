package org.point85.domain.plant;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class KeyedObject {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long primaryKey;

	protected KeyedObject() {
		// nothing to initialize
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
