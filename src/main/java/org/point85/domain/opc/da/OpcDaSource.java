package org.point85.domain.opc.da;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;

@Entity
@DiscriminatorValue(DataSourceType.OPC_DA_VALUE)

@NamedQueries({
		@NamedQuery(name = OpcDaSource.DA_SRC_KEY_BY_NAME, query = "SELECT source.primaryKey, source.version FROM OpcDaSource source WHERE source.name = :name"),
		@NamedQuery(name = OpcDaSource.DA_SRC_BY_NAME, query = "SELECT source FROM OpcDaSource source WHERE source.name = :name"),
		@NamedQuery(name = OpcDaSource.DA_PROG_IDS, query = "SELECT source.name FROM OpcDaSource source"), })
public class OpcDaSource extends DataSource {

	// queries
	public static final String DA_PROG_IDS = "OPCDA.ProgIds";
	public static final String DA_SRC_BY_NAME = "OPCDA.ByName";
	public static final String DA_SRC_BY_KEY = "OPCDA.ByKey";
	public static final String DA_SRC_KEY_BY_NAME = "OPCDA.KeyByName";

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
		return param1;
	}

	public void setClassId(String classId) {
		this.param1 = classId;
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
