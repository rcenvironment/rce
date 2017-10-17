/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
