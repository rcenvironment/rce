/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Manages component executions.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionService {

    /**
     * Initializes a new component execution.
     * 
     * @param executionContext {@link ComponentExecutionContext} providing information about the execution.
     * @param authToken token which authorizes the caller to execute the component
     * @param referenceTimestamp current timestamp on workflow node
     * @return execution identifier of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     * @throws ComponentExecutionException if initializing the component failed
     */
    String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp)
        throws CommunicationException, ComponentExecutionException;

    /**
     * Prepares a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void prepare(String executionId, NodeIdentifier node) throws CommunicationException;

    /**
     * Starts a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void start(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Pauses a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void pause(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Resumes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void resume(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * Cancels a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void cancel(String executionId, NodeIdentifier node) throws CommunicationException;

    /**
     * Disposes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws CommunicationException if communication error occurs (cannot occur if component runs locally)
     */
    void dispose(String executionId, NodeIdentifier node) throws CommunicationException;

    /**
     * Gets current component state.
     * 
     * @param executionId execution identifier (part of {@link ComponentExecutionInformation}) of the component to get the state for
     * @param node the node of the component controller
     * @return {@link ComponentState}
     * @throws CommunicationException if communication error occurs (cannot occur if controller and components run locally)
     */
    ComponentState getComponentState(String executionId, NodeIdentifier node) throws CommunicationException;
    
    /**
     * @return {@link WorkflowExecutionInformation} objects of all active and local workflows
     */
    Set<ComponentExecutionInformation> getLocalComponentExecutionInformations();

}
