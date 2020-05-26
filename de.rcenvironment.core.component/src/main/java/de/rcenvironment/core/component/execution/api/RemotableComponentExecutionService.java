/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote-accessible methods for component execution.
 *
 * @author Doreen Seider
 */
@RemotableService
public interface RemotableComponentExecutionService {

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
        throws RemoteOperationException, ComponentExecutionException;

    /**
     * Prepares a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void prepare(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Starts a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void start(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Pauses a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void pause(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Resumes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void resume(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Cancels a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void cancel(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Disposes a component.
     * 
     * @param executionId execution identifier of the component executed
     * @param node the hosting node of the component executed
     * @throws RemoteOperationException if communication error occurs (cannot occur if component runs locally)
     */
    void dispose(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

    /**
     * Gets current component state.
     * 
     * @param executionId execution identifier (part of {@link ComponentExecutionInformation}) of the component to get the state for
     * @param node the node of the component controller
     * @return {@link ComponentState}
     * @throws RemoteOperationException if communication error occurs (cannot occur if controller and components run locally)
     */
    ComponentState getComponentState(String executionId, InstanceNodeSessionId node) throws RemoteOperationException;

}
