/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.internal.NetworkGraphImpl;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallback;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedCallbackManager;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Keeps track of the last known topology information, and generates change events on relevant updates.
 * 
 * @author Robert Mischke
 */
class NetworkTopologyChangeTracker {

    private Set<NodeIdentifier> lastReachableNodes; // invariant: must always contain an *immutable* set

    private final AsyncOrderedCallbackManager<NetworkTopologyChangeListener> callbackManager;

    private NetworkGraphImpl cachedReachableNetworkGraph;

    public NetworkTopologyChangeTracker() {
        this.lastReachableNodes = Collections.unmodifiableSet(new HashSet<NodeIdentifier>()); // see invariant
        this.callbackManager = new AsyncOrderedCallbackManager<NetworkTopologyChangeListener>(SharedThreadPool.getInstance(),
            AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
    }

    public synchronized boolean updateReachableNetwork(final NetworkGraphImpl reachableNetworkGraph) {
        Set<NodeIdentifier> addedNodes;
        Set<NodeIdentifier> removedNodes;

        Set<? extends NodeIdentifier> reachableNodeIds = reachableNetworkGraph.getNodeIds();
        // create difference sets
        addedNodes = new HashSet<NodeIdentifier>(reachableNodeIds);
        addedNodes.removeAll(lastReachableNodes);
        removedNodes = new HashSet<NodeIdentifier>(lastReachableNodes);
        removedNodes.removeAll(reachableNodeIds);

        callbackManager.enqueueCallback(new AsyncCallback<NetworkTopologyChangeListener>() {

            @Override
            @TaskDescription("Communication Layer: Topology change callback (1p)")
            public void performCallback(NetworkTopologyChangeListener listener) {
                listener.onReachableNetworkChanged(reachableNetworkGraph);
            }
        });

        cachedReachableNetworkGraph = reachableNetworkGraph;

        if (!addedNodes.isEmpty() || !removedNodes.isEmpty()) {
            // create thread-safe copies of sets
            final Set<NodeIdentifier> newReachableNodesCopy = Collections.unmodifiableSet(reachableNodeIds);
            final Set<NodeIdentifier> addedNodesCopy = Collections.unmodifiableSet(addedNodes);
            final Set<NodeIdentifier> removedNodesCopy = Collections.unmodifiableSet(removedNodes);

            callbackManager.enqueueCallback(new AsyncCallback<NetworkTopologyChangeListener>() {

                @Override
                @TaskDescription("Communication Layer: Topology change callback (3p)")
                public void performCallback(NetworkTopologyChangeListener listener) {
                    listener.onReachableNodesChanged(newReachableNodesCopy, addedNodesCopy, removedNodesCopy);
                }
            });
            lastReachableNodes = newReachableNodesCopy; // see invariant
            sendLegacyListenerNotification();
            return true;
        } else {
            sendLegacyListenerNotification();
            return false;
        }
    }

    public synchronized Set<NodeIdentifier> getCurrentReachableNodes() {
        return lastReachableNodes; // immutable set; see invariant
    }

    /**
     * Adds a new {@link NetworkTopologyChangeListener}.
     * 
     * @param listener the listener
     */
    public synchronized void addListener(NetworkTopologyChangeListener listener) {
        // make copies in synchronized block
        final Set<NodeIdentifier> lastReachableNodesCopy = lastReachableNodes;
        final NetworkGraphImpl networkGraphCopy = cachedReachableNetworkGraph;
        callbackManager.addListenerAndEnqueueCallback(listener, new AsyncCallback<NetworkTopologyChangeListener>() {

            @Override
            public void performCallback(NetworkTopologyChangeListener listener) {
                // send specific callback to bring the listener up to date
                listener.onReachableNodesChanged(lastReachableNodesCopy, lastReachableNodesCopy, new HashSet<NodeIdentifier>());
                if (networkGraphCopy != null) {
                    listener.onReachableNetworkChanged(networkGraphCopy);
                } else {
                    // this case is harmless; it would be nice to change it for consistency, though, if possible - misc_ro
                    LogFactory.getLog(getClass()).debug(
                        "Topology listener " + listener.getClass().getName()
                            + " registered before an initial network graph was set; skipping initial callback");
                }
            }
        });
    }

    /**
     * Removes a {@link NetworkTopologyChangeListener}.
     * 
     * @param listener the listener
     */
    public synchronized void removeListener(NetworkTopologyChangeListener listener) {
        callbackManager.removeListener(listener);
    }

    private void sendLegacyListenerNotification() {
        // forward low-level event for backward compatibility
        // TODO remove when not used anymore - misc_ro
        callbackManager.enqueueCallback(new AsyncCallback<NetworkTopologyChangeListener>() {

            @Override
            @TaskDescription("Communication Layer: Topology change callback (0p)")
            public void performCallback(NetworkTopologyChangeListener listener) {
                listener.onNetworkTopologyChanged();
            }
        });
    }

}
