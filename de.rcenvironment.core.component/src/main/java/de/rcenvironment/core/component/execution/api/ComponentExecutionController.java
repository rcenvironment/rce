/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Component-specific {@link ExecutionController}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionController extends ExecutionController {

    /**
     * Prepares the component.
     */
    void prepare();

    /**
     * @return the {@link ComponentState}
     */
    ComponentState getState();

    /**
     * Calls if an {@link EndpointDatum} was received from a local {@link RemotableEndpointDatumDispatcher}.
     * 
     * @param endpointDatum {@link EndpointDatum} received
     */
    void onEndpointDatumReceived(EndpointDatum endpointDatum);

    /**
     * Called if asynchronous sending of an {@link EndpointDatum} failed.
     * 
     * @param endpointDatum affected {@link EndpointDatum}
     * @param e {@link RemoteOperationException} thrown
     */
    void onSendingEndointDatumFailed(EndpointDatum endpointDatum, RemoteOperationException e);

    /**
     * Cancels the component.
     * 
     * @param timeoutMsec max time to wait
     * @throws InterruptedException if waiting for the component to get cancelled failed
     * @return <code>false</code> if the timeout exceeded, otherwise <code>true</code>
     */
    boolean cancelSync(long timeoutMsec) throws InterruptedException;

    /**
     * @return <code>true</code> if sending heartbeats to workflow controller succeeded, <code>false</code> otherwise
     */
    boolean isWorkflowControllerReachable();

    /**
     * Verifies the results of the last component run if verification was requested.
     * 
     * @param verificationToken verification token used to verify results of a certain component run
     * @param verified <code>true</code> if results are verified otherwise <code>false</code>
     * @return <code>true</code> if verification result could be applied successfully, otherwise <code>false</code> (reason: invalid
     *         verification token)
     */
    boolean verifyResults(String verificationToken, boolean verified);

    /**
     * @return latest verification token if component is in {@link ComponentState#WAITING_FOR_APPROVAL} (or {@link ComponentState#PAUSING}),
     *         otherwise <code>null</code> in case of another {@link ComponentState} or in case no token exists.
     */
    String getVerificationToken();
}
