/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.api;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;

/**
 * Service for performing remote {@link ServiceCallRequest}s, and returning the received {@link ServiceCallResult}s.
 * 
 * Note that despite its name, this is not a "remote service" in the usual RCE terminology, but part of the infrastructure that performs
 * remote service calls.
 * 
 * @author Robert Mischke
 */
public interface RemoteServiceCallSenderService {

    /**
     * Performs a remote service call and returns the received {@link ServiceCallResult}. The destination node is read from the provided
     * {@link ServiceCallRequest}.
     * 
     * @param serviceCallRequest the call request
     * @return the call result
     * @throws CommunicationException on failure
     */
    ServiceCallResult performRemoteServiceCall(ServiceCallRequest serviceCallRequest) throws CommunicationException;

    /**
     * Performs a remote service call, parses the received {@link ServiceCallResult} and acts in place of the invoked method. If the method
     * completed normally, the (null or non-null) return value is returned, if there was an error, the respective Exception is thrown. The
     * destination node is read from the provided {@link ServiceCallRequest}.
     * 
     * @param serviceCallRequest the call request
     * @return the method return value on success (may be null)
     * @throws Throwable a {@link RemoteOperationException} or a checked method exception on failure
     */
    Object performRemoteServiceCallAsProxy(ServiceCallRequest serviceCallRequest) throws Throwable;
}
