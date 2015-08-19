/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.Collection;

/**
 * Component-specific {@link ExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionControllerService extends ExecutionControllerService {

    /**
     * Creates a new {@link ComponentExecutionController} for the component represented be {@link ComponentExecutionContext}.
     * 
     * @param executionContext {@link ComponentExecutionContext} of the component to execute
     * @param executionAuthToken the auth token which authorizes the execution
     * @param referenceTimestamp current timestamp on workflow node
     * @return execution identifier of the component instance
     * @throws ComponentExecutionException if instantiating component failed
     */
    String createExecutionController(ComponentExecutionContext executionContext, String executionAuthToken, Long referenceTimestamp)
        throws ComponentExecutionException;

    /**
     * @param executionId execution identifier of the component instance (provided by {@link ComponentExecutionInformation})
     * @return {@link ComponentState} of the component
     */
    ComponentState getComponentState(String executionId);

    /**
     * @return {@link ComponentExecutionInformation} objects of all active components
     */
    Collection<ComponentExecutionInformation> getComponentExecutionInformations();

    /**
     * Prepares a component.
     * 
     * @param executionId execution identifier of the component instance
     */
    void performPrepare(String executionId);
    
    /**
     * Add a new execution auth token.
     * @param authToken new auth token, which authorizes for execution
     */
    void addComponentExecutionAuthToken(String authToken);
    
}
