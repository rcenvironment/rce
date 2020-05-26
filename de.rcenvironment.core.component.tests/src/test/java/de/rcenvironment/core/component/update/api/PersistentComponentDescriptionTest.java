/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Test cases for {@link PersistentComponentDescription}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class PersistentComponentDescriptionTest {

    /**
     * Test.
     * 
     * @throws IOException on error
     */
    @Test
    public void test() throws IOException {

        String persistentComponentDescription = "{"
            + "\"identifier\" : \"86881b19-105c-4e48-85a5-8ee0ee7197be\","
            + "\"name\" : \"Add\","
            + "\"location\" : \"335:129\","
            + "\"component\" : {"
            + "\"identifier\" : \"de.rcenvironment.rce.components.python.PythonComponent_Python\""
            + "}"
            + "}\"";

        PersistentComponentDescription description = new PersistentComponentDescription(persistentComponentDescription);

        assertEquals("de.rcenvironment.rce.components.python.PythonComponent_Python", description.getComponentIdentifier());
        assertEquals("", description.getComponentVersion());
        assertEquals(null, description.getComponentNodeIdentifier());
        assertEquals(persistentComponentDescription, description.getComponentDescriptionAsString());

        final InstanceNodeSessionId dummyInstanceId = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
        final LogicalNodeId dummyLogicalNodeId = dummyInstanceId.convertToDefaultLogicalNodeId();

        persistentComponentDescription = "{"
            + "\"identifier\" : \"86881b19-105c-4e48-85a5-8ee0ee7197be\","
            + "\"name\" : \"Add\","
            + "\"location\" : \"335:129\","
            + "\"component\" : {"
            + "\"identifier\" : \"de.rcenvironment.rce.components.python.PythonComponent_Python\","
            + "\"version\" : \"2.0\""
            + "},"
            + "\"platform\" : \"" + dummyInstanceId.getInstanceNodeSessionIdString() + "\""
            + "}\"";

        description = new PersistentComponentDescription(persistentComponentDescription);

        assertEquals("de.rcenvironment.rce.components.python.PythonComponent_Python", description.getComponentIdentifier());
        assertEquals("2.0", description.getComponentVersion());
        // useful additional aspect: the string form contains a (legacy) instance id, so this covers backwards compatibility
        assertEquals(dummyLogicalNodeId, description.getComponentNodeIdentifier());
        assertEquals(persistentComponentDescription, description.getComponentDescriptionAsString());
    }

    /**
     * Tests if an semantically invalid JSON representation is handled properly.
     * 
     * @throws IOException on unexpected error
     **/
    @Test
    public void testMissingAttributesHandledProperly() throws IOException {

        String persistentComponentDescription = "{\"component\" : {}}\"";

        try {
            new PersistentComponentDescription(persistentComponentDescription);
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("'identifier' missing"));
        }

        persistentComponentDescription = "{}";
        try {
            new PersistentComponentDescription(persistentComponentDescription);
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("'component' missing"));
        }

    }
}
