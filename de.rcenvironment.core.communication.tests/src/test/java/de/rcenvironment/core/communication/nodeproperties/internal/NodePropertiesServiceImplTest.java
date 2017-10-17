/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Tests {@link NodePropertiesService} update propagation between two {@link VirtualInstance}s.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesServiceImplTest extends AbstractVirtualInstanceTest {

    private static final String TEST_PROPERTY_KEY_A = "a";

    /**
     * A wait time im msec that must be larger than the time over which node property updates are aggegated in the tested service.
     */
    private static final int WAIT_TIME_FOR_UPDATE_AGGREGATION = 400;

    /**
     * Common setup.
     */
    @Before
    public void setup() {
        NodeIdentifierTestUtils.resetTestNodeIdentifierService(false);
    }

    /**
     * Common teardown.
     */
    @After
    public void teardown() {
        NodeIdentifierTestUtils.resetTestNodeIdentifierService(true);
    }

    /**
     * Verifies that display name mappings are separated between virtual instances, and name associations are registered on both sides when
     * connecting.
     * 
     * @throws Exception none expected
     */
    @Test
    public void testDisplayNamePropagationOnConnect() throws Exception {
        setupInstances(2, true, true);

        final VirtualInstance instanceA = testTopology.getInstance(0);
        final VirtualInstance instanceB = testTopology.getInstance(1);

        final NodeIdentifierService nodeIdentifierServiceA = instanceA.getService(NodeIdentifierService.class);
        final NodeIdentifierService nodeIdentifierServiceB = instanceB.getService(NodeIdentifierService.class);

        nodeIdentifierServiceA.printAllNameAssociations(System.out, "Name mappings of instance A:");
        nodeIdentifierServiceB.printAllNameAssociations(System.out, "Name mappings of instance B:");

        assertNull(rawDisplayNameOfFirstAsSeenFromSecond(instanceA, instanceB));
        assertNull(rawDisplayNameOfFirstAsSeenFromSecond(instanceB, instanceA));

        log.debug("Connnecting virtual test instances");
        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

        nodeIdentifierServiceA.printAllNameAssociations(System.out, "Name mappings of instance A:");
        nodeIdentifierServiceB.printAllNameAssociations(System.out, "Name mappings of instance B:");

        assertEquals(getDisplayNameOf(instanceA), rawDisplayNameOfFirstAsSeenFromSecond(instanceA, instanceB));
        assertEquals(getDisplayNameOf(instanceB), rawDisplayNameOfFirstAsSeenFromSecond(instanceB, instanceA));
    }

    /**
     * Verifies that property deletions/removals are properly propagated, and are correctly combined with following updates.
     * 
     * @throws Exception on unexpected errors
     */
    @Test
    public void testNullHandlingInUpdatePropagation() throws Exception {
        setupInstances(2, true, true);

        final VirtualInstance instanceA = testTopology.getInstance(0);
        final VirtualInstance instanceB = testTopology.getInstance(1);
        final NodePropertiesService serviceA = instanceA.getNodePropertiesService();
        final NodePropertiesService serviceB = instanceB.getNodePropertiesService();

        serviceB.addRawNodePropertiesChangeListener(new RawNodePropertiesChangeListener() {

            @Override
            public void onRawNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties) {
                log.debug("'RawNodePropertiesAddedOrModified' listener callback: " + Arrays.toString(newProperties.toArray()));
            }
        });

        instanceB.injectService(NodePropertiesChangeListener.class, new NodePropertiesChangeListener() {

            @Override
            public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                log.debug(StringUtils.format("Added: %d,  Updated: %d, Removed: %d", addedProperties.size(), updatedProperties.size(),
                    removedProperties.size()));
            }

            @Override
            public void onNodePropertyMapsOfNodesChanged(Map<InstanceNodeSessionId, Map<String, String>> updatedPropertyMaps) {
                log.debug("Node property maps of these nodes changed: " + updatedPropertyMaps.keySet());
            }
        });

        Map<InstanceNodeSessionId, Map<String, String>> allPropertiesAtB;
        final InstanceNodeSessionId nodeIdOfA = instanceA.getInstanceNodeSessionId();

        log.debug("Connnecting virtual test instances");
        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);

        log.info("Setup complete, beginning actual node property tests");

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "1");
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals("1", allPropertiesAtB.get(nodeIdOfA).get(TEST_PROPERTY_KEY_A));

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "2");
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, null);
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals(null, allPropertiesAtB.get(nodeIdOfA).get(TEST_PROPERTY_KEY_A));
        // TODO review: should the behavior be changed to remove keys on null value? - misc_ro
        // assertFalse(allPropertiesAtB.get(nodeIdOfA).containsKey(TEST_PROPERTY_KEY_A));

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "3");
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, null);
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "4");
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals("4", allPropertiesAtB.get(nodeIdOfA).get(TEST_PROPERTY_KEY_A));
    }

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(true);
    }

    private String getDisplayNameOf(VirtualInstance instance) {
        return instance.getService(NodeConfigurationService.class).getInitialNodeInformation().getDisplayName();
    }

    private String rawDisplayNameOfFirstAsSeenFromSecond(VirtualInstance virtualInstanceX, VirtualInstance virtualInstanceY)
        throws IdentifierException {
        return virtualInstanceY.getService(NodeIdentifierService.class)
            .parseInstanceNodeSessionIdString(virtualInstanceX.getInstanceNodeSessionId().getInstanceNodeSessionIdString())
            .getRawAssociatedDisplayName();
    }
}
