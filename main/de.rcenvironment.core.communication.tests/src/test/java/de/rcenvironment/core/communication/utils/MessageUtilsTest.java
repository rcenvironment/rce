/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.impl.InitialNodeInformationImpl;

/**
 * {@link MessageUtils} test case.
 * 
 * @author Robert Mischke
 */
public class MessageUtilsTest {

    /**
     * Tests basic serialization/deserialization.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test
    public void basicRoundTrip() throws SerializationException {
        // create arbitrary serializable object
        InitialNodeInformationImpl testObject = new InitialNodeInformationImpl();
        String testValue = "test value";
        testObject.setDisplayName(testValue);
        // perform round-trip
        byte[] serialized = MessageUtils.serializeObject(testObject);
        Serializable restored1 = MessageUtils.deserializeObject(serialized);
        InitialNodeInformationImpl restored2 = MessageUtils.deserializeObject(serialized, InitialNodeInformationImpl.class);
        // verify
        assertTrue(restored1 instanceof InitialNodeInformationImpl);
        assertEquals(testValue, ((InitialNodeInformation) restored1).getDisplayName());
        assertEquals(testValue, restored2.getDisplayName());
    }

    /**
     * Tests proper serialization/deserialization of 'null'.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test
    public void nullRoundTrip() throws SerializationException {
        byte[] serialized = MessageUtils.serializeObject(null);
        Serializable restored1 = MessageUtils.deserializeObject(serialized);
        InitialNodeInformationImpl restored2 = MessageUtils.deserializeObject(serialized, InitialNodeInformationImpl.class);
        // verify
        assertNotNull(serialized);
        assertNull(restored1);
        assertNull(restored2);
    }

    /**
     * Verifies that null is not accepted as a serialized form. but causes an exception on
     * deserialization instead.
     * 
     * @throws SerializationException on unexpected errors
     */
    @Test(expected = SerializationException.class)
    public void exceptionOnNullDeserialization() throws SerializationException {
        MessageUtils.deserializeObject(null);
    }

}
