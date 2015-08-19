/*
 * Copyright (C) 2006-2014 DLR, Germany
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
    CANCELLED("Cancelled"),

    /** Failed. */
    FAILED("Failed"),

    /** Corrupted. */
    CORRUPTED("Corrupted");

    private String displayName;

    private FinalWorkflowState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
