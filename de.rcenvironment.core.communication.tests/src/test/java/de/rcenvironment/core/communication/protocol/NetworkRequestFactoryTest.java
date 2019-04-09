/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Test case for {@link NetworkRequestFactory}.
 * 
 * @author Robert Mischke
 */
public class NetworkRequestFactoryTest {

    private InstanceNodeSessionId senderId = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("senderId");

    private InstanceNodeSessionId receiverId = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("receiverId");;

    /**
     * Common teardown.
     */
    @After
    public void teardown() {
        NodeIdentifierTestUtils.removeTestNodeIdentifierServiceFromCurrentThread();
    }

    /**
     * Verifies {@link NetworkRequestFactory#createNetworkRequest()}.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test
    public void createNetworkRequest() throws SerializationException {
        NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
        validateMessageTypeMetadata(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC);
        // TODO test other types, too
    }

    private void validateMessageTypeMetadata(String messageType) throws SerializationException {
        String testString = "test";
        byte[] contentBytes = MessageUtils.serializeSafeObject(testString);
        NetworkRequest request =
            NetworkRequestFactory.createNetworkRequest(contentBytes, messageType, senderId, receiverId);
        assertNotNull(request.getRequestId());
        assertEquals(NetworkRequest.REQUEST_ID_LENGTH, request.getRequestId().length());
        assertEquals(messageType, request.getMessageType());
        assertEquals(senderId, request.accessMetaData().getSender());
        assertEquals(receiverId, request.accessMetaData().getFinalRecipient());
        assertEquals(testString, MessageUtils.deserializeObject(request.getContentBytes()));
    }

}
