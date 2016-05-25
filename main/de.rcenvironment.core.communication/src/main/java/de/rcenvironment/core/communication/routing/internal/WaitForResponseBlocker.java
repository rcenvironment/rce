/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * A helper class for waiting asynchronously for a single {@link NetworkResponse}. Used for connecting callback- and Future-based call
 * variants.
 * 
 * @author Robert Mischke
 */
public class WaitForResponseBlocker implements NetworkResponseHandler {

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(WaitForResponseBlocker.class);

    // this class is instantiated frequently, so save the overhead of logger fetching with a static instance - misc_ro
    private static final Log sharedLog = LogFactory.getLog(WaitForResponseBlocker.class);

    private final NetworkRequest request;

    private final NodeIdentifier eventNodeId;

    private final CountDownLatch responseReceivedLatch;

    private NetworkResponse response;

    private volatile String logMarker = null;

    public WaitForResponseBlocker(NetworkRequest request, NodeIdentifier localNodeId) {
        this.request = request;
        this.eventNodeId = localNodeId;
        this.responseReceivedLatch = new CountDownLatch(1);
    }

    @Override
    public void onResponseAvailable(NetworkResponse receivedResponse) {
        this.response = receivedResponse;
        responseReceivedLatch.countDown();
    }

    /**
     * Waits until a {@link NetworkResponse} was received via {@link #onResponseAvailable(NetworkResponse)}, or until the given timeout has
     * elapsed. In both cases, a {@link NetworkResponse} is returned; there are no timeout exceptions.
     * 
     * @param requestTimeoutMsec the maximum time to wait, in msec
     * @return the result wrapper; either the actual response, or a timeout representation
     */
    public NetworkResponse await(long requestTimeoutMsec) {
        if (VERBOSE_LOGGING) {
            sharedLog.debug("Waiting for response callback (" + logMarker + ")");
        }
        try {
            if (responseReceivedLatch.await(requestTimeoutMsec, TimeUnit.MILLISECONDS)) {
                if (VERBOSE_LOGGING) {
                    sharedLog.debug("Received response callback (" + logMarker + ")");
                }
                return response;
            } else {
                sharedLog.debug("Timeout reached while waiting for a network response to request " + request.getRequestId());
                // TODO test/verify that a "null" cause does not trigger downstream NPEs
                return NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, eventNodeId);
            }
        } catch (InterruptedException e) {
            String errorId = LogUtils.logErrorAndAssignUniqueMarker(sharedLog,
                "Interrupted while waiting for a network response: " + e.toString()); // stacktrace irrelevant
            return NetworkResponseFactory.generateResponseForErrorDuringDelivery(request, eventNodeId, errorId);
        }
    }

    public void setLogMarker(String logMarker) {
        this.logMarker = logMarker;
    }
}
