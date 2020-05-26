/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging;

import java.util.HashMap;

/**
 * Simple holder for message type / handler mappings.
 * 
 * @author Robert Mischke
 */
@SuppressWarnings("serial")
public class NetworkRequestHandlerMap extends HashMap<String, NetworkRequestHandler> {

    public NetworkRequestHandlerMap() {}

    /**
     * Convenience constructor for a single handler mapping.
     * 
     * @param messageType the message type to handle
     * @param handler the handler
     */
    public NetworkRequestHandlerMap(String messageType, NetworkRequestHandler handler) {
        put(messageType, handler);
    }
}
