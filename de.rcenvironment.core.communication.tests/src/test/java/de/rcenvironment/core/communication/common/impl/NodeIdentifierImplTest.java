/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * @author Robert Mischke
 */
public class NodeIdentifierImplTest {

    /**
     * Common setup.
     */
    @Before
    public void setup() {
        NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
    }

    /**
     * Common teardown.
     */
    @After
    public void teardown() {
        NodeIdentifierTestUtils.removeTestNodeIdentifierServiceFromCurrentThread();
    }

    /**
     * Verifies that {@link InstanceNodeId} behaves properly on serialization.
     * 
     * @throws SerializationException on unexpected exceptions
     */
    @Test
    public void instanceNodeIdSerializationRoundtrip() throws SerializationException {
        NodeIdentifierImpl original = (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeId();
        NodeIdentifierImpl reconstructed =
            (NodeIdentifierImpl) MessageUtils.deserializeObject(MessageUtils.serializeObject(original),
                NodeIdentifierImpl.class);
        checkForCompleteEquality(original, reconstructed);
    }

    /**
     * Verifies that {@link InstanceNodeSessionId} behaves properly on serialization.
     * 
     * @throws SerializationException on unexpected exceptions
     */
    @Test
    public void instanceNodeSessionIdSerializationRoundtrip() throws SerializationException {
        NodeIdentifierImpl original = (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("test1b");
        NodeIdentifierImpl reconstructed =
            (NodeIdentifierImpl) MessageUtils.deserializeObject(MessageUtils.serializeObject(original),
                NodeIdentifierImpl.class);
        checkForCompleteEquality(original, reconstructed);
    }

    /**
     * Verifies that {@link NodeIdentifierTestUtils#createTestInstanceNodeId()} returns an instance with the expected properties.
     */
    @Test
    public void validateTestInstanceIdProperties() {
        NodeIdentifierImpl original = (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeId();
        assertTrue(original instanceof InstanceNodeId);
        assertEquals(CommonIdBase.INSTANCE_ID_STRING_LENGTH, original.getInstanceNodeIdString().length());
        assertEquals(CommonIdBase.INSTANCE_ID_STRING_LENGTH, original.getFullIdString().length());
        assertNull(original.getSessionIdPart());
    }

    /**
     * Verifies that {@link NodeIdentifierTestUtils#createTestInstanceNodeSessionIdWithDisplayName(String)} returns an instance with the
     * expected properties.
     */
    @Test
    public void validateTestInstanceNodeSessionIdProperties() {
        NodeIdentifierImpl original =
            (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("proptest2");
        assertTrue(original instanceof InstanceNodeSessionId);
        assertEquals(CommonIdBase.INSTANCE_ID_STRING_LENGTH, original.getInstanceNodeIdString().length());
        assertEquals(CommonIdBase.INSTANCE_SESSION_ID_STRING_LENGTH, original.getFullIdString().length());
        assertEquals(CommonIdBase.SESSION_PART_LENGTH, original.getSessionIdPart().length());
        assertEquals("proptest2", original.getAssociatedDisplayName());
    }

    /**
     * Validates {@link NodeIdentifierImpl#isSameInstanceAs()} and {@link NodeIdentifierImpl#isSameInstanceSessionAs()}.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void comparisonMethods() throws IdentifierException {
        InstanceNodeSessionId original = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
        InstanceNodeSessionId unrelated = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
        InstanceNodeSessionId clone = NodeIdentifierUtils.parseInstanceNodeSessionIdString(original.getInstanceNodeSessionIdString());
        InstanceNodeSessionId sameInstanceIdButNewSession =
            NodeIdentifierTestUtils.createTestInstanceNodeSessionId(original.getInstanceNodeIdString());

        // internal sanity checks
        assertTrue(original.getInstanceNodeIdString().equals(sameInstanceIdButNewSession.getInstanceNodeIdString()));
        assertFalse(original.getSessionIdPart().equals(sameInstanceIdButNewSession.getSessionIdPart()));

        // actual tests
        assertTrue(original.isSameInstanceNodeAs(original));
        assertTrue(original.isSameInstanceNodeSessionAs(original));

        assertTrue(original.isSameInstanceNodeAs(clone));
        assertTrue(original.isSameInstanceNodeSessionAs(clone));

        assertFalse(original.isSameInstanceNodeAs(unrelated));
        assertFalse(original.isSameInstanceNodeSessionAs(unrelated));

        assertTrue(original.isSameInstanceNodeAs(sameInstanceIdButNewSession));
        assertFalse(original.isSameInstanceNodeSessionAs(sameInstanceIdButNewSession));
    }

    /**
     * Verifies that certain given string representations are parsed as expected.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void parsingOfStandardStringForms() throws IdentifierException {
        final String instanceIdString = "0123456789abcdef0123456789abcdef";
        final String sessionPartString = "0123456789";
        final String instanceSessionIdString = "0123456789abcdef0123456789abcdef::0123456789";
        // min length logical node parts
        final String shortLogicalNodePart = "1";
        final String customLogicalNodeIdString1 = "0123456789abcdef0123456789abcdef:1";
        final String customLogicalNodeSessionIdString1 = "0123456789abcdef0123456789abcdef:1:0123456789";
        // max length logical node parts
        final String longLogicalNodePart = "000000000000000000000000ffffffff";
        final String customLogicalNodeIdString2 = "0123456789abcdef0123456789abcdef:000000000000000000000000ffffffff";
        final String customLogicalNodeSessionIdString2 = "0123456789abcdef0123456789abcdef:000000000000000000000000ffffffff:0123456789";

        // internal test consistency / sanity check
        assertEquals(CommonIdBase.INSTANCE_ID_STRING_LENGTH, instanceIdString.length());
        assertEquals(CommonIdBase.INSTANCE_SESSION_ID_STRING_LENGTH, instanceSessionIdString.length());

        final InstanceNodeId instanceId = NodeIdentifierUtils.parseInstanceNodeIdString(instanceIdString);
        assertEquals(instanceIdString, ((NodeIdentifierImpl) instanceId).getFullIdString()); // internal
        assertEquals(instanceIdString, instanceId.getInstanceNodeIdString()); // public

        final InstanceNodeSessionId instanceSessionId = NodeIdentifierUtils.parseInstanceNodeSessionIdString(instanceSessionIdString);
        assertEquals(instanceSessionIdString, ((NodeIdentifierImpl) instanceSessionId).getFullIdString()); // internal
        assertEquals(instanceSessionIdString, instanceSessionId.getInstanceNodeSessionIdString()); // public
        assertEquals(instanceIdString, instanceSessionId.getInstanceNodeIdString());
        assertEquals(sessionPartString, ((LogicalNodeId) instanceSessionId).getSessionIdPart());

        final LogicalNodeId logicalNodeId1 = NodeIdentifierUtils.parseLogicalNodeIdString(customLogicalNodeIdString1);
        assertEquals(customLogicalNodeIdString1, ((NodeIdentifierImpl) logicalNodeId1).getFullIdString()); // internal
        assertEquals(customLogicalNodeIdString1, logicalNodeId1.getLogicalNodeIdString()); // public
        assertEquals(instanceIdString, logicalNodeId1.getInstanceNodeIdString());
        assertNull(logicalNodeId1.getSessionIdPart());
        assertEquals(shortLogicalNodePart, logicalNodeId1.getLogicalNodePart());

        final LogicalNodeId logicalNodeId2 = NodeIdentifierUtils.parseLogicalNodeIdString(customLogicalNodeIdString2);
        assertEquals(customLogicalNodeIdString2, ((NodeIdentifierImpl) logicalNodeId2).getFullIdString()); // internal
        assertEquals(customLogicalNodeIdString2, logicalNodeId2.getLogicalNodeIdString()); // public
        assertEquals(instanceIdString, logicalNodeId2.getInstanceNodeIdString());
        assertNull(logicalNodeId2.getSessionIdPart());
        assertEquals(longLogicalNodePart, logicalNodeId2.getLogicalNodePart());

        final LogicalNodeSessionId logicalNodeSessionId1 =
            NodeIdentifierUtils.parseLogicalNodeSessionIdString(customLogicalNodeSessionIdString1);
        assertEquals(customLogicalNodeSessionIdString1, ((NodeIdentifierImpl) logicalNodeSessionId1).getFullIdString()); // internal
        assertEquals(customLogicalNodeSessionIdString1, logicalNodeSessionId1.getLogicalNodeSessionIdString()); // public
        assertEquals(instanceIdString, logicalNodeSessionId1.getInstanceNodeIdString());
        assertEquals(sessionPartString, logicalNodeSessionId1.getSessionIdPart());
        assertEquals(shortLogicalNodePart, logicalNodeSessionId1.getLogicalNodePart());

        final LogicalNodeSessionId logicalNodeSessionId2 =
            NodeIdentifierUtils.parseLogicalNodeSessionIdString(customLogicalNodeSessionIdString2);
        assertEquals(customLogicalNodeSessionIdString2, ((NodeIdentifierImpl) logicalNodeSessionId2).getFullIdString()); // internal
        assertEquals(customLogicalNodeSessionIdString2, logicalNodeSessionId2.getLogicalNodeSessionIdString()); // public
        assertEquals(instanceIdString, logicalNodeSessionId2.getInstanceNodeIdString());
        assertEquals(sessionPartString, logicalNodeSessionId2.getSessionIdPart());
        assertEquals(longLogicalNodePart, logicalNodeSessionId2.getLogicalNodePart());
    }

    /**
     * Triggers all available conversions to make sure none of them fail internal consistency checks.
     */
    @Test
    public void triggerAvailableConversions() {
        NodeIdentifierTestUtils.createTestInstanceNodeId().convertToDefaultLogicalNodeId();
        NodeIdentifierTestUtils.createTestInstanceNodeId().convertToDefaultLogicalNodeId();
        NodeIdentifierTestUtils.createTestInstanceNodeSessionId().convertToInstanceNodeId();
        NodeIdentifierTestUtils.createTestInstanceNodeSessionId().convertToDefaultLogicalNodeId();
        NodeIdentifierTestUtils.createTestInstanceNodeSessionId().convertToDefaultLogicalNodeSessionId();
        NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true).convertToInstanceNodeSessionId();
        NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true).convertToLogicalNodeId();
    }

    /**
     * Tests the round-trip of serializing an <b>instance id</b> to a string form with {@link InstanceNodeSessionId#getFullIdString()} and
     * reconstructing with {@link NodeIdentifierUtils#parseInstanceNodeIdString(String)}.
     * 
     * @throws IdentifierException not expected
     */
    @Test
    public void reconstructionOfInstanceIdFromFullId() throws IdentifierException {
        NodeIdentifierImpl original = (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeId();
        final String stringForm = original.getFullIdString();
        NodeIdentifierImpl reconstructed =
            (NodeIdentifierImpl) NodeIdentifierUtils.parseInstanceNodeIdString(stringForm);
        checkForCompleteEquality(original, reconstructed);
        assertEquals(CommonIdBase.INSTANCE_ID_STRING_LENGTH, stringForm.length());
    }

    /**
     * Tests the round-trip of serializing an <b>instance session id</b> to a string form with
     * {@link InstanceNodeSessionId#getFullIdString()} and reconstructing with {@link NodeIdentifierUtils#parseInstanceNodeIdString(String)}
     * .
     * 
     * @throws SerializationException not expected
     * @throws IdentifierException not expected
     */
    @Test
    public void reconstructionOfInstanceNodeSessionIdFromFullId() throws SerializationException, IdentifierException {
        NodeIdentifierImpl original = (NodeIdentifierImpl) NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("test3");
        final String stringForm = original.getFullIdString();
        NodeIdentifierImpl reconstructed =
            (NodeIdentifierImpl) NodeIdentifierUtils.parseInstanceNodeSessionIdString(stringForm);
        checkForCompleteEquality(original, reconstructed);
        assertEquals(CommonIdBase.INSTANCE_SESSION_ID_STRING_LENGTH, stringForm.length());
    }

    private void checkForCompleteEquality(NodeIdentifierImpl reference, NodeIdentifierImpl toTest) {
        assertTrue(reference != toTest);
        assertEquals(reference.getFullIdString(), toTest.getFullIdString());
        assertEquals(reference.getInstanceNodeIdString(), toTest.getInstanceNodeIdString());
        assertEquals(reference.getSessionIdPart(), toTest.getSessionIdPart());
        assertEquals(reference.getLogicalNodePart(), toTest.getLogicalNodePart());
        assertEquals(reference.getAssociatedDisplayName(), toTest.getAssociatedDisplayName());
        // test comparison methods
        assertTrue(reference.isSameInstanceNodeAs(toTest));
        assertTrue(toTest.isSameInstanceNodeAs(reference));
    }

}
