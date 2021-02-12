/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.internal.LiveNetworkIdResolutionServiceImpl;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.management.BenchmarkSubtask;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;

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
        final InstanceNodeSessionId localNode = createRandomInstanceNodeSessionId();
        final InstanceNodeSessionId serverNode1 = createRandomInstanceNodeSessionId();
        final InstanceNodeSessionId serverNode2 = createRandomInstanceNodeSessionId();
        final Set<InstanceNodeSessionId> availableNodes = new HashSet<InstanceNodeSessionId>();
        // set up service and inject stubs/mocks
        BenchmarkServiceImpl service = new BenchmarkServiceImpl();
        service.bindPlatformService(new PlatformServiceDefaultStub() {

            @Override
            public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
                return localNode;
            }

            @Override
            public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
                return nodeId.equals(localNode);
            }
        });
        service.bindCommunicationService(new CommunicationServiceDefaultStub() {

            @Override
            public Set<InstanceNodeSessionId> getReachableInstanceNodes() {
                return availableNodes;
            }

        });
        service.bindLiveNetworkIdResolutionService(new LiveNetworkIdResolutionService() {

            @Override
            public LogicalNodeSessionId resolveToLogicalNodeSessionId(ResolvableNodeId id) throws IdentifierException {
                return null;
            }

            @Override
            public InstanceNodeSessionId resolveInstanceNodeIdStringToInstanceNodeSessionId(String input) throws IdentifierException {
                final LiveNetworkIdResolutionServiceImpl idResolver = new LiveNetworkIdResolutionServiceImpl();
                assertFalse(availableNodes.isEmpty());
                for (InstanceNodeSessionId id : availableNodes) {
                    idResolver.registerInstanceNodeSessionId(id);
                }
                return idResolver.resolveInstanceNodeIdStringToInstanceNodeSessionId(input);
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
        // TODO needs mechanism to find "default" instance session id for given instance id (and ideally, support both)
        setup = service.parseBenchmarkDescription(serverNode1.getInstanceNodeIdString() + "(,,,,)");
        assertEquals(1, setup.getSubtasks().size());
        subtask = setup.getSubtasks().get(0);
        assertEquals(1, subtask.getTargetNodes().size());
        assertTrue(subtask.getTargetNodes().contains(serverNode1));

        // verify multi-subtask string parsing
        // TODO improve test
        setup = service.parseBenchmarkDescription("*(,,,,);*(,,,,)");
        assertEquals(2, setup.getSubtasks().size());
    }

    private InstanceNodeSessionId createRandomInstanceNodeSessionId() {
        return NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
    }

}
