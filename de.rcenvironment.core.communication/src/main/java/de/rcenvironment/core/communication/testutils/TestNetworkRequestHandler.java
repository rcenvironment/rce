/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.messaging.internal.NetworkRequestUtils;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;

/**
 * A {@link NetworkRequestHandler} for integration tests. It expects String payloads and responds to them with Strings of a certain,
 * predictable pattern.
 * 
 * @author Robert Mischke
 */
public class TestNetworkRequestHandler implements NetworkRequestHandler {

    private InstanceNodeSessionId ownNodeId;

    public TestNetworkRequestHandler(InstanceNodeSessionId ownNodeId) {
        this.ownNodeId = ownNodeId;
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, InstanceNodeSessionId lastHopNodeId) throws InternalMessagingException {
        Serializable content = NetworkRequestUtils.deserializeWithExceptionHandling(request);
        if (!(content instanceof String)) {
            throw new RuntimeException("Test request handler received a non-string request: " + content);
        }
        try {
            return NetworkResponseFactory.generateSuccessResponse(request, getTestResponse((String) content, ownNodeId));
        } catch (SerializationException e) {
            throw new InternalMessagingException("Failed to serialize the result of  test call " + content, e);
        }
    }

    /**
     * The generation method for response strings. Tests should call this method to determine the expected response, instead of using
     * hard-coded strings.
     * 
     * @param content the received request content
     * @param respondingNodeId the id of the node generating the response
     * @return the response string
     */
    public static String getTestResponse(String content, InstanceNodeSessionId respondingNodeId) {
        return content + ".response.from=" + respondingNodeId.getInstanceNodeSessionIdString();
    }
}
