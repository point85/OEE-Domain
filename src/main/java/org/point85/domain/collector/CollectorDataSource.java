package org.point85.domain.collector;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.DataSourceConverter;
import org.point85.domain.plant.NamedObject;

@Entity
@Table(name = "DATA_SOURCE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@AttributeOverride(name = "primaryKey", column = @Column(name = "SOURCE_KEY"))

public abstract class CollectorDataSource extends NamedObject {
	public static final int DEFAULT_UPDATE_PERIOD_MSEC = 5000;

	@Column(name = "HOST")
	private String host;

	@Column(name = "USER_NAME")
	private String userName;

	@Column(name = "PASSWORD")
	private String userPassword;

	// to avoid repeated column mapping error
	@Column(name = "TYPE", insertable = false, updatable = false)
	@Convert(converter = DataSourceConverter.class)
	private DataSourceType sourceType;

	@Column(name = "PORT")
	private Integer port;

	@Column(name = "END_PATH")
	private String endpointPath;

	public CollectorDataSource() {
		super();
	}

	protected CollectorDataSource(String name, String description) {
		super(name, description);
	}

	public abstract String getId();

	public abstract void setId(String id);

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassword() throws Exception {
		return DomainUtils.decode(this.userPassword);
	}

	public void setPassword(String password) throws Exception {
		this.userPassword = DomainUtils.encode(password);
	}

	public DataSourceType getDataSourceType() {
		return this.sourceType;
	}

	public void setDataSourceType(DataSourceType type) {
		this.sourceType = type;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getEndpointPath() {
		return endpointPath;
	}

	public void setEndpointPath(String path) {
		this.endpointPath = path;
	}

	@Override
	public String toString() {
		return getId();
	}
}
