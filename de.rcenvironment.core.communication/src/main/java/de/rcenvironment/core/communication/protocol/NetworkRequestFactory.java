/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.impl.NetworkRequestImpl;
import de.rcenvironment.core.communication.model.internal.AbstractNetworkMessage;

/**
 * Central factory for {@link NetworkRequest} instances. Encapsulates metadata generation and reconstruction of {@link NetworkRequest}s from
 * received metadata.
 * 
 * @author Robert Mischke
 */
public final class NetworkRequestFactory {

    // TODO add consistent requestId handling

    private NetworkRequestFactory() {}

    /**
     * Generates a new {@link NetworkRequest} for sending.
     * 
     * @param contentBytes the serialized message body
     * @param messageType the message type; see {@link ProtocolConstants}
     * @param sender the sender
     * @param finalRecipient the final receiver/destination of the request; can be null if no forwarding is required
     * @return the generated {@link NetworkRequest}
     */
    public static NetworkRequest createNetworkRequest(byte[] contentBytes, String messageType, InstanceNodeSessionId sender,
        InstanceNodeSessionId finalRecipient) {
        MessageMetaData metaData = MessageMetaData.create();
        metaData.setMessageType(messageType);
        metaData.setSender(sender);
        if (finalRecipient != null) {
            metaData.setFinalRecipient(finalRecipient);
        }
        metaData.addTraceStep(sender.getInstanceNodeSessionIdString());
        // TODO refactor: remove unwrapping/wrapping
        return new NetworkRequestImpl(contentBytes, metaData.getInnerMap());
    }

    /**
     * Generates a new {@link NetworkRequest} to forward when a {@link NetworkRequest} with a different destination than the local node was
     * received.
     * 
     * This method takes care of hop count incrementing, route tracing, and sanity checking that the message is actually valid for
     * forwarding.
     * 
     * @param receivedRequest the {@link NetworkRequest} that arrived at the local node
     * @param localNodeId the {@link InstanceNodeSessionId} of the local node (NOT the recipient!)
     * @return the generated {@link NetworkRequest} for forwarding
     */
    public static NetworkRequest createNetworkRequestForForwarding(NetworkRequest receivedRequest, InstanceNodeSessionId localNodeId) {
        MessageMetaData newMetadata = MessageMetaData.cloneAndWrap(receivedRequest.accessRawMetaData());
        // TODO check TTL?
        // TODO check for valid message type?
        newMetadata.incHopCount();
        newMetadata.addTraceStep(localNodeId.getInstanceNodeSessionIdString());
        // the 3.0.0 approach is to maintain the original request id; change if necessary - misc_ro
        String requestIdToMaintain = receivedRequest.getRequestId();
        return new NetworkRequestImpl(receivedRequest.getContentBytes(), newMetadata.getInnerMap(), requestIdToMaintain);
    }

    /**
     * Reconstructs a {@link NetworkRequest} from the serialized body and metadata received by the transport implementation.
     * 
     * @param contentBytes the serialized message body
     * @param metaData the message metadata as passed by the transport implementation
     * @return the reconstructed {@link NetworkRequest}
     */
    public static NetworkRequest reconstructNetworkRequest(byte[] contentBytes, Map<String, String> metaData) {
        return new NetworkRequestImpl(contentBytes, metaData, metaData.get(AbstractNetworkMessage.METADATA_KEY_REQUEST_ID));
    }

    /**
     * Creates a new {@link NetworkRequest} that is independent of the original, i.e. changes to one instance will not affect the other.
     * Useful to avoid side effects during in-JVM testing.
     * 
     * @param request the original request
     * @return the independent clone
     */
    public static NetworkRequestImpl createDetachedClone(NetworkRequest request) {
        // clone the content array
        byte[] originalContent = request.getContentBytes();
        final byte[] detachedContentBytes = Arrays.copyOf(originalContent, originalContent.length);

        // clone the metadata; a shallow copy is sufficient as it is a String/String map
        final Map<String, String> clonedRequestMetaData = new HashMap<String, String>(request.accessRawMetaData());

        NetworkRequestImpl clonedRequest = new NetworkRequestImpl(detachedContentBytes, clonedRequestMetaData, request.getRequestId());
        return clonedRequest;
    }

    /**
     * Creates a clone of the given {@link NetworkRequest}, but with a new, unique request id. Intended for cases where a sender is
     * broadcasting the same request to multiple recipients.
     * 
     * Note that the payload bytes are not detached from the original request; modifications to one would affect the other. This is
     * irrelevant, though, as modifying the payload array after creation is neither intended nor supported. The metadata map, however, is
     * detached.
     * 
     * @param request the original request
     * @return a clone with shared payload data, detached metadat properties, and a new request id within these properties
     */
    public static NetworkRequest cloneWithNewRequestId(NetworkRequest request) {
        Map<String, String> newMetaData = new HashMap<String, String>(request.accessRawMetaData());
        // note: the new request id is automatically assigned by the constructor
        return new NetworkRequestImpl(request.getContentBytes(), newMetaData);
    }
}
