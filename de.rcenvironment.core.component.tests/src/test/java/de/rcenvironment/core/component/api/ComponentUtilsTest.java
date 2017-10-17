/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Test cases for {@link ComponentUtils}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class ComponentUtilsTest {

    private static final String COMPONENT_NAME = "Comp Name";

    private List<ComponentInstallation> installations;

    private ComponentInterfaceImpl compInterf1;

    private ComponentInterfaceImpl compInterf2;

    private ComponentInterfaceImpl compInterf11;

    private ComponentDescription cd1;

    private ComponentDescription cd3;

    private ComponentDescription cd4;

    private String version1 = "1.0";

    private String version2 = "2.0";

    private String version3 = "3.0";

    private final String compId1 = "cId1";

    private final String compId1WithVersion2 = compId1 + "/" + version2;

    private final String compId11 = "cId11";

    private final LogicalNodeId node1 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node.id-1")
        .convertToDefaultLogicalNodeId();

    private final LogicalNodeId node2 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node.id-2")
        .convertToDefaultLogicalNodeId();

    private final LogicalNodeId node3 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node.id-3")
        .convertToDefaultLogicalNodeId();

    private ComponentInstallationImpl compInst1;

    private ComponentInstallationImpl compInst2;

    private ComponentInstallationImpl compInst3;

    private ComponentInstallationImpl compInst4;

    /** Setup. */
    @SuppressWarnings("serial")
    @Before
    public void setUp() {

        compInterf1 = createComponentInterfaceImpl(compId1, version1);
        compInterf2 = createComponentInterfaceImpl(compId1, version2);
        compInterf11 = createComponentInterfaceImpl(compId11, version3);

        compInst1 = createComponentInstallationImpl(compInterf1, node1);
        cd1 = createComponentDescription(compInst1);

        compInst2 = createComponentInstallationImpl(compInterf1, node2);

        compInst3 = createComponentInstallationImpl(compInterf2, node3);
        cd3 = createComponentDescription(compInst3);

        compInst4 = createComponentInstallationImpl(compInterf11, node1);
        cd4 = createComponentDescription(compInst4);

        installations = new ArrayList<ComponentInstallation>() {

            {
                add(compInst1);
                add(compInst2);
                add(compInst3);
                add(compInst4);
            }
        };
    }

    private ComponentInterfaceImpl createComponentInterfaceImpl(String compId, String version) {
        ComponentInterfaceImpl compInterf = new ComponentInterfaceImpl();
        compInterf.setIdentifier(compId);
        compInterf.setVersion(version);
        return compInterf;
    }

    private ComponentInstallationImpl createComponentInstallationImpl(ComponentInterfaceImpl compInterf, LogicalNodeId node) {
        ComponentRevisionImpl compRev = new ComponentRevisionImpl();
        compRev.setComponentInterface(compInterf);
        ComponentInstallationImpl compInst = new ComponentInstallationImpl();
        compInst.setInstallationId(compInterf.getIdentifier());
        compInst.setComponentRevision(compRev);
        compInst.setNodeIdFromObject(node);
        return compInst;
    }

    private ComponentDescription createComponentDescription(ComponentInstallation compInst) {
        ComponentDescription cd = EasyMock.createNiceMock(ComponentDescription.class);
        EasyMock.expect(cd.getIdentifier()).andReturn(compInst.getComponentRevision().getComponentInterface().getIdentifier()).anyTimes();
        EasyMock.expect(cd.getVersion()).andReturn(compInst.getComponentRevision().getComponentInterface().getVersion()).anyTimes();
        EasyMock.expect(cd.getNode())
            .andReturn(NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(compInst.getNodeId())).anyTimes();
        EasyMock.expect(cd.getComponentInstallation()).andReturn(compInst).anyTimes();
        EasyMock.replay(cd);
        return cd;
    }

    /** Test. */
    @Test
    public void testGetNodesForComponent() {
        Map<LogicalNodeId, Integer> pis = ComponentUtils.getNodesForComponent(installations, cd1);
        assertEquals(3, pis.size());
        assertTrue(pis.containsKey(node1));
        assertTrue(pis.get(node1).equals(ComponentUtils.EQUAL_COMPONENT_VERSION));
        assertTrue(pis.containsKey(node2));
        assertTrue(pis.get(node2).equals(ComponentUtils.EQUAL_COMPONENT_VERSION));
        assertTrue(pis.containsKey(node3));
        assertTrue(pis.get(node3).equals(ComponentUtils.GREATER_COMPONENT_VERSION));

        pis = ComponentUtils.getNodesForComponent(installations, cd4);
        assertEquals(1, pis.size());
        assertTrue(pis.containsKey(node1));
        assertTrue(pis.get(node1).equals(ComponentUtils.EQUAL_COMPONENT_VERSION));
    }

    /** Test. */
    @Test
    public void testHasComponent() {
        assertTrue(ComponentUtils.hasComponent(installations, cd1.getIdentifier(), node1));
        assertFalse(ComponentUtils.hasComponent(installations, cd3.getIdentifier(), node1));
    }

    /** Test. */
    @Test
    public void testEliminateDuplicates() {
        List<ComponentInstallation> insts = ComponentUtils.eliminateComponentInterfaceDuplicates(installations, node2);
        assertEquals(2, insts.size());
        assertFalse(insts.contains(compInst1));
        assertTrue(insts.contains(compInst2));
        assertFalse(insts.contains(compInst3));
        assertTrue(insts.contains(compInst4));
    }

    /** Test. */
    @Test
    public void testGetComponentInstallation() {
        ComponentInstallation installation = ComponentUtils.getComponentInstallation(compId1WithVersion2, installations);
        assertEquals(compInst3, installation);

        installation = ComponentUtils.getComponentInstallation("unknown-id", installations);
        assertNull(installation);
    }

    /** Test. */
    @Test
    public void testGetPlaceholderComponentDescription() {
        ComponentInstallation cd = ComponentUtils.createPlaceholderComponentInstallation("de.rcenvironment.comp", "4.2", COMPONENT_NAME,
            NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true).convertToLogicalNodeId());
        assertEquals(COMPONENT_NAME, cd.getComponentRevision().getComponentInterface().getDisplayName());

        cd =
            ComponentUtils.createPlaceholderComponentInstallation("de.rcenvironment.comp", "1.0", null, NodeIdentifierTestUtils
                .createTestLogicalNodeSessionId(true).convertToLogicalNodeId());
        assertEquals("N/A", cd.getComponentRevision().getComponentInterface().getDisplayName());
    }

    /** Tests if a message is correctly created for difference kind of exception chains. */
    @Test
    public void testCreateErrorLogMessage() {
        String unexpectedMessage = "Unexpected error: ";
        String causeMessage = "; cause: ";

        // exception without message and without cause
        assertEquals(unexpectedMessage + RuntimeException.class.getSimpleName(),
            ComponentUtils.createErrorLogMessage(new RuntimeException()));
        // exception without message, but with cause that has no message as well
        assertEquals(unexpectedMessage + RuntimeException.class.getSimpleName()
            + causeMessage + unexpectedMessage + RuntimeException.class.getSimpleName(),
            ComponentUtils.createErrorLogMessage(new RuntimeException(new RuntimeException())));
        // exception with message, but without cause
        assertEquals("error message 1",
            ComponentUtils.createErrorLogMessage(new RuntimeException("error message 1")));
        // exception without message, but with cause that has message
        assertEquals(unexpectedMessage + RuntimeException.class.getSimpleName()
            + causeMessage + "error message 2",
            ComponentUtils.createErrorLogMessage(new RuntimeException(new RuntimeException("error message 2"))));
        // exception without message, but with cause that has no message
        assertEquals("error message 3" + causeMessage + unexpectedMessage + RuntimeException.class.getSimpleName(),
            ComponentUtils.createErrorLogMessage(new RuntimeException("error message 3", new RuntimeException())));
        // exception with message and with cause that has message
        assertEquals("error message 4" + causeMessage + "error message 5",
            ComponentUtils.createErrorLogMessage(new RuntimeException("error message 4", new RuntimeException("error message 5"))));
        // exception with message and with cause that has message and cause
        assertEquals("error message 6" + causeMessage + "error message 7" + causeMessage
            + unexpectedMessage + RuntimeException.class.getSimpleName(),
            ComponentUtils.createErrorLogMessage(new RuntimeException("error message 6",
                new RuntimeException("error message 7", new RuntimeException()))));
        // exception with message and with cause that has message and cause that has message
        assertEquals("error message 8" + causeMessage + "error message 9" + causeMessage + "error message 10",
            ComponentUtils.createErrorLogMessage(new RuntimeException("error message 8",
                new RuntimeException("error message 9", new RuntimeException("error message 10")))));
    }

}
