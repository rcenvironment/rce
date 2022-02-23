/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging;

import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkRequest;

/**
 * Service interface for receivers of routed or non-routed {@link NetworkRequest}s. Each request
 * must be answered with a {@link NetworkResponse}; failures and exceptions must be wrapped
 * accordingly.
 * 
 * @author Robert Mischke
 */
public interface MessageEndpointHandler {

    /**
     * Called for every received {@link NetworkRequest} that has the local node as its final
     * destination.
     * 
     * @param request the received request
     * @return the locally generated {@link NetworkResponse}
     */
    NetworkResponse onRequestArrivedAtDestination(NetworkRequest request);

    /**
     * Registers a handler for a given message type.
     * 
     * @param messageType the message type; see {@link ProtocolConstants}
     * @param handler the handler instance
     */
    void registerRequestHandler(String messageType, NetworkRequestHandler handler);

    /**
     * Registers one or more handlers for their given message types.
     * 
     * @param newMappings the type/handler mappings to add
     */
    void registerRequestHandlers(NetworkRequestHandlerMap newMappings);
}
