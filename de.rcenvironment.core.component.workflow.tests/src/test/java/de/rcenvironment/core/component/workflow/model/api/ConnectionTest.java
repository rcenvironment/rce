/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Test cases for {@link Connection}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class ConnectionTest {

    private ComponentDescription cd = EasyMock.createNiceMock(ComponentDescription.class);

    private WorkflowNode node1 = new WorkflowNode(cd);

    private WorkflowNode node2 = new WorkflowNode(cd);

    private WorkflowNode node3 = new WorkflowNode(cd);

    private EndpointDescription ep1 = new EndpointDescription(null, EndpointType.INPUT);

    private EndpointDescription ep2 = new EndpointDescription(null, EndpointType.OUTPUT);

    private Connection connection;

    private Connection anotherConnection;

    /** Test. */
    @Before
    public void setUp() {
        connection = new Connection(node1, ep2, node2, ep1);
        anotherConnection = new Connection(node1, ep2, node3, ep1);
    }

    /** Test. */
    @Test
    public void testConnection() {
        assertSame(connection.getSourceNode(), node1);
        assertSame(connection.getTargetNode(), node2);
        assertSame(connection.getOutput(), ep2);
        assertSame(connection.getInput(), ep1);
    }

    /** Test. */
    @Test
    public void testEquals() {

        assertTrue(connection.equals(connection));
        assertFalse(connection.equals(anotherConnection));
        assertFalse(anotherConnection.equals(connection));
        assertTrue(anotherConnection.equals(anotherConnection));
    }

    /** Test. */
    @Test
    public void testHashCode() {
        assertEquals(connection.hashCode(), connection.hashCode());
        assertFalse(connection.hashCode() == anotherConnection.hashCode());
    }

}
