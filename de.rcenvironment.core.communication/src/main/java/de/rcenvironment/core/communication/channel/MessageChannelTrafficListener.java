/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;

/**
 * Callback interface for messaging events; intended for monitoring and testing.
 * 
 * @author Robert Mischke
 */
public interface MessageChannelTrafficListener {

    /**
     * Called when the {@link MessageChannelService} has sent a {@link NetworkRequest}.
     * 
     * @param request the request
     */
    void onRequestSentIntoChannel(NetworkRequest request);

    /**
     * Called when the {@link MessageChannelService} has received a {@link NetworkRequest}, but has not processed it yet.
     * 
     * @param request the received request
     * @param sourceId the instance session id of the last hop this request was received from
     */
    void onRequestReceivedFromChannel(NetworkRequest request, InstanceNodeSessionId sourceId);

    /**
     * Called after the connection service has processed a received {@link NetworkRequest}, and is about to send the generated response back
     * to the caller.
     * 
     * @param response the generated response; may be null if no handler could process the request
     * @param request the received request
     * @param sourceId the id of the last hop the request was received from
     */
    void onResponseSentIntoChannel(NetworkResponse response, NetworkRequest request, InstanceNodeSessionId sourceId);

    // TODO review: add onRequestGenerated/onResponseReceived callbacks? trigger on each hop or only
    // on start/end of route? add source/destination parameters? -- misc_ro
}
