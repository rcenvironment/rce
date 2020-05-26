/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Provides methods for inspecting the current network state and creating RPC service proxies to method perform calls on remote nodes.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public interface CommunicationService {

    /**
     * @return the ids of the reachable nodes in the known topology
     */
    Set<InstanceNodeSessionId> getReachableInstanceNodes();

    /**
     * @return the {@link LogicalNodeId}s of the reachable nodes in the known topology; note that these are returned instead of
     *         {@link LogicalNodeSessionId}s to support the migration from plain instance ids
     */
    Set<LogicalNodeId> getReachableLogicalNodes();

    /**
     * Returns a disconnected snapshot of the current network state. Changes to the network state will not affect the returned model.
     * 
     * @return a disconnected model of the current network
     */
    // NetworkGraph getCurrentNetworkSnapshot();

    /**
     * Creates a Reliable RPC Stream on the target node and returns a local handle for it. This handle can be used as a
     * {@link NetworkDestination} in methods that support it to enable reliable RPC handling for the service calls invoked using it.
     * 
     * @param targetNodeId the remote node to create the reliable RPC stream to
     * @return the handle of the created stream (which can be used to fetch remotable service proxies as with plain node ids)
     * @throws RemoteOperationException if establishing the stream with the remote node fails
     */
    ReliableRPCStreamHandle createReliableRPCStream(ResolvableNodeId targetNodeId) throws RemoteOperationException;

    /**
     * Closes/disposes the Reliable RPC Stream identified by the given handle. If the remote node is currently unreachable, then the stream
     * can not actually be closed/disposed on the remote side, but it will still be closed at the local node, discarding any future or
     * pending request attempts.
     * 
     * @param streamHandle the handle of the stream to close
     * @throws RemoteOperationException if the remote node could not be reached for a clean stream shutdown; if this happens, the stream has
     *         still been closed locally
     */
    void closeReliableRPCStream(ReliableRPCStreamHandle streamHandle) throws RemoteOperationException;

    /**
     * Returns an instance of a remote service registered with the given interface at the local OSGi registry or a proxy of a remote
     * service.
     * 
     * @param <T> return type.
     * @param iface The interface of the service to get.
     * @param nodeId the id of the platform the desired service is registered; passing an id referring to the local instance is permitted,
     *        passing null is not; depending on the type of id given, it may require resolution to a more specific id, which may fail
     * @return An instance of the service if local or a proxy of a service of remote.
     * @throws IllegalArgumentException if the given service is not a {@link RemotableService}
     */
    <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) throws IllegalArgumentException;

    /**
     * @param type string identifier defining the desired output
     * @return a human-readable summary of the current network state; intended for logging and administrative output (for example, on an
     *         interactive console)
     */
    String getFormattedNetworkInformation(String type);

}
