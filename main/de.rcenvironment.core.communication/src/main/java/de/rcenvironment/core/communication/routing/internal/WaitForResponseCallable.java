/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * A helper class for waiting asynchronously for a single {@link NetworkResponse}. Used for connecting callback- and Future-based call
 * variants.
 * 
 * @author Robert Mischke
 */
public class WaitForResponseCallable implements Callable<NetworkResponse>, NetworkResponseHandler {

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(WaitForResponseCallable.class);

    // this class is instantiated frequently, so save the overhead of logger fetching with a static instance - misc_ro
    private static final Log SHARED_LOGGER = LogFactory.getLog(WaitForResponseCallable.class);

    private final NetworkRequest request;

    private final long requestTimeoutMsec;

    private final String eventNodeId;

    private final CountDownLatch responseReceivedLatch;

    private NetworkResponse response;

    private volatile String logMarker = null;

    public WaitForResponseCallable(NetworkRequest request, long requestTimeoutMsec, String eventNodeId) {
        this.request = request;
        this.requestTimeoutMsec = requestTimeoutMsec;
        this.eventNodeId = eventNodeId;
        this.responseReceivedLatch = new CountDownLatch(1);
    }

    @Override
    public void onResponseAvailable(NetworkResponse receivedResponse) {
        this.response = receivedResponse;
        responseReceivedLatch.countDown();
    }

    @Override
    @TaskDescription("Waiting for communication response")
    public NetworkResponse call() throws Exception {
        if (VERBOSE_LOGGING) {
            SHARED_LOGGER.debug("Waiting for response callback (" + logMarker + ")");
        }
        try {
            if (responseReceivedLatch.await(requestTimeoutMsec, TimeUnit.MILLISECONDS)) {
                if (VERBOSE_LOGGING) {
                    SHARED_LOGGER.debug("Received response callback (" + logMarker + ")");
                }
                return response;
            } else {
                SHARED_LOGGER.debug("Timeout reached while waiting for a network response to request " + request.getRequestId());
                // TODO test/verify that a "null" cause does not trigger downstream NPEs
                return NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, eventNodeId, null);
            }
        } catch (InterruptedException e) {
            SHARED_LOGGER.warn("Interrupted while waiting for a network response: " + e.toString()); // stacktrace irrelevant
            return NetworkResponseFactory.generateResponseForTimeoutWaitingForResponse(request, eventNodeId, e);
        }
    }

    public void setLogMarker(String logMarker) {
        this.logMarker = logMarker;
    }
}
