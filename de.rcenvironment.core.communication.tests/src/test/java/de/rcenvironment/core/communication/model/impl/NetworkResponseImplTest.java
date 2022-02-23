/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Test case for {@link NetworkResponseImpl}.
 * 
 * @author Robert Mischke
 */
public class NetworkResponseImplTest {

    /**
     * Verifies the basic content serialization/deserialization round-trip.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test
    public void contentSerialization() throws SerializationException {
        String testString = "test";
        NetworkResponseImpl instance1 =
            new NetworkResponseImpl(MessageUtils.serializeObject(testString), MessageMetaData.create().getInnerMap());
        assertEquals(testString, instance1.getDeserializedContent());
        NetworkResponseImpl instance2 = new NetworkResponseImpl(instance1.getContentBytes(), instance1.accessRawMetaData());
        assertEquals(testString, instance2.getDeserializedContent());
    }

}
