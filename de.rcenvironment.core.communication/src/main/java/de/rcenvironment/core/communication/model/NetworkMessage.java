/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.Serializable;
import java.util.Map;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;

/**
 * Represents a single message sent over the communication layer. In the typical request-response
 * communication flow, both the request and the response are {@link NetworkMessage}s.
 * 
 * TODO review: add more convenience methods?
 * 
 * @author Robert Mischke
 */
public interface NetworkMessage {

    /**
     * Returns the top-level message type. See the VALUE_MESSAGE_TYPE_* constants in
     * {@link ProtocolConstants} for possible values.
     * 
     * @return the String id of the message type
     */
    String getMessageType();

    /**
     * Provides access to the raw payload bytes without triggering deserialization.
     * 
     * @return the raw payload byte array
     */
    byte[] getContentBytes();

    /**
     * Provides access to the deserialized payload; the result of the deserialization may be cached
     * internally.
     * 
     * @return the result of deserializing the payload byte array
     * @throws SerializationException on deserialization failure
     */
    Serializable getDeserializedContent() throws SerializationException;

    /**
     * Provides read-write access to this message's metadata.
     * 
     * @return the internal {@link MessageMetaData}
     */
    MessageMetaData accessMetaData();

    /**
     * Provides direct access to the internal metadata map. Changes to the returned map affect the
     * internal state of the {@link NetworkMessage}.
     * 
     * @return a mutable reference to the internal metadata map
     */
    Map<String, String> accessRawMetaData();
}
