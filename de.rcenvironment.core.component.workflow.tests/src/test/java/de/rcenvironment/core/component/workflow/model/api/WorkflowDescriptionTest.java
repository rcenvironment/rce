/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Random;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Test cases for {@link WorkflowDescription}.
 * 
 * @author Heinrich Wendel
 * @author Robert Mischke (id adaptations and some cleanup)
 */
public class WorkflowDescriptionTest {

    private static final String DEFAULT_TEST_WORKFLOW_ID = "testWfId";

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private ComponentDescription cd = EasyMock.createNiceMock(ComponentDescription.class);

    /** Test. */
    @Test
    public void testWorkflowDescription() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        assertEquals(DEFAULT_TEST_WORKFLOW_ID, desc.getIdentifier());
    }

    /** Test. */
    @Test
    public void testGetName() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        desc.setName("test2");
        assertEquals("test2", desc.getName());
    }

    /** Test. */
    @Test
    public void testGetWorkflowVersion() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        final int version = 7;
        desc.setWorkflowVersion(version);
        assertEquals(version, desc.getWorkflowVersion());
    }

    /** Test. */
    @Test
    public void testGetAdditionalInformation() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        String value = "test ... " + RANDOM.nextLong();
        String expectedValue = new String(value);
        desc.setAdditionalInformation(value);
        assertEquals(expectedValue, desc.getAdditionalInformation());
    }

    /** Test. */
    @Test
    public void testGetControllerNode() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        InstanceNodeSessionId tp = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName(DEFAULT_TEST_WORKFLOW_ID);
        desc.setControllerNode(tp.convertToDefaultLogicalNodeId());
        assertEquals(tp.convertToDefaultLogicalNodeId(), desc.getControllerNode());
    }

    /** Test. */
    @Test
    @Ignore
    public void testGetWorkflowNodes() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        WorkflowNodeChangeListener l1 = new WorkflowNodeChangeListener(WorkflowDescription.PROPERTY_NODES);
        desc.addPropertyChangeListener(l1);

        assertEquals(0, desc.getWorkflowNodes().size());
        WorkflowNode node = new WorkflowNode(cd);
        desc.addWorkflowNode(node);
        assertTrue(l1.getFired());
        assertEquals(1, desc.getWorkflowNodes().size());
        assertTrue(desc.getWorkflowNodes().contains(node));

        desc.removePropertyChangeListener(l1);
        WorkflowNodeChangeListener l2 = new WorkflowNodeChangeListener(WorkflowDescription.PROPERTY_NODES);
        desc.addPropertyChangeListener(l2);

        desc.removeWorkflowNode(node);
        assertTrue(l2.getFired());
        assertEquals(0, desc.getWorkflowNodes().size());
    }

    /** Test. */
    @Test
    @Ignore
    public void testGetWorkflowNode() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        WorkflowNode node = new WorkflowNode(cd);
        try {
            desc.getWorkflowNode(node.getIdentifierAsObject());
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        desc.addWorkflowNode(node);
        assertEquals(node, desc.getWorkflowNode(node.getIdentifierAsObject()));
    }

    /** Test. */
    @Test
    @Ignore
    public void testGetConnections() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);

        WorkflowNodeChangeListener l1 = new WorkflowNodeChangeListener(WorkflowDescription.PROPERTY_CONNECTIONS);

        WorkflowNode node1 = new WorkflowNode(cd);
        WorkflowNode node2 = new WorkflowNode(cd);
        EndpointDescription ep1 = new EndpointDescription(null, EndpointType.INPUT);
        EndpointDescription ep2 = new EndpointDescription(null, EndpointType.OUTPUT);
        Connection connection = new Connection(node1, ep2, node2, ep1);
        desc.addWorkflowNode(node1);
        desc.addWorkflowNode(node2);

        desc.addPropertyChangeListener(l1);
        assertEquals(0, desc.getConnections().size());
        desc.addConnection(connection);
        assertEquals(1, desc.getConnections().size());
        assertTrue(desc.getConnections().contains(connection));
        assertTrue(l1.getFired());

        desc.removePropertyChangeListener(l1);

        WorkflowNodeChangeListener l2 = new WorkflowNodeChangeListener(WorkflowDescription.PROPERTY_CONNECTIONS);
        desc.addPropertyChangeListener(l2);
        desc.removeConnection(connection);
        assertEquals(0, desc.getConnections().size());
        assertTrue(l2.getFired());
    }

    /** Test. */
    @Test
    public void testClone() {
        WorkflowDescription desc = new WorkflowDescription(DEFAULT_TEST_WORKFLOW_ID);
        desc.clone();
    }

    /**
     * Dummy implementation of {@link PropertyChangeListener}.
     * 
     * @author Heinrich Wendel
     */
    class WorkflowNodeChangeListener implements PropertyChangeListener {

        /** Fired or not? */
        private boolean fired = false;

        /** Event to check. */
        private String event;

        /**
         * Constructor.
         * 
         * @param event Name of the property to listen for.
         */
        WorkflowNodeChangeListener(String event) {
            this.event = event;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            assertEquals(event, evt.getPropertyName());
            fired = true;
        }

        public boolean getFired() {
            return fired;
        }
    };
}
