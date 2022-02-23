/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * The receiving end of a Reliable RPC Stream. It processes incoming network requests, checking them for repetition, dispatching each
 * individual request to the appropriate local service exactly once, and caching the result in case it gets lost and needs to be re-sent.
 *
 * @author Robert Mischke
 */
public class ReliableRPCStreamReceiver {

    private final String streamId;

    private final RemoteServiceCallHandlerService serviceCallHandlerService;

    private final AtomicInteger concurrencyCounter = new AtomicInteger(0);

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("RemoteServiceCalls");

    private final Object sequentialHandlingLock = new Object();

    private ServiceCallResult lastProcessedResult;

    private long lastProcessedSequenceNumber; // 0 = nothing processed yet

    private final Log log = LogFactory.getLog(getClass());

    public ReliableRPCStreamReceiver(String streamId, RemoteServiceCallHandlerService serviceCallHandlerService) {
        this.streamId = streamId;
        this.serviceCallHandlerService = Objects.requireNonNull(serviceCallHandlerService);
    }

    public String getStreamId() {
        return streamId;
    }

    /**
     * Handles an incoming network request; see main class JavaDoc for the general process.
     * 
     * @param serviceCallRequest the incoming request
     * @return the result to send back (which may be cached from an equivalent earlier request)
     * @throws InternalMessagingException on internal, unrecoverable errors
     */
    public ServiceCallResult handle(ServiceCallRequest serviceCallRequest)
        throws InternalMessagingException {

        final long newSequenceNumber = serviceCallRequest.getSequenceNumber();
        if (newSequenceNumber <= 0) { // sanity check
            throw new IllegalStateException();
        }

        if (verboseRequestLoggingEnabled) {
            final int concurrencyLevel = concurrencyCounter.incrementAndGet();
            if (concurrencyLevel > 1) {
                log.debug(StringUtils.format("Handling remote rRPC %d of stream %s, calling %s.%s() at a "
                    + "concurrent call count of %d", newSequenceNumber, serviceCallRequest.getReliableRPCStreamId(),
                    serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), concurrencyLevel));
            }
        }

        try {
            synchronized (sequentialHandlingLock) { // for safety; with well-behaved clients, this is redundant
                if (newSequenceNumber == lastProcessedSequenceNumber) { // can only match if last seqNo is >0, so there is a result
                    log.debug(StringUtils.format(
                        "Received repeated request for rRPC %d of stream %s, calling %s.%s(); sending the cached result again, "
                            + "as it was probably lost in transmission",
                        newSequenceNumber, serviceCallRequest.getReliableRPCStreamId(),
                        serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()));
                    return lastProcessedResult;
                } else if (newSequenceNumber == lastProcessedSequenceNumber + 1) {
                    if (verboseRequestLoggingEnabled) {
                        log.debug(StringUtils.format("Processing sequential remote rRPC %d of stream %s, calling %s.%s()",
                            newSequenceNumber, serviceCallRequest.getReliableRPCStreamId(),
                            serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()));
                    }
                    final ServiceCallResult result = serviceCallHandlerService.dispatchToLocalService(serviceCallRequest);
                    lastProcessedSequenceNumber = newSequenceNumber;
                    lastProcessedResult = result;
                    return result;
                } else {
                    throw new IllegalStateException("Unexpected state: new sequence number is " + newSequenceNumber
                        + ", while the last processed one is " + lastProcessedSequenceNumber);
                }
            }
        } finally {
            if (verboseRequestLoggingEnabled) {
                concurrencyCounter.decrementAndGet();
                log.debug(StringUtils.format("Completed incoming rRPC request %d of stream %s", serviceCallRequest.getSequenceNumber(),
                    streamId));
            }
        }

    }

}
