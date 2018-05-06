package org.point85.domain.opc.da;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.OPC_DA_VALUE)

public class OpcDaSource extends CollectorDataSource {	
	public OpcDaSource() {
		super();
		setDataSourceType(DataSourceType.OPC_DA);
	}

	public OpcDaSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.OPC_DA);
	}

	public String getProgId() {
		return getName();
	}

	public void setProgId(String progId) {
		setName(progId);
	}

	public String getClassId() {
		return null;
	}

	public void setClassId(String classId) {

	}

	@Override
	public String getId() {
		return getProgId();
	}

	@Override
	public void setId(String id) {
		setProgId(id);
	}
}
