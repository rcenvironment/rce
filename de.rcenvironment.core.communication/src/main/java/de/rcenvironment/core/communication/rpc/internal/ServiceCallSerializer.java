/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * A simple specialized wrapper around {@link MessageUtils} with added request logging.
 * 
 * TODO also move deserialization here
 *
 * @author Robert Mischke
 */
public final class ServiceCallSerializer {

    // A soft size limit for individual network payloads; all messages exceeding this should be logged as warnings.
    // This is currently (arbitrarily) set to 512 kB, which is twice the current default chunked upload size. -- misc_ro
    private static final int OUTGOING_NETWORK_PAYLOAD_SIZE_WARNING_THRESHOLD = 1024 * 1024;

    private final boolean verboseRequestLoggingEnabled = DebugSettings.getVerboseLoggingEnabled("RemoteServiceCalls");

    private final Log log = LogFactory.getLog(getClass());

    /**
     * A simple wrapper around {@link MessageUtils#serializeObject()} with added request logging.
     * 
     * @param serviceCallRequest the request to serialize
     * @return the serialized form
     * @throws SerializationException on error
     */
    public byte[] getSerializedForm(ServiceCallRequest serviceCallRequest) throws SerializationException {
        final byte[] serializedRequest = MessageUtils.serializeObject(serviceCallRequest);
        if (verboseRequestLoggingEnabled) {
            log.debug(StringUtils.format("Converted RPC for %s.%s() on %s into a network payload of %d bytes; rRPC stream: %s",
                serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId(),
                serializedRequest.length, serviceCallRequest.getReliableRPCStreamId()));
        }
        if (serializedRequest.length >= OUTGOING_NETWORK_PAYLOAD_SIZE_WARNING_THRESHOLD) {
            log.debug(
                StringUtils.format("Generated a large network message for an RPC to %s.%s() on %s (payload size: %d bytes)",
                    serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), serviceCallRequest.getTargetNodeId(),
                    serializedRequest.length));
        }
        return serializedRequest;
    }
}
