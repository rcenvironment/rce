/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
    
    /** Waiting for resources. */
    WAITING_FOR_RESOURCES("Waiting for resources"),
    
    /** Waiting for approval. */
    WAITING_FOR_APPROVAL("Waiting for approval"),
    
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
    
    /** Canceling. */
    CANCELLING("Canceling"),
    
    /** Canceling after failure. */
    CANCELLING_AFTER_FAILURE("Canceling after failure"),
    
    /** Canceled. */
    CANCELED("Canceled"),
    
    /** Tearing down. */
    TEARING_DOWN("Tearing down"),
    
    /** Disposing. */
    DISPOSING("Disposing"),
    
    /** Disposed. */
    DISPOSED("Disposed"),

    /** Verification failed. */
    RESULTS_REJECTED("Results rejected"),
    
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
