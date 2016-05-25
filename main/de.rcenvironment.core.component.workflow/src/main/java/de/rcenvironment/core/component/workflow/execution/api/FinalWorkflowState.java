/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

/**
 * The possible end states of an executed workflow. This is a subset of {@link WorkflowState}.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public enum FinalWorkflowState {

    /** Finished. */
    FINISHED("Finished"),
    
    /** Canceled. */
    CANCELLED("Cancelled"),
    
    /** Failed. */
    FAILED("Failed");
    
    private String displayName;
    
    FinalWorkflowState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @param workflowState {@link WorkflowState} to check
     * @return <code>true</code> if {@link WorkflowState} equals (string compare) one on the {@link FinalWorkflowState}s, otherwise
     *         <code>false</code>
     */
    public static boolean isFinalWorkflowState(WorkflowState workflowState) {
        return workflowState.name().equals(FINISHED.name())
            || workflowState.name().equals(CANCELLED.name())
            || workflowState.name().equals(FAILED.name());
    }

}
