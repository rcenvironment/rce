/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.timeline;

import de.rcenvironment.core.gui.resources.api.ColorSource;
import de.rcenvironment.core.gui.resources.api.StandardColors;

/**
 * 
 * @author Hendrik Abbenhaus
 *
 */
public final class TimelineViewConstants {

    /***/
    public static final ColorSource CANVAS_COLOR_BACKGROUND = StandardColors.RCE_WHITE;

    /***/
    public static final ColorSource CANVAS_COLOR_BACKGROUND_LINE = StandardColors.RCE_BLACK;
    
    /***/
    public static final ColorSource CANVAS_COLOR_SELECTION_AREA = StandardColors.RCE_WHITE;
    
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
