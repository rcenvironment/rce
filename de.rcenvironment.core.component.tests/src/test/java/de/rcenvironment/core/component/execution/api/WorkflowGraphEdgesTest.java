/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;

/**
 * Unit tests for {@link WorkflowGraphEdges}.
 * 
 * @author Alexander Weinert
 */
public class WorkflowGraphEdgesTest {
    
    /**
     * Tests that an empty {@link WorkflowGraphEdges} object can be successfully serialized and deserialized.
     * 
     * @throws IOException Thrown if serialization of the test object fails. Not expected.
     * @throws ClassNotFoundException Thrown if deserialization of the test object fails. Not expected.
     */
    @Test
    public void testEmptySerialization() throws IOException, ClassNotFoundException {
        final WorkflowGraphEdges edges = new WorkflowGraphEdges();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(edges);
        
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        
        final WorkflowGraphEdges deserializedEdges = (WorkflowGraphEdges) ois.readObject();
        
        Assert.assertEquals(edges, deserializedEdges);
    }

    
    /**
     * Tests that an {@link WorkflowGraphEdges} object containing a single edge can be successfully serialized and deserialized.
     * 
     * @throws IOException Thrown if serialization of the test object fails. Not expected.
     * @throws ClassNotFoundException Thrown if deserialization of the test object fails. Not expected.
     */
    @Test
    public void testSingleElementSerialization() throws IOException, ClassNotFoundException {
        final WorkflowGraphEdges edges = new WorkflowGraphEdges();
        final WorkflowGraphNode node1 = WorkflowGraphTestUtils.createNewNode(2, 3, false);
        final WorkflowGraphNode node2 = WorkflowGraphTestUtils.createNewNode(1, 1, false);
        final WorkflowGraphEdge edge =
            WorkflowGraphTestUtils.createEdge(node1, 1, EndpointCharacter.SAME_LOOP, node2, 0, EndpointCharacter.SAME_LOOP);
        edges.addEdge(edge);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(edges);
        
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        
        final WorkflowGraphEdges deserializedEdges = (WorkflowGraphEdges) ois.readObject();
        
        Assert.assertEquals(edges, deserializedEdges);
    }
}
