package org.point85.domain.socket;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;

/**
 * The WebSocketSource class represents an web socket server as a source of
 * application events.
 *
 */
@Entity
@DiscriminatorValue(DataSourceType.WEB_SOCKET_VALUE)
public class WebSocketSource extends CollectorDataSource {
	@Column(name = "KEYSTORE")
	private String keystore;

	@Column(name = "KEYSTORE_PWD")
	private String keystorePassword;

	// overloaded
	@Column(name = "MSG_MODE")
	private String messageMode;

	// client auth as String
	@Column(name = "SEC_POLICY")
	private String clientAuthentication;

	/**
	 * Constructor
	 */
	public WebSocketSource() {
		super();
		setDataSourceType(DataSourceType.WEB_SOCKET);
	}

	/**
	 * Constructor
	 * @param name Name of source
	 * @param description Description of source
	 */
	public WebSocketSource(String name, String description) {
		super(name, description);
		setDataSourceType(DataSourceType.WEB_SOCKET);
	}

	public boolean isClientAuthorization() {
		boolean value = false;

		if (clientAuthentication != null && clientAuthentication.equals("true")) {
			value = true;
		}
		return value;
	}

	public void setClientAuthorization(boolean value) {
		clientAuthentication = value ? "true" : "false";
	}

	public String getKeystore() {
		return keystore;
	}

	public void setKeystore(String fileName) {
		this.keystore = fileName;
	}

	public String getKeystorePassword() {
		return DomainUtils.decode(keystorePassword);
	}

	public void setKeystorePassword(String password) {
		this.keystorePassword = DomainUtils.encode(password);
	}

	// overload message mode
	public String getKeyPassword() {
		return DomainUtils.decode(messageMode);
	}

	public void setKeyPassword(String password) {
		this.messageMode = DomainUtils.encode(password);
	}

	@Override
	public String getId() {
		return getName();
	}

	@Override
	public void setId(String id) {
		setName(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WebSocketSource) {
			return super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getHost(), getKeystore());
	}
}
