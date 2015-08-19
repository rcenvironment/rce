/*
 * Copyright (C) 2006-2014 DLR, Germany
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
public enum FinalComponentState {

    /** Finished. */
    FINISHED("Finished"),
    
    /** Finished, but has not been executed, although inputs are connected. */
    FINISHED_WITHOUT_EXECUTION("Finished without any execution"),
    
    /** Failed. */
    FAILED("Failed"),
    
    /** Canceled. */
    CANCELLED("Cancelled");
    
    private String displayName;
    
    private FinalComponentState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
