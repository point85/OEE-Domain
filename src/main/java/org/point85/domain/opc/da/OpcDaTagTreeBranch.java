package org.point85.domain.opc.da;

import java.util.Collection;

import org.openscada.opc.lib.da.browser.Branch;

public class OpcDaTagTreeBranch {
    private Branch branch;

    public OpcDaTagTreeBranch(Branch branch) {
        this.branch = branch;
    }

    public Branch getBranch() {
        return this.branch;
    }

    public Collection<Branch> getBranches() {
    	return this.branch.getBranches();
    }
    
    public String getName() {
        return branch.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isLast() {
        boolean isLast = false;

        try {            
            if (branch.getBranches().isEmpty()) {
                isLast = true;
            }
        } catch (Exception e) {
            isLast = true;
        }
        return isLast;
    }
}
