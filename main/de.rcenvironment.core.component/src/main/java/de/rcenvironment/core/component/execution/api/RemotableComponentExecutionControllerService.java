/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote-accessible methods for component execution control.
 *
 * @author Doreen Seider
 */
@RemotableService
public interface RemotableComponentExecutionControllerService extends RemotableExecutionControllerService {

    /**
     * Creates a new {@link ComponentExecutionController} for the component represented be {@link ComponentExecutionContext}.
     * 
     * @param executionContext {@link ComponentExecutionContext} of the component to execute
     * @param executionAuthToken the auth token which authorizes the execution
     * @param referenceTimestamp current timestamp on workflow node
     * @return execution identifier of the component instance
     * @throws ComponentExecutionException if instantiating component failed
       @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     */
    String createExecutionController(ComponentExecutionContext executionContext, String executionAuthToken, Long referenceTimestamp)
        throws ComponentExecutionException, RemoteOperationException;

    /**
     * @param executionId execution identifier of the component instance (provided by {@link ComponentExecutionInformation})
     * @return {@link ComponentState} of the component
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    ComponentState getComponentState(String executionId) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Prepares a component.
     * 
     * @param executionId execution identifier of the component instance
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void performPrepare(String executionId) throws ExecutionControllerException, RemoteOperationException;

    /**
     * Add a new execution auth token.
     * @param authToken new auth token, which authorizes for execution
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     */
    void addComponentExecutionAuthToken(String authToken) throws RemoteOperationException;
    
    /**
     * Called if asynchronous sending of an {@link EndpointDatum} failed.
     * 
     * @param executionId execution identifier of the component that requested sending the {@link EndpointDatum}
     * @param endpointDatum affected {@link EndpointDatum}
     * @param e {@link RemoteOperationException} thrown
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onSendingEndointDatumFailed(String executionId, EndpointDatum endpointDatum, RemoteOperationException e) 
        throws ExecutionControllerException, RemoteOperationException;

}
