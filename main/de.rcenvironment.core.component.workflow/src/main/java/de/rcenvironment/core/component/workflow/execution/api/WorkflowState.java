/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;


/**
 * States of a workflow.
 * 
 * @author Doreen Seider
 */
public enum WorkflowState {
    
    /** Initial. */
    INIT("Init"),
    
    /** Starting. */
    STARTING("Starting"),
    
    /** Preparing. */
    PREPARING("Preparing"),
    
    /** Prepared. */
    PREPARED("Prepared"),
    
    /** Running. */
    RUNNING("Running"),
    
    /** Pausing. */
    PAUSING("Pausing"),
    
    /** Paused. */
    PAUSED("Paused"),
    
    /** Running. */
    RESUMING("Resuming"),
    
    /** Finished. */
    FINISHED("Finished"),
    
    /** Canceling. */
    CANCELING("Cancelling"),
    
    /** Canceling after failed. */
    CANCELING_AFTER_FAILED("Cancelling after failure"),
    
    /** Canceled. */
    CANCELLED("Cancelled"),
    
    /** Failed. */
    FAILED("Failed"),
    
    /** Disposing. */
    DISPOSING("Disposing"),
    
    /** Disposed. */
    DISPOSED("Disposed"),
    
    /** Unknown. */
    UNKNOWN("Unknown"),
    
    /** Is alive. */
    IS_ALIVE("Is alive");
    
    private String displayName;
    
    WorkflowState(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * @param workflowState text to check
     * @return <code>true</code> if given text represents a valid {@link WorkflowState}, otherwise <code>false</code>
     */
    public static boolean isWorkflowStateValidAndUserReadable(String workflowState) {
        return isWorkflowStateValid(workflowState) && !workflowState.equals(IS_ALIVE.name());
    }
    
    /**
     * @param workflowState {@link WorkflowState} to check
     * @return <code>true</code> if given text represents a valid {@link WorkflowState}, otherwise <code>false</code>
     */
    public static boolean isWorkflowStateUserReadable(WorkflowState workflowState) {
        return !workflowState.equals(IS_ALIVE);
    }
    
    /**
     * @param workflowState text to check
     * @return <code>true</code> if given text represents a valid {@link WorkflowState}, otherwise <code>false</code>
     */
    public static boolean isWorkflowStateValid(String workflowState) {
        try {
            valueOf(workflowState);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
}
