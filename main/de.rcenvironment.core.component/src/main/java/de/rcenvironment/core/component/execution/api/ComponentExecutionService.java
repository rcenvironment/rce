/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Manages component executions.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionService  {

    /**
     * Initializes a new component execution.
     * 
     * @param executionContext {@link ComponentExecutionContext} providing information about the execution.
     * @param authToken token which authorizes the caller to execute the component
     * @param referenceTimestamp current timestamp on workflow node
     * @return execution identifier of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ComponentExecutionException if initializing the component failed
     */
    String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp)
        throws ComponentExecutionException, RemoteOperationException;

    /**
     * Prepares a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void prepare(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Starts a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void start(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Pauses a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void pause(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Resumes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void resume(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Cancels a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void cancel(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Disposes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void dispose(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Gets current component state.
     * 
     * @param executionId execution identifier (part of {@link ComponentExecutionInformation}) of the component to get the state for
     * @param node the node of the component controller
     * @return {@link ComponentState}
     * @throws RemoteOperationException if communication error occurs (cannot occur if controller and components run locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    ComponentState getComponentState(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException;
    
    /**
     * @return {@link WorkflowExecutionInformation} objects of all active and local workflows
     */
    Set<ComponentExecutionInformation> getLocalComponentExecutionInformations();
    
}
