/*
 * Copyright (C) 2006-2014 DLR, Germany
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
    
    private WorkflowState(String displayName) {
        this.displayName = displayName;
    }
    /**
     * @param state text to check
     * @return <code>true</code> if given text represents a valid {@link WorkflowState}, otherwise <code>false</code>
     */
    public static boolean isWorkflowStateValidAndUserReadable(String state) {
        return isWorkflowStateValid(state) && !state.equals(IS_ALIVE.name());
    }
    
    /**
     * @param state text to check
     * @return <code>true</code> if given text represents a valid {@link WorkflowState}, otherwise <code>false</code>
     */
    public static boolean isWorkflowStateValid(String state) {
        try {
            valueOf(state);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
}
