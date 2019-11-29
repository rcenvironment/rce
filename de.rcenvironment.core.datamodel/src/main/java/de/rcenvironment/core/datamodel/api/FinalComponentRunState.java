/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.api;

/**
 * States a component can have if workflow has been terminated.
 * 
 * @author Doreen Seider
 */
public enum FinalComponentRunState {

    /** Finished. */
    FINISHED("Finished"),
    
    /** Failed. */
    FAILED("Failed"),
    
    /** Canceled. */
    CANCELLED("Cancelled"),
    
    /** Results rejected. */
    RESULTS_REJECTED("Results rejected"),

    /** Results approved. */
    RESULTS_APPROVED("Results approved");

    private String displayName;
    
    FinalComponentRunState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
