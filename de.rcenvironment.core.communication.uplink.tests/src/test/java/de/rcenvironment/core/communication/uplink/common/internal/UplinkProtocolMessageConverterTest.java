/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * {@link UplinkProtocolMessageConverter} tests.
 *
 * @author Robert Mischke
 */
public class UplinkProtocolMessageConverterTest {

    private final UplinkProtocolMessageConverter converter = new UplinkProtocolMessageConverter("unit test");

    /**
     * Encode/decode test.
     * 
     * @throws ProtocolException on failure
     */
    @Test
    public void emptyToolDescriptorListUpdate() throws ProtocolException {
        final ToolDescriptorListUpdate original = new ToolDescriptorListUpdate("dummySource", "dummyName", new ArrayList<>());
        final MessageBlock encoded = converter.encodeToolDescriptorListUpdate(original);
        final ToolDescriptorListUpdate restoredCopy = converter.decodeToolDescriptorListUpdate(encoded);
        assertEquals(original.getDestinationId(), restoredCopy.getDestinationId());
        // the restored list should not be the exact same object
        assertFalse(original.getToolDescriptors() == restoredCopy.getToolDescriptors());
        // compare list content (empty)
        assertArrayEquals(original.getToolDescriptors().toArray(), restoredCopy.getToolDescriptors().toArray());
    }

    /**
     * Encode/decode test.
     * 
     * @throws ProtocolException on failure
     */
    @Test
    public void filledToolDescriptorListUpdate() throws ProtocolException {
        final ArrayList<ToolDescriptor> originalToolDescriptors = new ArrayList<>();
        originalToolDescriptors.add(new ToolDescriptor("tId", "tVer", new HashSet<String>(Arrays.asList("groupId")), "hash",
            new JsonDataWithOptionalEncryption("serialized", new HashMap<String, String>())));
        final ToolDescriptorListUpdate original = new ToolDescriptorListUpdate("dummySource", "dummyName", originalToolDescriptors);
        final MessageBlock encoded = converter.encodeToolDescriptorListUpdate(original);
        final ToolDescriptorListUpdate restoredCopy = converter.decodeToolDescriptorListUpdate(encoded);
        assertEquals(original.getDestinationId(), restoredCopy.getDestinationId());
        // the restored list should not be the exact same object
        assertFalse(original.getToolDescriptors() == restoredCopy.getToolDescriptors());
        // compare list content
        assertArrayEquals(original.getToolDescriptors().toArray(), restoredCopy.getToolDescriptors().toArray());
    }

}
