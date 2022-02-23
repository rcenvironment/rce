/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * The request sending end of a Reliable RPC Stream. It attempts to perform a normal RPC, but in case of network errors, the request may be
 * repeated depending on the nature of the error. This process is transparent to the initiator/caller of the Reliable RPC (or rRPC for
 * short).
 *
 * @author Robert Mischke
 */
public class ReliableRPCStreamSender {

    private static final int RETRY_WAIT_TIME_SECONDS = 5;

    private final ReliableRPCStreamHandle streamHandle;

    private final String streamId;

    private final MessageRoutingService routingService;

    private final AtomicLong sequenceNumberGenerator = new AtomicLong(0);

    private final AtomicInteger concurrentRequestCounter = new AtomicInteger(0);

    private final Object singleRequestLock = new Object();

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("RemoteServiceCalls");

    private final ServiceCallSerializer serviceCallSerializer = new ServiceCallSerializer();

    private final Log log = LogFactory.getLog(getClass());

    public ReliableRPCStreamSender(ReliableRPCStreamHandle streamHandle, MessageRoutingService routingService) {
        this.streamHandle = streamHandle;
        this.streamId = streamHandle.getStreamId();
        this.routingService = routingService;
    }

    /**
     * Performs a RPC request that was initiated at the local node. See the main class JavaDoc for the general approach.
     * 
     * @param serviceCallRequest the request to perform on a remote node, retrying the transmission if necessary
     * @return the final result of the call; if it represents a failure, then it that error was considered unrecoverable (e.g. the remote
     *         node having restarted)
     * @throws SerializationException if serializing the request body failed
     */
    public ServiceCallResult performRequest(ServiceCallRequest serviceCallRequest) throws SerializationException {
        final int concurrencyLevel = concurrentRequestCounter.incrementAndGet();
        if (concurrencyLevel > 1) {
            log.debug((concurrencyLevel - 1) + " concurrent rRPC request(s) in stream " + streamId
                + " are blocked waiting for a previous rRPC to complete");
        }
        // TODO acquiring the lock is not "fair", which may violate in-order assumptions; not relevant for currently calling code, though
        try {
            // a simple shared-lock-per-stream approach, which may nonetheless be sufficient:
            // - the RPC call is blocking anyway, so using the calling thread for execution avoids extra thread overhead
            // - a shared, long-held lock may seem risky, but it is always acquired last, so it should not add new deadlock potential
            synchronized (singleRequestLock) {
                long sequenceNumber = assignNewSequenceNumber(serviceCallRequest);
                // serialization is done inside this method as it must include the sequence number, which is not known before
                byte[] serializedRequest = serviceCallSerializer.getSerializedForm(serviceCallRequest);
                // note: this is somewhat redundant with the verbose logging performed inside the serialization method
                if (verboseRequestLoggingEnabled) {
                    log.debug(StringUtils.format("Sending rRPC %d of stream %s for remote node %s, calling %s.%s(), "
                        + "serialized into a network payload of %d bytes", sequenceNumber, streamId, serviceCallRequest.getTargetNodeId(),
                        serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serializedRequest.length));
                }
                ServiceCallResult result = null;
                int retryCount = 0;
                while (true) {
                    if (verboseRequestLoggingEnabled && retryCount != 0) {
                        log.debug(
                            StringUtils.format("Starting retry attempt %d for rRPC %d of stream %s for remote node %s, calling %s.%s()",
                                retryCount, sequenceNumber, streamId, serviceCallRequest.getTargetNodeId(),
                                serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()));
                    }
                    NetworkResponse response = attemptRemoteCall(serviceCallRequest, serializedRequest);
                    // detect certain fatal errors; if one of these happen, return the failure result to the original caller
                    if (response.getResultCode() == ResultCode.TARGET_NODE_RESTARTED
                        || response.getResultCode() == ResultCode.INSTANCE_ID_COLLISION) {
                        log.debug(
                            StringUtils.format("Encountered a non-recoverable error during rRPC %d of stream %s "
                                + "for remote node %s (%s); returning error response to original caller", sequenceNumber,
                                streamId, serviceCallRequest.getTargetNodeId(), response.getResultCode()));
                        return ServiceCallResultFactory.representNetworkErrorAsRemoteOperationException(serviceCallRequest, response);
                    }

                    result = RemoteServiceCallSenderServiceImpl.deserializeSCRNetworkResponse(serviceCallRequest, response);
                    if (result.isSuccess()) { // TODO check: is this correct in case of method-level exceptions? -- misc_ro
                        break;
                    }

                    log.debug(
                        StringUtils.format(
                            "Received non-success response for rRPC %d of stream %s for remote node %s; retrying in %d seconds...",
                            sequenceNumber, streamId, serviceCallRequest.getTargetNodeId(), RETRY_WAIT_TIME_SECONDS));
                    retryCount++;
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(RETRY_WAIT_TIME_SECONDS));
                    } catch (InterruptedException e) {
                        return ServiceCallResultFactory.representInternalErrorAtSender(serviceCallRequest,
                            "Interrupted while waiting to retry rRPC " + sequenceNumber + " of stream " + streamId);
                    }
                }
                if (verboseRequestLoggingEnabled) {
                    log.debug(
                        StringUtils.format("Returning final response for rRPC %d of stream %s for remote node %s", sequenceNumber, streamId,
                            serviceCallRequest.getTargetNodeId()));
                }
                return result;
            }
        } finally {
            concurrentRequestCounter.decrementAndGet();
        }
    }

    private NetworkResponse attemptRemoteCall(ServiceCallRequest serviceCallRequest, byte[] serializedRequest)
        throws SerializationException {
        ServiceCallResult result;
        NetworkResponse response;
        response = routingService.performRoutedRequest(serializedRequest, ProtocolConstants.VALUE_MESSAGE_TYPE_RPC,
            serviceCallRequest.getTargetNodeId().convertToInstanceNodeSessionId());
        return response;
    }

    private long assignNewSequenceNumber(ServiceCallRequest serviceCallRequest) {
        long sequenceNumber = sequenceNumberGenerator.incrementAndGet();
        serviceCallRequest.setSequenceNumber(sequenceNumber);
        return sequenceNumber;
    }
}
