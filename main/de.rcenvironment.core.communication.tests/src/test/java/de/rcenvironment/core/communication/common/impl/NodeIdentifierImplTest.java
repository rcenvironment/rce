/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * @author Robert Mischke
 */
public class NodeIdentifierImplTest {

    /**
     * Verifies that {@link NodeIdentifierImpl} behaves properly on serialization.
     * 
     * @throws SerializationException on unexpected exceptions
     */
    @Test
    public void serializationRoundtrip() throws SerializationException {
        NodeIdentifierImpl original = new NodeIdentifierImpl("id");
        NodeIdentifierImpl deserialized =
            (NodeIdentifierImpl) MessageUtils.deserializeObject(MessageUtils.serializeObject(original),
                NodeIdentifierImpl.class);
        assertEquals(original.getIdString(), deserialized.getIdString());
        // check object identity of the assigned meta information holder
        assertTrue(original.getMetaInformationHolder() == deserialized.getMetaInformationHolder());
    }

}
