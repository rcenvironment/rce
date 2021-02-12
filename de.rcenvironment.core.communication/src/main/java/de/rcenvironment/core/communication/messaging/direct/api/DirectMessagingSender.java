/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.direct.api;

import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * A service for sending single messages and receiving responses to/from a direct network neighbor.
 * 
 * @author Robert Mischke
 */
public interface DirectMessagingSender {

    /**
     * Sends the given request into the given {@link MessageChannel}, using the default timeout. The response is returned via the provided
     * {@link NetworkResponseHandler}.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channel the {@link MessageChannel} to send into
     * @param responseHandler the response handler
     */
    void sendDirectMessageAsync(NetworkRequest request, MessageChannel channel, NetworkResponseHandler responseHandler);

    /**
     * Sends the given request into the {@link MessageChannel} identified by the given id, using a custom timeout. The response is returned
     * via the provided {@link NetworkResponseHandler}.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channel the {@link MessageChannel} to send into
     * @param responseHandler the response handler
     * @param timeoutMsec the timeout in msec
     */
    void sendDirectMessageAsync(NetworkRequest request, MessageChannel channel, NetworkResponseHandler responseHandler, int timeoutMsec);

    /**
     * Sends the given request into the {@link MessageChannel} identified by the given id, using the default timeout. The response is
     * returned via the provided {@link NetworkResponseHandler}.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channelId the id of the {@link MessageChannel} to send into
     * @param responseHandler the response handler
     */
    void sendDirectMessageAsync(NetworkRequest request, String channelId, NetworkResponseHandler responseHandler);

    /**
     * Convenience method that sends a request containing the given payload and metadata into the given {@link MessageChannel}, and returns
     * the received response.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channel the connection to send to
     * @param timeoutMsec the timeout in msec
     * @return the associated {@link NetworkResponse}
     */
    NetworkResponse sendDirectMessageBlocking(NetworkRequest request, MessageChannel channel, int timeoutMsec);

}
