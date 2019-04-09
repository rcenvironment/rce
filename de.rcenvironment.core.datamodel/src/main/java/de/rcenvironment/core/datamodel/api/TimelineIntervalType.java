/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.datamodel.api;

/**
 * Types of timeline intervals appearing during workflow execution.
 * 
 * @author Doreen Seider
 */
public enum TimelineIntervalType {

    /** Workflow run. For internal use only. */
    WORKFLOW_RUN,

    /** Component run. For internal use only. */
    COMPONENT_RUN,
    
    /** Workflow paused. */
    WORKFLOW_PAUSED,
    
    /** External tool run within a component run. */
    EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN;

}
