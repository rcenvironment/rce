/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

/**
 * Callback class used to callback workflow {@link ExecutionController}s, mainly by associated {@link ComponentExecutionController} objects.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionControllerCallback extends BatchedConsoleRowsProcessor {
    
    /**
     * Called if component changed its state.
     * 
     * @param compExeId execution identifier of component executed
     * @param oldState old {@link ComponentState}
     * @param newState new {@link ComponentState}
     * @param executionCount current execution count
     * @param executionCountOnResets execution count on component resets
     */
    // introduced as null parameter can not be passed to a remote accessible method (the appropriate method won't be find on the other node)
    void onComponentStateChanged(String compExeId, ComponentState oldState, ComponentState newState,
        Integer executionCount, String executionCountOnResets);
    
    /**
     * Called if component changed its state.
     * 
     * @param compExeId execution identifier of component executed
     * @param oldState old {@link ComponentState}
     * @param newState new {@link ComponentState}
     * @param executionCount current execution count
     * @param executionCountOnResets execution count on component resets
     * @param t {@link Throwable} of new state was caused by an exception
     */
    void onComponentStateChanged(String compExeId, ComponentState oldState, ComponentState newState,
        Integer executionCount, String executionCountOnResets, Throwable t);

    /**
     * Called on for input values passed to an upcoming call of {@link Component#processInputs()}.
     *  
     * @param serializedEndpointDatum serialized {@link EndpointDatum} read
     */
    void onInputProcessed(String serializedEndpointDatum);
    
    /**
     * Called if a component said that he is still alive.
     *  
     * @param executionIdentifier execution identifier of the calling component
     */
    void onComponentHeartbeatReceived(String executionIdentifier);

}
