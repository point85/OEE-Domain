/*
MIT License

Copyright (c) 2016 Kent Randall

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.point85.domain.schedule;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.point85.domain.i18n.DomainLocalizer;
import org.point85.domain.plant.KeyedObject;

/**
 * Class Named represents a named object such as a Shift or Team.
 * 
 * @author Kent Randall
 *
 */
@MappedSuperclass
public abstract class Named extends KeyedObject {
	// name
	@Column(name = "NAME")
	private String name;

	// description
	@Column(name = "DESCRIPTION")
	private String description;

	protected Named() {
	}

	protected Named(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Get name
	 * 
	 * @return Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set name
	 * 
	 * @param name Name
	 * @throws Exception exception
	 */
	public void setName(String name) throws Exception {
		if (name == null) {
			throw new Exception(DomainLocalizer.instance().getErrorString("name.not.defined"));
		}
		this.name = name;
	}

	/**
	 * Get description
	 * 
	 * @return Description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set description
	 * 
	 * @param description Description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Compare this named object to another named object
	 * 
	 * @return true if equal
	 */
	@Override
	public boolean equals(Object other) {

		if (!(other instanceof Named)) {
			return false;
		}

		return getName().equals(((Named) other).getName());
	}

	/**
	 * Get the hash code
	 * 
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(getName());
	}

	/**
	 * Get a string representation of a named object
	 */
	@Override
	public String toString() {
		return getName() + " (" + getDescription() + ")";
	}
}
