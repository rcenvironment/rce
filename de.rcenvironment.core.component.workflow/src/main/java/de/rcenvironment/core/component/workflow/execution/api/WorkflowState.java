/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
    CANCELING("Canceling"),
    
    /** Canceling after failed. */
    CANCELING_AFTER_FAILED("Canceling after failure"),
    
    /** Canceling after verification failed. */
    CANCELING_AFTER_RESULTS_REJECTED("Canceling after results rejected"),
    
    /** Canceled. */
    CANCELLED("Canceled"),
    
    /** Failed. */
    FAILED("Failed"),
    
    /** Verification failed. */
    RESULTS_REJECTED("Results rejected"),
    
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

    public boolean isResumable() {
        return this.equals(WorkflowState.PAUSED);
    }
    
    public boolean isPausable() {
        return this.equals(WorkflowState.RUNNING);
    }
    
    public boolean isCancellable() {
        return this.equals(WorkflowState.RUNNING) || this.equals(WorkflowState.PAUSED);
    }
    
}
