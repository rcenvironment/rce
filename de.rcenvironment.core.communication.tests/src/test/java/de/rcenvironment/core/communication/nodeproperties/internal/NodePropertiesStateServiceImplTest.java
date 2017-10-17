/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.service.MockAdditionalServicesRegistrationService;

/**
 * {@link NodePropertiesStateServiceImpl} unit test.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesStateServiceImplTest {

    private static final String KEY_A = "keyA";

    private static final String KEY_B = "keyB";

    private static final String VALUE_A = "valueA";

    private static final String VALUE_A2 = "valueA2";

    private static final String VALUE_B = "valueB";

    private static final String VALUE_B2 = "valueB2";

    private static final int ASYNC_EVENTS_WAIT_MSEC = 200;

    private NodePropertiesStateServiceImpl service;

    private RawNodePropertiesChangeListener serviceRawNodePropertiesChangeListener;

    private NetworkTopologyChangeListener serviceNetworkTopologyChangeListener;

    private CallbackCollector callbackCollector;

    private final InstanceNodeSessionId node1 = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();

    private final InstanceNodeSessionId node2 = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();

    private final String node1InstanceSessionIdString = node1.getInstanceNodeSessionIdString();

    private final String node2InstanceSessionIdString = node2.getInstanceNodeSessionIdString();

    private final Set<InstanceNodeSessionId> emptySet;

    private final Set<InstanceNodeSessionId> node1Set;

    private final Set<InstanceNodeSessionId> node2Set;

    private final Set<InstanceNodeSessionId> node12Set;

    /**
     * Simple holder for the parameters of a NodePropertiesChangeListener event.
     * 
     * @author Robert Mischke
     */
    private class CallbackHolder {

        public final Collection<? extends NodeProperty> added;

        public final Collection<? extends NodeProperty> updated;

        public final Collection<? extends NodeProperty> removed;

        CallbackHolder(Collection<? extends NodeProperty> addedSet, Collection<? extends NodeProperty> updatedSet,
            Collection<? extends NodeProperty> removedSet) {
            this.added = addedSet;
            this.updated = updatedSet;
            this.removed = removedSet;
        }

        public void verifySetSizes(int aSize, int uSize, int rSize) {
            assertEquals(aSize, added.size());
            assertEquals(uSize, updated.size());
            assertEquals(rSize, removed.size());
        }

    }

    /**
     * {@link NodePropertiesChangeListener} implementation that collects callbacks in an internal list.
     * 
     * @author Robert Mischke
     */
    private class CallbackCollector implements NodePropertiesChangeListener {

        private final List<CallbackHolder> queue = Collections.synchronizedList(new ArrayList<CallbackHolder>());

        private final Map<InstanceNodeSessionId, Map<String, String>> propertyMapsByNode =
            Collections.synchronizedMap(new HashMap<InstanceNodeSessionId, Map<String, String>>());

        public void resetQueue() {
            queue.clear();
        }

        public int getCount() {
            return queue.size();
        }

        public CallbackHolder get(int index) {
            return queue.get(index);
        }

        public Map<String, String> getPropertyMapForNode(InstanceNodeSessionId nodeId) {
            return propertyMapsByNode.get(nodeId);
        }

        @Override
        public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
            Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
            queue.add(new CallbackHolder(addedProperties, updatedProperties, removedProperties));
        }

        @Override
        public void onNodePropertyMapsOfNodesChanged(Map<InstanceNodeSessionId, Map<String, String>> updatedPropertyMaps) {
            propertyMapsByNode.putAll(updatedPropertyMaps); // inner maps are immutable
        }
    }

    public NodePropertiesStateServiceImplTest() {
        emptySet = new HashSet<InstanceNodeSessionId>();
        node1Set = new HashSet<InstanceNodeSessionId>();
        node1Set.add(node1);
        node2Set = new HashSet<InstanceNodeSessionId>();
        node2Set.add(node2);
        node12Set = new HashSet<InstanceNodeSessionId>();
        node12Set.add(node1);
        node12Set.add(node2);
    }

    /**
     * Test setup.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Before
    public void setUp() throws Exception {
        ConcurrencyUtils.getThreadPoolManagement().reset();

        service = new NodePropertiesStateServiceImpl();

        callbackCollector = new CallbackCollector();

        MockAdditionalServicesRegistrationService listenerRegistrationService = new MockAdditionalServicesRegistrationService();
        listenerRegistrationService.registerAdditionalServicesProvider(service);
        serviceRawNodePropertiesChangeListener =
            listenerRegistrationService.getListeners(RawNodePropertiesChangeListener.class).iterator().next();
        serviceNetworkTopologyChangeListener =
            listenerRegistrationService.getListeners(NetworkTopologyChangeListener.class).iterator().next();
    }

    /**
     * Tests for proper callback on adding a listener to a service with no properties yet.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testListenerAdditionOnCleanService() throws InterruptedException {
        // subscribe
        service.addNodePropertiesChangeListener(callbackCollector);

        // verify: there should have been a callback with empty change sets
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        CallbackHolder callbackHolder = callbackCollector.get(0);
        assertEquals(0, callbackHolder.added.size());
        assertEquals(0, callbackHolder.updated.size());
        assertEquals(0, callbackHolder.removed.size());

        // check maps
        assertEquals(null, callbackCollector.getPropertyMapForNode(node1));
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));
    }

    /**
     * Tests for proper callback on adding a listener to a service with known properties.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testListenerAdditionAfterPropertiesKnown() throws InterruptedException {
        CallbackHolder currentCallback;
        NodeProperty callbackProperty;

        // set reachable nodes (node 1 only)
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node1Set, node1Set, emptySet);

        // add a property for a reachable node
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node1InstanceSessionIdString, KEY_A, "201", VALUE_A)));
        // add an property for an unreachable node
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node2InstanceSessionIdString, KEY_B, "202", VALUE_B)));

        // subscribe
        service.addNodePropertiesChangeListener(callbackCollector);

        // verify callback parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);

        callbackProperty = currentCallback.added.iterator().next();
        assertEquals(node1InstanceSessionIdString, callbackProperty.getInstanceNodeSessionIdString());
        assertEquals(KEY_A, callbackProperty.getKey());
        assertEquals(VALUE_A, callbackProperty.getValue());
        assertEquals(0, currentCallback.updated.size());
        assertEquals(0, currentCallback.removed.size());

        // check maps
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));
    }

    /**
     * Tests for proper callbacks on property change events.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testListenerCallbacksOnPropertyChanges() throws InterruptedException {
        CallbackHolder currentCallback;
        NodeProperty callbackProperty;

        // set reachable nodes (node 1 only)
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node1Set, node1Set, emptySet);

        // subscribe
        service.addNodePropertiesChangeListener(callbackCollector);

        // verify that the callback happened; content not checked as there is a separate test for that
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());

        // check maps
        assertEquals(null, callbackCollector.getPropertyMapForNode(node1));
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));

        callbackCollector.resetQueue();

        // add a property
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node1InstanceSessionIdString, KEY_A, "301", VALUE_A)));

        // verify callback and parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);
        assertEquals(1, currentCallback.added.size());
        callbackProperty = currentCallback.added.iterator().next();
        assertEquals(node1InstanceSessionIdString, callbackProperty.getInstanceNodeSessionIdString());
        assertEquals(KEY_A, callbackProperty.getKey());
        assertEquals(VALUE_A, callbackProperty.getValue());
        assertEquals(0, currentCallback.updated.size());
        assertEquals(0, currentCallback.removed.size());

        // check maps
        assertNotNull(callbackCollector.getPropertyMapForNode(node1));
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));

        callbackCollector.resetQueue();

        // add another property
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node1InstanceSessionIdString, KEY_B, "302", VALUE_B)));

        // verify callback and parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);

        assertEquals(1, currentCallback.added.size());
        callbackProperty = currentCallback.added.iterator().next();
        assertEquals(node1InstanceSessionIdString, callbackProperty.getInstanceNodeSessionIdString());
        assertEquals(KEY_B, callbackProperty.getKey());
        assertEquals(VALUE_B, callbackProperty.getValue());
        assertEquals(0, currentCallback.updated.size());
        assertEquals(0, currentCallback.removed.size());

        // check maps
        assertNotNull(callbackCollector.getPropertyMapForNode(node1));
        assertEquals(2, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertEquals(VALUE_B, callbackCollector.getPropertyMapForNode(node1).get(KEY_B));
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));

        callbackCollector.resetQueue();

        // update property "keyA"
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node1InstanceSessionIdString, KEY_A, "303", VALUE_A2)));

        // verify callback and parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);

        assertEquals(0, currentCallback.added.size());
        assertEquals(1, currentCallback.updated.size());
        callbackProperty = currentCallback.updated.iterator().next();
        assertEquals(node1InstanceSessionIdString, callbackProperty.getInstanceNodeSessionIdString());
        assertEquals(KEY_A, callbackProperty.getKey());
        assertEquals(VALUE_A2, callbackProperty.getValue());
        assertEquals(0, currentCallback.removed.size());

        // check maps
        assertNotNull(callbackCollector.getPropertyMapForNode(node1));
        assertEquals(2, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A2, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertEquals(VALUE_B, callbackCollector.getPropertyMapForNode(node1).get(KEY_B));
        assertEquals(null, callbackCollector.getPropertyMapForNode(node2));

        // TODO continue
    }

    /**
     * Tests for proper callbacks on property change events.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testListenerCallbacksOnTopologyChanges() throws InterruptedException {
        CallbackHolder currentCallback;
        NodeProperty currentProperty;

        // set reachable nodes (node 1 only)
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node1Set, node1Set, emptySet);

        // add a "local" property
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node1InstanceSessionIdString, KEY_A, "401", VALUE_A)));

        // "connect" node2
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node12Set, node2Set, emptySet);

        // add a "remote" property
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node2InstanceSessionIdString, KEY_B, "402", VALUE_B)));

        // subscribe
        service.addNodePropertiesChangeListener(callbackCollector);

        // verify that the callback happened; content not checked as there is a separate test for that
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());

        // check maps
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertEquals(1, callbackCollector.getPropertyMapForNode(node2).size());
        assertEquals(VALUE_B, callbackCollector.getPropertyMapForNode(node2).get(KEY_B));

        callbackCollector.resetQueue();

        // "disconnect" node2
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node1Set, emptySet, node2Set);

        // verify callback and parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());

        currentCallback = callbackCollector.get(0);
        assertEquals(0, currentCallback.added.size());
        assertEquals(0, currentCallback.updated.size());
        assertEquals(1, currentCallback.removed.size());
        verifyPropertyValues(currentCallback.removed.iterator().next(), node2InstanceSessionIdString, KEY_B, VALUE_B);

        // check maps
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertNull(callbackCollector.getPropertyMapForNode(node2));

        callbackCollector.resetQueue();

        // create a property change for the disconnected node; this can happen in practice
        // as changes are processed asynchronously
        serviceRawNodePropertiesChangeListener.onRawNodePropertiesAddedOrModified(createTestProperties(
            StringUtils.escapeAndConcat(node2InstanceSessionIdString, KEY_B, "403", VALUE_B2)));

        // verify that no change event was fired for the disconnected node
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(0, callbackCollector.getCount());

        // check maps; should be unmodified as well
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertNull(callbackCollector.getPropertyMapForNode(node2));

        callbackCollector.resetQueue();

        // with the disconnected node2, unregister and re-register the listener to check the initial callback
        service.removeNodePropertiesChangeListener(callbackCollector);
        service.addNodePropertiesChangeListener(callbackCollector);

        // verify callback and parameters
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);
        currentCallback.verifySetSizes(1, 0, 0);
        currentProperty = currentCallback.added.iterator().next();
        verifyPropertyValues(currentProperty, node1InstanceSessionIdString, KEY_A, VALUE_A);

        callbackCollector.resetQueue();

        // "reconnect" node2
        serviceNetworkTopologyChangeListener.onReachableNodesChanged(node12Set, node2Set, emptySet);

        // verify callback and parameters; the reconnected property should cause an "added" event
        Thread.sleep(ASYNC_EVENTS_WAIT_MSEC);
        assertEquals(1, callbackCollector.getCount());
        currentCallback = callbackCollector.get(0);
        assertEquals(1, currentCallback.added.size());
        verifyPropertyValues(currentCallback.added.iterator().next(), node2InstanceSessionIdString, KEY_B, VALUE_B2);
        assertEquals(0, currentCallback.updated.size());
        assertEquals(0, currentCallback.removed.size());

        // check maps
        assertEquals(1, callbackCollector.getPropertyMapForNode(node1).size());
        assertEquals(VALUE_A, callbackCollector.getPropertyMapForNode(node1).get(KEY_A));
        assertEquals(1, callbackCollector.getPropertyMapForNode(node2).size());
        assertEquals(VALUE_B2, callbackCollector.getPropertyMapForNode(node2).get(KEY_B));

        // TODO continue
    }

    private void verifyPropertyValues(NodeProperty callbackProperty, String nodeIdString, String key, String value) {
        assertEquals(nodeIdString, callbackProperty.getInstanceNodeSessionIdString());
        assertEquals(key, callbackProperty.getKey());
        assertEquals(value, callbackProperty.getValue());
    }

    private List<NodeProperty> createTestProperties(String... values) {
        final List<NodeProperty> result = new ArrayList<NodeProperty>();
        for (String value : values) {
            try {
                result.add(new NodePropertyImpl(value, NodeIdentifierTestUtils.getTestNodeIdentifierService()));
            } catch (IdentifierException e) {
                throw NodeIdentifierUtils.wrapIdentifierException(e);
            }
        }
        return result;
    }
}
