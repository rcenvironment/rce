/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import de.rcenvironment.core.communication.common.CommunicationException;

/**
 * Service for performing remote {@link ServiceCallRequest}s, and returning the received
 * {@link ServiceCallResult}s.
 * 
 * @author Robert Mischke
 */
public interface RemoteServiceCallService {

    /**
     * Performs a remote service call. The destination node is read from the provided
     * {@link ServiceCallRequest}.
     * 
     * @param serviceCallRequest the call request
     * @return the call result
     * @throws CommunicationException on failure
     */
    ServiceCallResult performRemoteServiceCall(ServiceCallRequest serviceCallRequest) throws CommunicationException;
}
