package org.point85.domain.opc.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openscada.opc.dcom.da.impl.OPCItemProperties;
import org.openscada.opc.lib.da.browser.Branch;
import org.openscada.opc.lib.da.browser.Leaf;
import org.openscada.opc.lib.da.browser.TreeBrowser;

public class OpcDaTreeBrowser {

	private TreeBrowser treeBrowser;
	private OPCItemProperties itemProperties;

	public OpcDaTreeBrowser(TreeBrowser browser,
			OPCItemProperties itemProperties) {
		this.treeBrowser = browser;
		this.itemProperties = itemProperties;
	}

	public TreeBrowser getTreeBrowser() {
		return this.treeBrowser;
	}

	public OpcDaTagTreeBranch browseBranches() throws Exception {
		Branch rootBranch = treeBrowser.browseBranches();
		return new OpcDaTagTreeBranch(rootBranch);
	}

	public Collection<OpcDaTagTreeBranch> getAllBranches() throws Exception {
		OpcDaTagTreeBranch rootBranch = new OpcDaTagTreeBranch(treeBrowser.browse());
		return getBranches(rootBranch);
	}

	public Collection<OpcDaTagTreeBranch> getBranches(OpcDaTagTreeBranch tagBranch)
			throws Exception {

		fillBranches(tagBranch);

		Collection<Branch> branches = tagBranch.getBranch().getBranches();

		Collection<OpcDaTagTreeBranch> tagBranches = new ArrayList<>(
				branches.size());

		for (Branch branch : branches) {
			OpcDaTagTreeBranch aBranch = new OpcDaTagTreeBranch(branch);
			tagBranches.add(aBranch);
		}
		return tagBranches;
	}

	private void fillBranches(OpcDaTagTreeBranch tagBranch) throws Exception {
		treeBrowser.fillBranches(tagBranch.getBranch());
	}

	private void fillLeaves(OpcDaTagTreeBranch tagBranch) throws Exception {
		treeBrowser.fillLeaves(tagBranch.getBranch());
	}

	public boolean isLastNode(OpcDaTagTreeBranch tagBranch) throws Exception {
		fillBranches(tagBranch);

		boolean isLast = tagBranch.isLast();

		if (isLast) {
			fillLeaves(tagBranch);
		}

		return isLast;
	}

	public Collection<OpcDaBrowserLeaf> getLeaves(OpcDaTagTreeBranch tagBranch) {
		Collection<OpcDaBrowserLeaf> allLeaves = new ArrayList<>();

		Branch branch = tagBranch.getBranch();
		Collection<Leaf> leaves = branch.getLeaves();

		for (Leaf leaf : leaves) {
			allLeaves.add(new OpcDaBrowserLeaf(leaf, itemProperties));
		}

		return allLeaves;
	}

	private Branch findChildBranch(Branch parent, String name) throws Exception {
		Branch branch = null;

		Collection<Branch> children = parent.getBranches();

		for (Branch child : children) {
			if (child.getName().equals(name)) {
				branch = child;
				this.getTreeBrowser().fillBranches(branch);
				break;
			}
		}

		return branch;
	}
	
	public OpcDaBrowserLeaf findTag(String pathName) throws Exception {
		String[] tokens = OpcDaBrowserLeaf.getPath(pathName);
		int len = tokens.length - 1;
		List<String> accessPath = new ArrayList<>(len);

		for (int i = 0; i < len; i++) {
			accessPath.add(tokens[i]);
		}
		return findTag(accessPath, tokens[len]);
	}

	public OpcDaBrowserLeaf findTag(List<String> branchNames, String leafName)
			throws Exception {
		OpcDaBrowserLeaf leafTag = null;

		// get root branch
		Branch next = browseBranches().getBranch();

		// look for matching name at each level in the name space
		for (String branchName : branchNames) {
			Branch child = this.findChildBranch(next, branchName);

			if (child != null) {
				next = child;
			}
		}

		if (next != null) {
			TreeBrowser tb = this.getTreeBrowser();
			tb.fillLeaves(next);
			Collection<Leaf> leaves = next.getLeaves();

			for (Leaf leaf : leaves) {
				if (leaf.getName().equals(leafName)) {
					leafTag = new OpcDaBrowserLeaf(leaf, this.itemProperties);
					break;
				}
			}
		}

		return leafTag;
	}
}
