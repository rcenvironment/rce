/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 *
 * @author Hendrik Abbenhaus
 */
public class TimelineFilterTreeNode {

    private String componentID;

    private List<TimelineFilterTreeNode> children = new ArrayList<TimelineFilterTreeNode>();
    
    private TimelineComponentRow row = null; 

    private TimelineFilterTreeNode parent;

    public TimelineFilterTreeNode() {
       
    }
    
    public void setComponentID(String newComponentID){
        this.componentID = newComponentID;
    }

    public List<TimelineFilterTreeNode> getChildren() {
        return children;
    }

    /**
     * 
     * 
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
     * 
     * @return Returns a DisplayName for this Node
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
     * 
     * 
     * @return Returns true if row is not empty
     */
    public boolean hasRow(){
        if (row == null){
            return false;
        }
        return true;
    }
    
    /**
     * 
     * 
     * @param checkComponentID the componentID to check
     * @return Returns the Node with same ID or null if there is no child with same ID
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
