package org.point85.domain.generic;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.dto.GenericSourceDto;

/**
 * The GeneriSource class represents user defined data as a source of
 * application events or other purposes.
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.GENERIC_VALUE)
public class GenericSource extends CollectorDataSource {
	// overloaded attributes
	@Column(name = "KEYSTORE")
	private String attribute1;

	@Column(name = "END_PATH")
	private String attribute2;

	@Column(name = "SEC_POLICY")
	private String attribute3;

	@Column(name = "MSG_MODE")
	private String attribute4;

	@Column(name = "KEYSTORE_PWD")
	private String attribute5;

	public GenericSource() {
		super();
		setDataSourceType(DataSourceType.GENERIC);
	}

	public GenericSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.GENERIC);
	}

	public GenericSource(GenericSourceDto dto) {
		super(dto);

		attribute1 = dto.getAttribute1();
		attribute2 = dto.getAttribute2();
		attribute3 = dto.getAttribute3();
		attribute4 = dto.getAttribute4();
		attribute5 = dto.getAttribute5();
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	public String getAttribute1() {
		return attribute1;
	}

	public void setAttribute1(String attribute) {
		this.attribute1 = attribute;
	}

	public String getAttribute2() {
		return attribute2;
	}

	public void setAttribute2(String attribute) {
		this.attribute2 = attribute;
	}

	public String getAttribute3() {
		return attribute3;
	}

	public void setAttribute3(String attribute) {
		this.attribute3 = attribute;
	}

	public String getAttribute4() {
		return attribute4;
	}

	public void setAttribute4(String attribute) {
		this.attribute4 = attribute;
	}

	public String getAttribute5() {
		return attribute5;
	}

	public void setAttribute5(String attribute) {
		this.attribute5 = attribute;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GenericSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getPort(), getDescription());
	}
}
