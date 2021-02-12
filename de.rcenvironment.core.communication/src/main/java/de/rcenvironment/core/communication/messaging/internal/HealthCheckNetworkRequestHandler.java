/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;

/**
 * Handler for incoming connection health check requests.
 * 
 * @author Robert Mischke
 */
public class HealthCheckNetworkRequestHandler implements NetworkRequestHandler {

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, InstanceNodeSessionId lastHopNodeId) {
        // send back content token
        return NetworkResponseFactory.generateSuccessResponse(request, request.getContentBytes());
    }
}
