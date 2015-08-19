/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.management.BenchmarkSubtask;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.utils.incubator.IdGenerator;

/**
 * Tests for {@link BenchmarkServiceImplTest}.
 * 
 * @author Robert Mischke
 */
public class BenchmarkServiceImplTest {

    /**
     * Various tests for {@link BenchmarkServiceImpl#parseBenchmarkDescription(String)}.
     */
    @Test
    public void testDescriptionParsing() {
        // define network test environment
        final NodeIdentifier localNode = NodeIdentifierFactory.fromNodeId(IdGenerator.randomUUIDWithoutDashes());
        NodeIdentifier serverNode1 = NodeIdentifierFactory.fromNodeId(IdGenerator.randomUUIDWithoutDashes());
        NodeIdentifier serverNode2 = NodeIdentifierFactory.fromNodeId(IdGenerator.randomUUIDWithoutDashes());
        final Set<NodeIdentifier> availableNodes = new HashSet<NodeIdentifier>();
        // set up service and inject stubs/mocks
        BenchmarkServiceImpl service = new BenchmarkServiceImpl();
        service.bindPlatformService(new PlatformServiceDefaultStub() {

            @Override
            public NodeIdentifier getLocalNodeId() {
                return localNode;
            }

            @Override
            public boolean isLocalNode(NodeIdentifier nodeId) {
                return nodeId.equals(localNode);
            }
        });
        service.bindCommunicationService(new CommunicationServiceDefaultStub() {

            @Override
            public Set<NodeIdentifier> getReachableNodes() {
                return availableNodes;
            }
        });
        // test invalid parameters
        try {
            service.parseBenchmarkDescription(null);
        } catch (RuntimeException e) {
            assertTrue(e instanceof NullPointerException);
        }
        try {
            service.parseBenchmarkDescription("");
        } catch (RuntimeException e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        BenchmarkSetup setup;
        BenchmarkSubtask subtask;

        // prepare next test: populate virtual network with local node only
        availableNodes.clear();
        availableNodes.add(localNode);
        // test with minimal description: "**"=contact all nodes, "(,,,,)" = use default values
        setup = service.parseBenchmarkDescription("**(,,,,)");
        assertEquals(1, setup.getSubtasks().size());
        subtask = setup.getSubtasks().get(0);
        // check default values
        assertEquals(1, subtask.getNumMessages());
        assertEquals(1, subtask.getRequestSize());
        assertEquals(1, subtask.getResponseSize());
        assertEquals(0, subtask.getResponseDelay());

        // prepare next test: populate virtual network with three nodes
        availableNodes.clear();
        availableNodes.add(localNode);
        availableNodes.add(serverNode1);
        availableNodes.add(serverNode2);

        // verify that "**" uses the whole network
        setup = service.parseBenchmarkDescription("**(,,,,)");
        assertEquals(1, setup.getSubtasks().size());
        subtask = setup.getSubtasks().get(0);
        assertEquals(3, subtask.getTargetNodes().size());
        assertTrue(subtask.getTargetNodes().contains(localNode));

        // verify that "*" excludes the local node
        setup = service.parseBenchmarkDescription("*(,,,,)");
        assertEquals(1, setup.getSubtasks().size());
        subtask = setup.getSubtasks().get(0);
        assertEquals(2, subtask.getTargetNodes().size());
        assertFalse(subtask.getTargetNodes().contains(localNode));

        // verify explicit target declaration
        setup = service.parseBenchmarkDescription(serverNode1.getIdString() + "(,,,,)");
        assertEquals(1, setup.getSubtasks().size());
        subtask = setup.getSubtasks().get(0);
        assertEquals(1, subtask.getTargetNodes().size());
        assertTrue(subtask.getTargetNodes().contains(serverNode1));

        // verify multi-subtask string parsing
        // TODO improve test
        setup = service.parseBenchmarkDescription("*(,,,,);*(,,,,)");
        assertEquals(2, setup.getSubtasks().size());
    }

}
