package org.point85.domain.plant;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

/**
 * NamedObject is the base class for all named objects
 * 
 * @author Kent Randall
 *
 */
@MappedSuperclass
public abstract class NamedObject extends KeyedObject implements Comparable<NamedObject> {
	// optimistic locking version
	@Version
	private Integer version;

	// name
	@Column(name = "NAME")
	private String name;

	// description
	@Column(name = "DESCRIPTION")
	private String description;

	protected NamedObject() {
		super();
	}

	protected NamedObject(String name, String description) {
		super();
		this.name = name;
		this.description = description;
	}

	/**
	 * Get the optimistic locking version
	 * 
	 * @return version
	 */
	public Integer getVersion() {
		return version;
	}

	/**
	 * Set the optimistic locking version
	 * 
	 * @param version Version
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = (name != null) ? name.trim() : name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = (description != null) ? description.trim() : description;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NamedObject) {
			NamedObject other = (NamedObject) obj;
			if (getName().equals(other.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int compareTo(NamedObject other) {
		return getName().compareTo(other.getName());
	}

	@Override
	public String toString() {
		return name + ", " + description;
	}

	public String getDisplayString() {
		return name + " (" + description + ")";
	}
}
