/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote-accessible methods for component execution control.
 *
 * @author Doreen Seider
 * @author Robert Mischke
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
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
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
     * @param executionId execution identifier of the related component
     * @param verificationToken verification token used to verify results of a certain component run
     * @param verified <code>true</code> if results are verified otherwise <code>false</code>
     * @return <code>true</code> if verification result could be applied successfully, otherwise <code>false</code> (most likely reason:
     *         invalid verification token or component not in state {@link ComponentState#WAITING_FOR_APPROVAL} (anymore))
     * @throws RemoteOperationException if called from remote and remote method call failed
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    Boolean performVerifyResults(String executionId, String verificationToken, Boolean verified)
        throws ExecutionControllerException, RemoteOperationException;

    /**
     * Called if asynchronous sending of an {@link EndpointDatum} failed.
     * 
     * @param executionId execution identifier of the component that requested sending the {@link EndpointDatum}
     * @param serializedEndpointDatum affected {@link EndpointDatum} given as serialized string (see {@link EndpointDatumSerializer})
     * @param e {@link RemoteOperationException} thrown
     * @throws RemoteOperationException if called from remote and remote method call failed (cannot occur if controller and components run
     *         locally)
     * @throws ExecutionControllerException if {@link ExecutionController} is not available (anymore)
     */
    void onSendingEndointDatumFailed(String executionId, String serializedEndpointDatum, RemoteOperationException e)
        throws ExecutionControllerException, RemoteOperationException;

}
