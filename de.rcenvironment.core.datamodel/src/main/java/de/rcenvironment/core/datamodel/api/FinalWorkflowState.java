/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

/**
 * States a workflow can have after it has been terminated.
 * 
 * @author Doreen Seider
 */
public enum FinalWorkflowState {

    /** Finished. */
    FINISHED("Finished"),

    /** Cancelled. */
    CANCELLED("Canceled"),

    /** Failed. */
    FAILED("Failed"),
    
    /** Failed. */
    RESULTS_REJECTED("Verification failed"),

    /** Corrupted. */
    CORRUPTED("Corrupted");

    private String displayName;

    FinalWorkflowState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
