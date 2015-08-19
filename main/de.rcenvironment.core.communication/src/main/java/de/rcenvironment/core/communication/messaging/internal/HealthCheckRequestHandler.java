/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;

/**
 * Handler for incoming connection health check requests.
 * 
 * @author Robert Mischke
 */
public class HealthCheckRequestHandler implements NetworkRequestHandler {

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, NodeIdentifier lastHopNodeId) {
        // send back content token
        return NetworkResponseFactory.generateSuccessResponse(request, request.getContentBytes());
    }
}
