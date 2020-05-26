/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utilities for handling {@link NetworkRequest}s.
 * 
 * @author Robert Mischke
 */
public final class NetworkRequestUtils {

    private NetworkRequestUtils() {}

    /**
     * Helper method that wraps {@link NetworkRequest#getDeserializedContent()} and converts occurring {@link SerializationException}s into
     * {@link InternalMessagingException}s.
     * 
     * @param request the request
     * @return the deserialized content on success
     * @throws InternalMessagingException on {@link SerializationException}s
     */
    public static Serializable deserializeWithExceptionHandling(NetworkRequest request) throws InternalMessagingException {
        try {
            return request.getDeserializedContent();
        } catch (SerializationException e) {
            throw new InternalMessagingException(StringUtils.format("Failed to deserialize a NetworkRequest's content: type=%s, id=%s",
                request.getMessageType(), request.getRequestId()), e);
        }
    }

}
