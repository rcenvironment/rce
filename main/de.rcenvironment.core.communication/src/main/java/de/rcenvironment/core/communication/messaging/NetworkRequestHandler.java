/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;

/**
 * Callback interface for incoming connection-level requests.
 * 
 * @author Robert Mischke
 */
public interface NetworkRequestHandler {

    /**
     * Handles {@link NetworkRequest}s of certain message type. Each request must result in a ready-to-send {@link NetworkResponse} or a
     * thrown exception; returning "null" is not allowed.
     * 
     * @param request the received request
     * @param lastHopNodeId the node id of the immediate neighbor the request was received from
     * @return the generated response, if the request could be handled; null, otherwise
     * @throws InternalMessagingException on unhandled errors
     */
    NetworkResponse handleRequest(NetworkRequest request, InstanceNodeSessionId lastHopNodeId) throws InternalMessagingException;
}
