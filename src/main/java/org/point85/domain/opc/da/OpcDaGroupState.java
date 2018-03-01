/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.domain.opc.da;

import org.openscada.opc.dcom.da.OPCGroupState;

/**
 *
 * @author krandal
 */
public class OpcDaGroupState {

    private OPCGroupState groupState;

    public OpcDaGroupState() {
        groupState = new OPCGroupState();
    }

    public OpcDaGroupState(OPCGroupState state) {
        this.groupState = state;
    }

    public String getName() {
        return groupState.getName();
    }

    public int getUpdateRate() {
        return groupState.getUpdateRate();
    }

    public void setUpdateRate(int rate) {
        groupState.setUpdateRate(rate);
    }

    public float getPercentDeadband() {
        return groupState.getPercentDeadband();
    }

    public void setPercentDeadband(float deadband) {
        groupState.setPercentDeadband(deadband);
    }

    public boolean isActive() {
        return groupState.isActive();
    }

    public void setActive(boolean active) {
        groupState.setActive(active);
    }
}
