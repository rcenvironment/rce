/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * This interface describes the methods that the different service call sender has to implement.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
// replaced by RemoteServiceCallService
@Deprecated
public interface ServiceCallSender {

    /**
     * This methods initializes the connection parameters required for the connection. It must be called before send is called.
     * 
     * @param contact Information that describes how to contact the communication partner.
     * @throws CommunicationException Thrown if the communication failed.
     */
    void initialize(NetworkContact contact) throws CommunicationException;

    /**
     * This method establishes a connection to another RCE platform and calls the method described in the {@link ServiceCallRequest}. It
     * returns the result as {@link ServiceCallResult}.
     * 
     * TODO specify if all implementations are expected to be thread-safe - misc_ro
     * 
     * @param serviceCallRequest The {@link ServiceCallRequest} object describing the remote service method call.
     * @return The result of the remote service method call call as {@link ServiceCallResult}.
     * @throws RemoteOperationException Thrown if the communication failed.
     */
    ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws RemoteOperationException;

}
