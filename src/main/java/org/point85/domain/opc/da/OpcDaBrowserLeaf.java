package org.point85.domain.opc.da;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

import org.openscada.opc.dcom.da.PropertyDescription;
import org.openscada.opc.dcom.da.impl.OPCItemProperties;
import org.openscada.opc.lib.da.browser.Leaf;

public class OpcDaBrowserLeaf {
	private static final String PATH_DELIMITERS = "\\.";
	private static final char PATH_DELIMITER = '.';

	private OpcDaVariant dataType;

	private Leaf leaf;

	private Collection<PropertyDescription> properties;

	private OPCItemProperties propertyManager;

	public OpcDaBrowserLeaf(Leaf leaf, OPCItemProperties propertyManager) {
		this.leaf = leaf;
		this.propertyManager = propertyManager;
	}

	public String getItemId() {
		return leaf.getItemId();
	}

	public Leaf getLeaf() {
		return leaf;
	}

	public Collection<PropertyDescription> getProperties() throws Exception {
		if (properties == null) {
			properties = propertyManager.queryAvailableProperties(leaf.getItemId());
		}
		return properties;
	}

	@Override
	public String toString() {
		return getItemId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpcDaBrowserLeaf) {
			OpcDaBrowserLeaf other = (OpcDaBrowserLeaf) obj;
			if (getItemId().equals(other.getItemId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 41 + Objects.hashCode(leaf);
	}

	public LinkedList<String> getAccesspath() {
		LinkedList<String> stack = (LinkedList<String>) getLeaf().getParent().getBranchStack();

		return stack;
	}

	public String getName() {
		return leaf.getName();
	}

	public String getPathName() {
		StringBuffer sb = new StringBuffer();

		LinkedList<String> path = getAccesspath();

		for (int i = 0; i < path.size(); i++) {
			sb.append(path.get(i)).append(PATH_DELIMITER);
		}
		sb.append(getName());

		return sb.toString();
	}

	public static String[] getPath(String pathName) {
		String[] tokens = pathName.split(PATH_DELIMITERS);
		return tokens;
	}

	public OpcDaVariant getDataType() {
		return dataType;
	}

	public void setDataType(OpcDaVariant dataType) {
		this.dataType = dataType;
	}
}
