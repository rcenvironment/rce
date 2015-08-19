/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Test case for {@link NetworkRequestFactory}.
 * 
 * @author Robert Mischke
 */
public class NetworkRequestFactoryTest {

    private static final int UUID_LENGTH = 36;

    private NodeIdentifier senderId = NodeIdentifierFactory.fromNodeId("senderId");

    private NodeIdentifier receiverId = NodeIdentifierFactory.fromNodeId("receiverId");;

    /**
     * Verifies {@link NetworkRequestFactory#createNetworkRequest()}.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test
    public void createNetworkRequest() throws SerializationException {
        validateMessageTypeMetadata(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC);
        // TODO test other types, too
    }

    private void validateMessageTypeMetadata(String messageType) throws SerializationException {
        String testString = "test";
        byte[] contentBytes = MessageUtils.serializeSafeObject(testString);
        NetworkRequest request =
            NetworkRequestFactory.createNetworkRequest(contentBytes, messageType, senderId, receiverId);
        assertNotNull(request.getRequestId());
        assertEquals(UUID_LENGTH, request.getRequestId().length());
        assertEquals(messageType, request.getMessageType());
        assertEquals(senderId, request.accessMetaData().getSender());
        assertEquals(receiverId, request.accessMetaData().getFinalRecipient());
        assertEquals(testString, MessageUtils.deserializeObject(request.getContentBytes()));
    }

}
