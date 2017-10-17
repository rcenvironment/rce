/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import java.util.Set;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.utils.common.rpc.RemotableService;

/**
 * Convenient service serving as a distribute abstraction layer for the services of the communication bundle: {@link PlatformService},
 * {@link ServiceProxyFactory}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
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
    <T> T getRemotableService(Class<T> iface, ResolvableNodeId nodeId) throws IllegalArgumentException;

    /**
     * @param type string identifier defining the desired output
     * @return a human-readable summary of the current network state; intended for logging and administrative output (for example, on an
     *         interactive console)
     */
    String getFormattedNetworkInformation(String type);

}
