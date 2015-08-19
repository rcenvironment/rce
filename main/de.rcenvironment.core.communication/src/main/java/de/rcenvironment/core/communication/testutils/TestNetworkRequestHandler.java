/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;

/**
 * A {@link NetworkRequestHandler} for integration tests. It expects String payloads and responds to
 * them with Strings of a certain, predictable pattern.
 * 
 * @author Robert Mischke
 */
public class TestNetworkRequestHandler implements NetworkRequestHandler {

    private NodeIdentifier ownNodeId;

    public TestNetworkRequestHandler(NodeIdentifier ownNodeId) {
        this.ownNodeId = ownNodeId;
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, NodeIdentifier lastHopNodeId) throws SerializationException {
        Serializable content = request.getDeserializedContent();
        if (!(content instanceof String)) {
            throw new RuntimeException("Test request handler received a non-string request: " + content);
        }
        return NetworkResponseFactory.generateSuccessResponse(request, getTestResponse((String) content, ownNodeId));
    }

    /**
     * The generation method for response strings. Tests should call this method to determine the
     * expected response, instead of using hard-coded strings.
     * 
     * @param content the received request content
     * @param respondingNodeId the id of the node generating the response
     * @return the response string
     */
    public static String getTestResponse(String content, NodeIdentifier respondingNodeId) {
        return content + ".response.from=" + respondingNodeId.getIdString();
    }
}
