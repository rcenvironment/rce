/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Manages component executions.
 * 
 * This service allows to use any node in the network that is resolvable.
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
    void prepare(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Starts a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void start(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Pauses a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void pause(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Resumes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void resume(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Cancels a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void cancel(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Disposes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void dispose(String executionId, NetworkDestination node) throws ExecutionControllerException, RemoteOperationException;

    /**
     * @param verificationToken verification token used to verify results of a certain component run
     * @return {@link ComponentExecutionInformation} of the component related to the verification token or <code>null</code> if no one
     *         related was found
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     */
    ComponentExecutionInformation getComponentExecutionInformation(String verificationToken) throws RemoteOperationException;

    /**
     * Verifies the results of the last component run if verification was requested.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @param verificationToken verification token related to the component executed
     * @param verified <code>true</code> if results are verified otherwise <code>false</code>
     * @return <code>true</code> if verification result could be applied successfully, otherwise <code>false</code> (most likely reason:
     *         invalid verification token or component not in state {@link ComponentState#WAITING_FOR_APPROVAL} (anymore))
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    boolean verifyResults(String executionId, ResolvableNodeId node, String verificationToken, boolean verified)
        throws ExecutionControllerException, RemoteOperationException;

    /**
     * Gets current component state.
     * 
     * @param executionId execution identifier (part of {@link ComponentExecutionInformation}) of the component to get the state for
     * @param node the node of the component controller
     * @return {@link ComponentState}
     * @throws RemoteOperationException if communication error occurs (cannot occur if controller and components run locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    ComponentState getComponentState(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException;

    /**
     * @return {@link WorkflowExecutionInformation} objects of all active and local workflows
     */
    Set<ComponentExecutionInformation> getLocalComponentExecutionInformations();

}
