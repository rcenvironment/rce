/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandlerMap;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.internal.PayloadTestFuzzer;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;
import de.rcenvironment.core.utils.common.LogUtils;

/**
 * The default {@link MessageEndpointHandler} implementation used in RCE instances.
 * 
 * @author Robert Mischke
 */
public class MessageEndpointHandlerImpl implements MessageEndpointHandler {

    private final Map<String, NetworkRequestHandler> requestHandlerMap = new HashMap<String, NetworkRequestHandler>();

    private final NodeIdentifierService nodeIdentifierService;

    private final Log log = LogFactory.getLog(getClass());

    public MessageEndpointHandlerImpl(NodeIdentifierService nodeIdentifierService) {
        this.nodeIdentifierService = nodeIdentifierService;
    }

    @Override
    public NetworkResponse onRequestArrivedAtDestination(NetworkRequest request) {
        // find matching handler
        // TODO get rid of synchronization?
        NetworkRequestHandler handler;
        synchronized (requestHandlerMap) {
            handler = requestHandlerMap.get(request.getMessageType());
        }

        // actually *handle* the message outside the synchronized block
        if (handler != null) {
            try {
                // trigger deserialization here to catch errors early and in a defined place - misc_ro
                try {

                    request.getDeserializedContent();
                } catch (SerializationException e) {
                    throw new InternalMessagingException("Error deserializing request body", e);
                }

                try {
                    // FIXME restore or remove "previous hop" parameter
                    InstanceNodeSessionId prevHopId = null;
                    return handler.handleRequest(request, prevHopId);
                } catch (RuntimeException e) {
                    throw new InternalMessagingException("Uncaught RuntimeException while handling remote request", e);
                }
            } catch (InternalMessagingException e) {
                return logAndWrapLowLevelException(request, e);
            }
        } else {
            Serializable loggableContent;
            try {
                loggableContent = request.getDeserializedContent();
            } catch (SerializationException e) {
                // used for logging only
                loggableContent = "Failed to deserialize content: " + e;
            }
            String errorId =
                LogUtils.logErrorAndAssignUniqueMarker(log, "No request handler matched for message type '" + request.getMessageType()
                    + "'; string representation of request: " + NetworkFormatter.message(loggableContent, request.accessRawMetaData()));

            return NetworkResponseFactory.generateResponseForInternalErrorAtRecipient(request, errorId);
        }
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

    private NetworkResponse logAndWrapLowLevelException(NetworkRequest request, InternalMessagingException e) {
        @SuppressWarnings("unused")// suppress Eclipse warning when the ENABLED constant is false - misc_ro
        boolean compressStacktrace =
            PayloadTestFuzzer.ENABLED && e.getCause() != null && e.getCause().getClass() == SerializationException.class;
        final String errorId;
        if (compressStacktrace) {
            errorId = LogUtils.logErrorAndAssignUniqueMarker(log, e.toString());
        } else {
            if (e.getCause() != null) {
                errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log, e.getMessage(), e.getCause());
            } else {
                errorId = LogUtils.logErrorAndAssignUniqueMarker(log, e.getMessage());
            }
        }
        NetworkResponse response = NetworkResponseFactory.generateResponseForInternalErrorAtRecipient(request, errorId);
        return response;
    }
}
