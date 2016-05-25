/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.timeline;

import org.eclipse.swt.graphics.RGB;

/**
 * 
 * @author Hendrik Abbenhaus
 *
 */
public final class TimelineViewConstants {
      
    /***/
    public static final RGB CANVAS_COLOR_BACKGROUND_PREVIEW = new RGB(100, 100, 100);

    /***/
    public static final RGB CANVAS_COLOR_BACKGROUND = new RGB(255, 255, 255);

    /***/
    public static final RGB CANVAS_COLOR_BACKGROUND_LINE = new RGB(0, 0, 0);
    
    /***/
    public static final RGB CANVAS_COLOR_SELECTION_AREA = new RGB(255, 255, 255);
    
    /***/
    public static final int CANVAS_SELECTION_AREA_OPACITY = 140;
    
    /***/
    public static final int CANVAS_DEFAULT_OPACITY = 1000;
    
    
    /**
     * Key for workflow-name in json-string.
     */
    public static final String JSON_WORKFLOWNAME = "WorkflowName";
    /**
     * Key for workflow-starttime in json-string.
     */
    public static final String JSON_WORKFLOWSTARTTIME = "WorkflowStartTime";
    /**
     * Key for workflow-starttime in json-string.
     */
    public static final String JSON_WORKFLOWENDTIME = "WorkflowEndTime";
    /**
     * Key for component-list in json-string.
     */
    public static final String JSON_COMPONENTS = "Components"; 
    /**
     * Key for component name in json-string.
     */
    public static final String JSON_COMPONENT_NAME = "Name";
    /**
     * Key for component id in json-string.
     */
    public static final String JSON_COMPONENT_ID = "Id";
    /**
     * Key for component events in json-string.
     */
    public static final String JSON_COMPONENT_EVENTS = "Events";
    /**
     * Key for component eventtext in json-string.
     */
    public static final String JSON_COMPONENT_EVENT_INFOTEXT = "InformationText";
    
    /**
     * Key for endtime of activity in json-string.
     */
    public static final String JSON_ACTIVITYTIME = "Time";
    
    /**
     * Key for activitytype in json-string.
     */
    public static final String JSON_ACTIVITYTYPE = "Type";
    
    /**
     * Canvas size value.
     */
    public static final int CANVAS_DEFAULT_HEIGHT_HINT = 15;
    
        /**
     * Scale default value.
     */
    public static final int SCALE_DEFAULT_VALUE = 100;
        
    /**
     * 
     */
    public static final int NAMELABEL_DEFAULT_WIDTH = 72;

    private TimelineViewConstants(){
        
    }

}
