
package org.point85.domain.opc.da;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.da.OPCITEMDEF;
import org.openscada.opc.dcom.da.OPCITEMRESULT;
import org.openscada.opc.dcom.da.ValueData;
import org.openscada.opc.dcom.da.impl.OPCItemMgt;

/**
 * Runtime monitored item
 * 
 */
public class OpcDaMonitoredItem {

	private final OPCITEMDEF opcItemDef;
	private final OPCITEMRESULT opcItemResult;
	private ValueData valueData;

	private OpcDaMonitoredGroup group;

	private String pathName;

	public OpcDaMonitoredItem(OPCITEMDEF opcItemDef, OPCITEMRESULT opcItemResult) {
		this.opcItemDef = opcItemDef;
		this.opcItemResult = opcItemResult;
	}

	public ResultSet<Integer> setActive() throws JIException {
		OPCItemMgt itemManager = getGroup().getGroupManager().getItemManagement();
		int handle = getItemResult().getServerHandle();
		return itemManager.setActiveState(true, handle);
	}

	public OPCITEMDEF getItemDef() {
		return this.opcItemDef;
	}

	public OPCITEMRESULT getItemResult() {
		return this.opcItemResult;
	}

	public ValueData getValueData() {
		return this.valueData;
	}

	public void setValueData(ValueData valueData) throws Exception {
		this.valueData = valueData;
	}

	public String getValueString() {
		String valueString = null;
		try {
			valueString = getValue().getValueAsString();
		} catch (Exception e) {
		}
		return valueString;
	}

	public OpcDaVariant getValue() {
		if (valueData != null) {
			return new OpcDaVariant(valueData.getValue());
		} else {
			return new OpcDaVariant(new JIVariant(0));
		}
	}

	public Short getQuality() {
		if (valueData != null) {
			return new Short(this.valueData.getQuality());
		} else {
			return new Short((short) 0);
		}
	}

	public Date getTimestamp() {
		if (valueData != null) {
			return valueData.getTimestamp().getTime();
		} else {
			return new Date();
		}
	}

	public OffsetDateTime getLocalTimestamp() {
		OffsetDateTime odt = null;
		if (valueData != null) {
			Date date = valueData.getTimestamp().getTime();
			odt = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		} else {
			odt = OffsetDateTime.now();
		}
		return odt;
	}

	public String getItemId() {
		return opcItemDef.getItemID();
	}

	@Override
	public String toString() {
		String text = "";
		try {
			text = opcItemDef.getItemID();
		} catch (Exception e) {
		}
		return text;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpcDaMonitoredItem) {
			OpcDaMonitoredItem other = (OpcDaMonitoredItem) obj;
			if (getItemId().equals(other.getItemId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(opcItemDef);
	}

	public OpcDaMonitoredGroup getGroup() {
		return group;
	}

	public void setGroup(OpcDaMonitoredGroup group) {
		this.group = group;
	}

	public String getPathName() {
		return pathName;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}
}
