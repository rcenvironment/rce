/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandlerMap;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.impl.NetworkResponseImpl;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;

/**
 * The default {@link MessageEndpointHandler} implementation used in RCE instances.
 * 
 * @author Robert Mischke
 */
public class MessageEndpointHandlerImpl implements MessageEndpointHandler {

    private final Map<String, NetworkRequestHandler> requestHandlerMap = new HashMap<String, NetworkRequestHandler>();

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public NetworkResponse onRequestArrivedAtDestination(NetworkRequest request) {

        // find matching handler
        // TODO get rid of synchronization?
        NetworkRequestHandler handler;
        synchronized (requestHandlerMap) {
            handler = requestHandlerMap.get(request.getMessageType());
        }

        // actually *handle* the message outside the synchronized block
        NetworkResponse response = null;
        if (handler != null) {
            // trigger deserialization here to catch errors early and in a defined place - misc_ro
            try {
                request.getDeserializedContent();
            } catch (SerializationException e) {
                log.warn("Error deserializing request body", e);
                return NetworkResponseFactory.generateResponseForExceptionAtDestination(request, e);
            }

            try {
                // FIXME restore or remove "previous hop" parameter
                NodeIdentifier prevHopId = null;
                response = handler.handleRequest(request, prevHopId);
            } catch (RuntimeException | SerializationException | CommunicationException e) {
                response = logAndWrapException(request, e);
            }
        } else {
            Serializable loggableContent;
            try {
                loggableContent = request.getDeserializedContent();
            } catch (SerializationException e) {
                // used for logging only
                loggableContent = "Failed to deserialize content: " + e;
            }
            log.warn("No request handler matched for message type '" + request.getMessageType()
                + "', generating failure response; string representation of request: "
                + NetworkFormatter.message(loggableContent, request.accessRawMetaData()));

            response = new NetworkResponseImpl(null, request.getRequestId(), ProtocolConstants.ResultCode.NO_MATCHING_HANDLER);
        }

        return response;
    }

    @Override
    public void registerRequestHandler(String messageType, NetworkRequestHandler handler) {
        synchronized (requestHandlerMap) {
            requestHandlerMap.put(messageType, handler);
        }
    }

    @Override
    public void registerRequestHandlers(NetworkRequestHandlerMap newMappings) {
        synchronized (requestHandlerMap) {
            requestHandlerMap.putAll(newMappings);
        }
    }

    private NetworkResponse logAndWrapException(NetworkRequest request, Throwable e) {
        NetworkResponse response;
        log.warn("Returning an exception response for an incoming request:", e);
        response = NetworkResponseFactory.generateResponseForExceptionAtDestination(request, e);
        return response;
    }
}
