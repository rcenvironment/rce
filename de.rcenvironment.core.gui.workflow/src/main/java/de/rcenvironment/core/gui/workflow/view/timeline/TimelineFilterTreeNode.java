/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node for Component tree.
 * @author Hendrik Abbenhaus
 */
public class TimelineFilterTreeNode {

    private String componentID;

    private List<TimelineFilterTreeNode> children = new ArrayList<TimelineFilterTreeNode>();
    
    private TimelineComponentRow row = null; 

    private TimelineFilterTreeNode parent;
    
    private boolean checked = true;

    public TimelineFilterTreeNode() {
       
    }
    
    public void setComponentID(String newComponentID){
        this.componentID = newComponentID;
    }

    public List<TimelineFilterTreeNode> getChildren() {
        return children;
    }

    /**
     * Adds a child to the List of children.
     * @param child the child
     */
    public void addChild(TimelineFilterTreeNode child) {
        children.add(child);
        child.setParent(this);
    }

    public TimelineFilterTreeNode getParent() {
        return parent;
    }

    public void setParent(TimelineFilterTreeNode parent) {
        this.parent = parent;
    }
    
    /**
     * Sets itself and its children checked or unchecked.
     * @param checked <code>true</code> or <code>false</code>
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
        if (!children.isEmpty()){
            for (TimelineFilterTreeNode child : children){
                child.setChecked(checked);
            }
        }
    }
    
    
    public boolean isChecked() {
        return checked;
    }

    /**
     * Returns a DisplayName for this Node.
     * @return DisplayName for this Node
     */
    public String getDisplayName() {
        
        if (this.hasRow()){
            return row.getName();
        } else {
            return TimelineView.getComponentNameFromId(componentID, this);
        }
    }
    
    public String getComponentID(){
        return this.componentID;
    }
    
    /**
     * Check if this instance has a connected row.
     * @return Returns false if row is empty else true
     */
    public boolean hasRow(){
        if (row == null){
            return false;
        }
        return true;
    }
    
    /**
     * Checks if there already is a child with equal component id. 
     * @param checkComponentID the componentID to check
     * @return Returns the Node with same ID or {@value null} if there is no child with same ID
     */
    public TimelineFilterTreeNode hasChildWithComponentID(String checkComponentID){
        for (TimelineFilterTreeNode current : getChildren()){
            if (current.getComponentID().equals(checkComponentID)){
                return current;
            }
        }
        return null;
    }
    

    public TimelineComponentRow getRow() {
        return row;
    }

    public void setRow(TimelineComponentRow row) {
        this.row = row;
    }
}
