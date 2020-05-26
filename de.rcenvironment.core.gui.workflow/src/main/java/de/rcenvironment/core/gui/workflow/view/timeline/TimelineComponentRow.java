/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.Date;

import org.eclipse.swt.graphics.Image;

/**
 * Each instance of a component gets it's own {@link TimelineComponentRow}.
 * @author Hendrik Abbenhaus
 */
public class TimelineComponentRow implements Comparable<TimelineComponentRow> {
    
    private TimelineActivityPart[] activities = null;
    
    private String componentID = null;
    
    private String name = null;
    
    private Date visibleStartTime = null;
    
    private Date visibleEndTime = null;
    
    private Date workflowStartTime = null;
    
    private Date workflowEndTime = null;

    
    public TimelineComponentRow(String name, String componentID, Date visibleStartTime, Date visibleEndTime) {
        this.name = name;
        this.componentID = componentID;
        this.visibleStartTime = visibleStartTime;
        this.visibleEndTime = visibleEndTime;
    }
    
    public void setActivities(TimelineActivityPart[] activities){
        this.activities = activities;
    }
    
    public TimelineActivityPart[] getActivities(){
        return this.activities;
    }
    
    /**
     * 
     * @return an Icon for the current Component Instance. 
     */
    public Image getIcon(){
        return TimelineView.getImageIconFromId(this.componentID, this);
    }
    
    /**
     * 
     * @return the Display name of the current Component.
     */
    public String getName(){
        return this.name;
    }
    
    /**
     * Returns an identifier of the current Component.
     * @return the identifier
     */
    public String getComponentID(){
        return this.componentID;
    }

    public Date getVisibleStartTime() {
        return this.visibleStartTime;
    }

    public Date getVisibleEndTime() {
        return this.visibleEndTime;
    }

    public Date getWorkflowStartTime() {
        return workflowStartTime;
    }

    public void setWorkflowStartTime(Date workflowStartTime) {
        this.workflowStartTime = workflowStartTime;
    }

    public Date getWorkflowEndTime() {
        return workflowEndTime;
    }

    public void setWorkflowEndTime(Date workflowEndTime) {
        this.workflowEndTime = workflowEndTime;
    }

    @Override
    public int compareTo(TimelineComponentRow o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

}
