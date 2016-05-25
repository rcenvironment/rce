/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;


/**
 * Enumeration of {@link Component} states.
 *
 * @author Doreen Seider
 */
public enum ComponentState {
    
    /** Initial. */
    INIT("Init"),
    
    /** Preparing. */
    PREPARING("Preparing"),
    
    /** Prepared. */
    PREPARED("Prepared"),
    
    /** Starting. */
    STARTING("Starting"),
    
    /** Waiting. */
    WAITING("Waiting for resources"),
    
    /** Running. */
    PROCESSING_INPUTS("Processing inputs"),
    
    /** Idling. */
    IDLING("Idling"),
    
    /** Finished. */
    FINISHED("Finished"),
    
    /** Resetting. */
    RESETTING("Resetting"),
    
    /** Reset. */
    IDLING_AFTER_RESET("Idling after reset"),
    
    /** Finished, but has not been executed, although inputs are connected. */
    FINISHED_WITHOUT_EXECUTION("Finished without any execution"),
    
    /** Failed. */
    FAILED("Failed"),
    
    /** Pausing. */
    PAUSING("Pausing"),
    
    /** Paused. */
    PAUSED("Paused"),
    
    /** Resuming. */
    RESUMING("Resuming"),
    
    /** Cancelling. */
    CANCELLING("Cancelling"),
    
    /** Cancelling after failure. */
    CANCELLING_AFTER_FAILURE("Cancelling after failure"),

    /** Canceled. */
    CANCELED("Cancelled"),
    
    /** Tearing down. */
    TEARING_DOWN("Tearing down"),
    
    /** Disposing. */
    DISPOSING("Disposing"),
    
    /** Disposed. */
    DISPOSED("Disposed"),
    
    /** Unkown. */
    UNKNOWN("Unknown");
    
    private String displayName;
    
    ComponentState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

}
