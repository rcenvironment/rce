/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;

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
     * Verifies that property deletions/removals are properly propagated, and are correctly combined with following updates.
     * 
     * @throws Exception on unexpected errors
     */
    @Test
    public void testNullHandlingInUpdatePropagation() throws Exception {
        setupInstances(2, true, true);
        VirtualInstance instanceA = testTopology.getInstance(0);
        VirtualInstance instanceB = testTopology.getInstance(1);
        testTopology.connectAndWait(0, 1, DEFAULT_NODE_REACHABILITY_TIMEOUT);
        NodePropertiesService serviceA = instanceA.getNodePropertiesService();
        NodePropertiesService serviceB = instanceB.getNodePropertiesService();

        serviceB.addRawNodePropertiesChangeListener(new RawNodePropertiesChangeListener() {

            @Override
            public void onRawNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties) {
                log.debug("addedOrMod: " + Arrays.toString(newProperties.toArray()));
            }
        });

        instanceB.injectService(NodePropertiesChangeListener.class, new NodePropertiesChangeListener() {

            @Override
            public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                log.debug("OK");

            }

            @Override
            public void onNodePropertyMapsOfNodesChanged(Map<NodeIdentifier, Map<String, String>> updatedPropertyMaps) {
                log.debug("OK");
                // TODO Auto-generated method stub

            }
        });

        Map<NodeIdentifier, Map<String, String>> allPropertiesAtB;

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "1");
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals("1", allPropertiesAtB.get(instanceA.getNodeId()).get(TEST_PROPERTY_KEY_A));

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "2");
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, null);
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals(null, allPropertiesAtB.get(instanceA.getNodeId()).get(TEST_PROPERTY_KEY_A));

        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "3");
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, null);
        serviceA.addOrUpdateLocalNodeProperty(TEST_PROPERTY_KEY_A, "4");
        Thread.sleep(WAIT_TIME_FOR_UPDATE_AGGREGATION);
        allPropertiesAtB = serviceB.getAllNodeProperties();
        assertEquals("4", allPropertiesAtB.get(instanceA.getNodeId()).get(TEST_PROPERTY_KEY_A));
    }

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(true);
    }
}
