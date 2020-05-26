/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.internal.NetworkGraphImpl;
import de.rcenvironment.core.communication.routing.InstanceRestartAndPresenceService;
import de.rcenvironment.core.communication.routing.InstanceSessionNetworkStatus;
import de.rcenvironment.core.communication.routing.InstanceSessionNetworkStatus.State;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Keeps track of the last known topology information, and generates change events on relevant updates.
 * 
 * @author Robert Mischke
 */
class NetworkTopologyChangeTracker implements InstanceRestartAndPresenceService {

    private Set<InstanceNodeSessionId> lastReachableNodes; // invariant: must always contain an *immutable* set

    private final AsyncOrderedCallbackManager<NetworkTopologyChangeListener> callbackManager;

    private NetworkGraphImpl cachedReachableNetworkGraph;

    NetworkTopologyChangeTracker() {
        this.lastReachableNodes = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>()); // see invariant
        this.callbackManager =
            ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
    }

    public synchronized boolean updateReachableNetwork(final NetworkGraphImpl reachableNetworkGraph) {
        final Set<InstanceNodeSessionId> addedNodes;
        final Set<InstanceNodeSessionId> removedNodes;
        final Set<? extends InstanceNodeSessionId> reachableNodeIds = reachableNetworkGraph.getNodeIds();

        // create difference sets
        addedNodes = new HashSet<>(reachableNodeIds);
        addedNodes.removeAll(lastReachableNodes);
        removedNodes = new HashSet<>(lastReachableNodes);
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
            final Set<InstanceNodeSessionId> newReachableNodesCopy = Collections.unmodifiableSet(reachableNodeIds);
            final Set<InstanceNodeSessionId> addedNodesCopy = Collections.unmodifiableSet(addedNodes);
            final Set<InstanceNodeSessionId> removedNodesCopy = Collections.unmodifiableSet(removedNodes);

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

    public synchronized Set<InstanceNodeSessionId> getCurrentReachableNodes() {
        return lastReachableNodes; // immutable set; see invariant
    }

    @Override
    public InstanceSessionNetworkStatus queryInstanceSessionNetworkStatus(InstanceNodeSessionId lookupId) {
        InstanceNodeSessionId firstMatch = null;
        for (InstanceNodeSessionId iterated : lastReachableNodes) {
            if (iterated.isSameInstanceNodeAs(lookupId)) { // note: same INSTANCE, not SESSION id
                if (firstMatch == null) {
                    firstMatch = iterated;
                } else {
                    // there is more than one INSI with the same instance node id present in the current network (usually bad)
                    InstanceNodeSessionId otherMatch;
                    // determine which match is not the queried id
                    if (firstMatch.isSameInstanceNodeSessionAs(lookupId)) {
                        otherMatch = iterated;
                    } else {
                        otherMatch = firstMatch;
                    }
                    return new InstanceSessionNetworkStatus(lookupId, State.ID_COLLISION, otherMatch);
                }
            }
        }
        if (firstMatch != null) {
            // a single match was found
            if (firstMatch.isSameInstanceNodeSessionAs(lookupId)) {
                return new InstanceSessionNetworkStatus(lookupId, State.PRESENT, null); // the session is simply present in the network
            } else {
                return new InstanceSessionNetworkStatus(lookupId, State.PRESENT_WITH_DIFFERENT_SESSION, firstMatch); // typically restarted
            }
        }
        return new InstanceSessionNetworkStatus(lookupId, State.NOT_PRESENT, null); // not present; typically disconnected
    }

    /**
     * Adds a new {@link NetworkTopologyChangeListener}.
     * 
     * @param listener the listener
     */
    public synchronized void addListener(NetworkTopologyChangeListener listener) {
        // make copies in synchronized block
        final Set<InstanceNodeSessionId> lastReachableNodesCopy = lastReachableNodes;
        final NetworkGraphImpl networkGraphCopy = cachedReachableNetworkGraph;
        callbackManager.addListenerAndEnqueueCallback(listener, new AsyncCallback<NetworkTopologyChangeListener>() {

            @Override
            public void performCallback(NetworkTopologyChangeListener listener) {
                // send specific callback to bring the listener up to date
                listener.onReachableNodesChanged(lastReachableNodesCopy, lastReachableNodesCopy, new HashSet<InstanceNodeSessionId>());
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
