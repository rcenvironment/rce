/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.utils.common.AutoCreationMap;

/**
 * A service that listens for low-level {@link RawNodePropertiesChangeListener} and {@link NetworkTopologyChangeListener} events, and
 * converts them into higher-level {@link NodePropertiesChangeListener} events.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesStateServiceImpl implements AdditionalServicesProvider {

    private static final Set<NodeProperty> IMMUTABLE_EMPTY_PROPERTY_SET = Collections.unmodifiableSet(new HashSet<NodeProperty>());

    private final AutoCreationMap<InstanceNodeSessionId, Map<String, NodeProperty>> propertyObjectMapsByNode =
        new AutoCreationMap<InstanceNodeSessionId, Map<String, NodeProperty>>() {

            protected Map<String, NodeProperty> createNewEntry(InstanceNodeSessionId key) {
                return new HashMap<String, NodeProperty>();
            };
        };

    private final Map<InstanceNodeSessionId, Map<String, String>> immutableValueMapsOfNodes =
        new HashMap<InstanceNodeSessionId, Map<String, String>>();

    private final Map<InstanceNodeSessionId, Map<String, String>> immutableValueMapsOfReachableNodes =
        new HashMap<InstanceNodeSessionId, Map<String, String>>();

    private Set<InstanceNodeSessionId> reachableNodes = new HashSet<InstanceNodeSessionId>();

    private final Set<NodeProperty> reachableProperties = new HashSet<NodeProperty>();

    private final AsyncOrderedCallbackManager<NodePropertiesChangeListener> callbackManager;

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    public NodePropertiesStateServiceImpl() {
        callbackManager =
            ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        final List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(RawNodePropertiesChangeListener.class, new RawNodePropertiesChangeListener() {

            @Override
            public void onRawNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties) {
                updateOnRawPropertiesAddedOrModified(newProperties);
            }

        }));
        result.add(new AdditionalServiceDeclaration(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

            @Override
            public void onReachableNodesChanged(Set<InstanceNodeSessionId> newReachableNodes, Set<InstanceNodeSessionId> addedNodes,
                Set<InstanceNodeSessionId> removedNodes) {
                NodePropertiesStateServiceImpl.this.updateOnReachableNodesChanged(newReachableNodes, addedNodes, removedNodes);
            }
        }));
        return result;
    }

    /**
     * Registers a new {@link NodePropertiesChangeListener}. A {@link NodePropertiesChangeListener#onReachableNodePropertiesChanged()}
     * callback will be triggered immediately, with all currently connected/reachable {@link NodeProperty}s as "added" properties.
     * 
     * @param listener the new listener
     */
    public synchronized void addNodePropertiesChangeListener(NodePropertiesChangeListener listener) {
        // create copies in synchronized block block
        final Set<NodeProperty> reachablePropertiesCopy = new HashSet<NodeProperty>(reachableProperties);
        final Map<InstanceNodeSessionId, Map<String, String>> valueMapsDeltaCopy =
            Collections.unmodifiableMap(immutableValueMapsOfReachableNodes);
        // send initial callback
        callbackManager.addListenerAndEnqueueCallback(listener, new AsyncCallback<NodePropertiesChangeListener>() {

            @Override
            public void performCallback(NodePropertiesChangeListener listener) {
                listener.onReachableNodePropertiesChanged(reachablePropertiesCopy,
                    IMMUTABLE_EMPTY_PROPERTY_SET, IMMUTABLE_EMPTY_PROPERTY_SET);
                listener.onNodePropertyMapsOfNodesChanged(valueMapsDeltaCopy);
            }
        });
    }

    /**
     * Unregisters a {@link NodePropertiesChangeListener}.
     * 
     * @param listener the listener to remove
     */
    public void removeNodePropertiesChangeListener(NodePropertiesChangeListener listener) {
        callbackManager.removeListener(listener);
    }

    private synchronized void updateOnRawPropertiesAddedOrModified(Collection<? extends NodeProperty> newOrUpdatedProperties) {
        Set<NodeProperty> addedProperties = new HashSet<NodeProperty>();
        Set<NodeProperty> updatedProperties = new HashSet<NodeProperty>();
        Set<NodeProperty> removedProperties = new HashSet<NodeProperty>();

        Set<InstanceNodeSessionId> valueMapsToUpdate = new HashSet<InstanceNodeSessionId>();

        for (NodeProperty property : newOrUpdatedProperties) {
            final InstanceNodeSessionId nodeId = property.getInstanceNodeSessionId();

            final Map<String, NodeProperty> propertyObjectMap = propertyObjectMapsByNode.get(nodeId);
            boolean isReachableNode = reachableNodes.contains(nodeId);

            valueMapsToUpdate.add(nodeId);

            if (property.getValue() != null) {
                // register in by-node map
                NodeProperty replacedPropertyInstance = propertyObjectMap.put(property.getKey(), property);
                boolean wasPresent = replacedPropertyInstance != null;
                if (isReachableNode) {
                    reachableProperties.add(property);
                    if (wasPresent) {
                        updatedProperties.add(property);
                    } else {
                        addedProperties.add(property);
                    }
                }
            } else {
                // remove from by-node map
                propertyObjectMap.remove(property.getKey());
                if (isReachableNode) {
                    removedProperties.add(property);
                    reachableProperties.remove(property);
                }
                propertyObjectMap.remove(property.getKey());
            }
        }

        if (!addedProperties.isEmpty() || !updatedProperties.isEmpty() || !removedProperties.isEmpty()) {
            final Set<NodeProperty> addedPropertiesCopy = Collections.unmodifiableSet(addedProperties);
            final Set<NodeProperty> updatedPropertiesCopy = Collections.unmodifiableSet(updatedProperties);
            final Set<NodeProperty> removedPropertiesCopy = Collections.unmodifiableSet(removedProperties);

            if (verboseLogging) {
                log.debug(StringUtils.format("Reporting node property state change: %d properties added, %d updated, %d removed",
                    addedPropertiesCopy.size(), updatedPropertiesCopy.size(), removedPropertiesCopy.size()));
            }

            callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {

                @Override
                public void performCallback(NodePropertiesChangeListener listener) {
                    listener.onReachableNodePropertiesChanged(addedPropertiesCopy, updatedPropertiesCopy, removedPropertiesCopy);
                }
            });
        }

        Map<InstanceNodeSessionId, Map<String, String>> valueMapsDelta = new HashMap<InstanceNodeSessionId, Map<String, String>>();
        for (InstanceNodeSessionId nodeId : valueMapsToUpdate) {
            // construct new value map for each modified node
            Map<String, String> valueMap = createImmutableValueMapForNode(nodeId);
            if (reachableNodes.contains(nodeId)) {
                // add to "delta" map if this node is reachable
                valueMapsDelta.put(nodeId, valueMap);
                // add to state-keeping map of *reachable* nodes
                immutableValueMapsOfReachableNodes.put(nodeId, valueMap);
            }
            // add to state-keeping map of *all* nodes
            immutableValueMapsOfNodes.put(nodeId, valueMap);
        }
        // TODO check: can this be safely merged with the other callback (regarding asynchronous topology changes)? - misc_ro
        if (!valueMapsDelta.isEmpty()) {
            final Map<InstanceNodeSessionId, Map<String, String>> valueMapsDeltaCopy = Collections.unmodifiableMap(valueMapsDelta);
            callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {

                @Override
                public void performCallback(NodePropertiesChangeListener listener) {
                    listener.onNodePropertyMapsOfNodesChanged(valueMapsDeltaCopy);
                }
            });
        }
    }

    private synchronized void updateOnReachableNodesChanged(Set<InstanceNodeSessionId> newReachableNodes,
        Set<InstanceNodeSessionId> addedNodes, Set<InstanceNodeSessionId> removedNodes) {

        Map<InstanceNodeSessionId, Map<String, String>> valueMapsDelta = new HashMap<InstanceNodeSessionId, Map<String, String>>();

        // find now-disconnected properties
        List<NodeProperty> disconnectedProperties = new ArrayList<NodeProperty>();
        for (InstanceNodeSessionId nodeId : removedNodes) {
            Map<String, NodeProperty> nodePropertyMap = propertyObjectMapsByNode.get(nodeId);
            if (nodePropertyMap != null) {
                disconnectedProperties.addAll(nodePropertyMap.values());
            }
            valueMapsDelta.put(nodeId, null);
            // adapt (reduce) map of reachable node value maps
            immutableValueMapsOfReachableNodes.remove(nodeId);
        }

        // find reconnected properties
        List<NodeProperty> reconnectedProperties = new ArrayList<NodeProperty>();
        for (InstanceNodeSessionId nodeId : addedNodes) {
            Map<String, NodeProperty> nodePropertyMap = propertyObjectMapsByNode.get(nodeId);
            if (nodePropertyMap != null) {
                reconnectedProperties.addAll(nodePropertyMap.values());
            }
            Map<String, String> immutableValueMap = immutableValueMapsOfNodes.get(nodeId);
            valueMapsDelta.put(nodeId, immutableValueMap);
            // adapt map of reachable node value maps
            immutableValueMapsOfReachableNodes.put(nodeId, immutableValueMap);
        }

        // if (!disconnectedProperties.isEmpty()) {
        // final Collection<NodeProperty> disconnectedPropertiesCopy = Collections.unmodifiableCollection(disconnectedProperties);
        // callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {
        //
        // @Override
        // public void performCallback(NodePropertiesChangeListener listener) {
        // listener.onNodePropertiesDisconnected(disconnectedPropertiesCopy);
        // }
        // });
        // }

        // if (!reconnectedProperties.isEmpty()) {
        // final Collection<NodeProperty> reconnectedPropertiesCopy = Collections.unmodifiableCollection(reconnectedProperties);
        // callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {
        //
        // @Override
        // public void performCallback(NodePropertiesChangeListener listener) {
        // listener.onNodePropertiesReconnected(reconnectedPropertiesCopy);
        // }
        // });
        // }

        if (!disconnectedProperties.isEmpty() || !reconnectedProperties.isEmpty()) {
            if (verboseLogging) {
                log.debug(StringUtils.format(
                    "Reporting node property state change after topology change: %d properties disconnected, %d reconnected",
                    disconnectedProperties.size(), reconnectedProperties.size()));
            }

            final Collection<NodeProperty> disconnectedPropertiesCopy = Collections.unmodifiableCollection(disconnectedProperties);
            final Collection<NodeProperty> reconnectedPropertiesCopy = Collections.unmodifiableCollection(reconnectedProperties);
            final Collection<NodeProperty> updatedPropertiesDummy = Collections.unmodifiableCollection(new ArrayList<NodeProperty>());

            callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {

                @Override
                public void performCallback(NodePropertiesChangeListener listener) {
                    listener.onReachableNodePropertiesChanged(reconnectedPropertiesCopy, updatedPropertiesDummy,
                        disconnectedPropertiesCopy);
                }
            });
        }

        // TODO check: can this be safely merged with the other callback (regarding asynchronous topology changes)? - misc_ro
        if (!valueMapsDelta.isEmpty()) {
            final Map<InstanceNodeSessionId, Map<String, String>> valueMapsDeltaCopy = Collections.unmodifiableMap(valueMapsDelta);
            callbackManager.enqueueCallback(new AsyncCallback<NodePropertiesChangeListener>() {

                @Override
                public void performCallback(NodePropertiesChangeListener listener) {
                    listener.onNodePropertyMapsOfNodesChanged(valueMapsDeltaCopy);
                }
            });
        }

        reachableProperties.addAll(reconnectedProperties);
        reachableProperties.removeAll(disconnectedProperties);

        reachableNodes = newReachableNodes;
    }

    private Map<String, String> createImmutableValueMapForNode(InstanceNodeSessionId nodeId) {
        final Map<String, NodeProperty> propertyObjectMap = propertyObjectMapsByNode.get(nodeId);
        Map<String, String> valueMap = new HashMap<String, String>();
        for (NodeProperty property : propertyObjectMap.values()) {
            valueMap.put(property.getKey(), property.getValue());
        }
        // make immutable
        return Collections.unmodifiableMap(valueMap);
    }
}
