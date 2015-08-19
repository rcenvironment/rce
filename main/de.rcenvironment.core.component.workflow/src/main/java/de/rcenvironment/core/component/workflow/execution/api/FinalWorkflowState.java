/*
 * Copyright (C) 2006-2015 DLR, Germany
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
    
    private FinalWorkflowState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

}
