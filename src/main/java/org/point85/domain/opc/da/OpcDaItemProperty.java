/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.domain.opc.da;

/**
 *
 * @author Kent Randall
 */
public class OpcDaItemProperty {

    private Integer propId;
    private String propDescription;
    private Short propdataType;
    private OpcDaVariant propValue;

    public OpcDaItemProperty(int id, String description, short type) {
        this.propId = id;
        this.propDescription = description;
        this.propdataType = type;
    }
    
    public Integer getId() {
        return this.propId;
    }
    
    public String getDescription() {
        return this.propDescription;
    }
    
    public Short getType() {
        return this.propdataType;
    }
    
    public String getTypeAsString() {
        return this.getValue().getTypeAsString();
    }
    
    public OpcDaVariant getValue() {
        return this.propValue;
    }
    
    public String getValueAsString() throws Exception {
        return this.getValue().getValueAsString();
    }

    public void setValue(OpcDaVariant value) {
        this.propValue = value;
    }
}
