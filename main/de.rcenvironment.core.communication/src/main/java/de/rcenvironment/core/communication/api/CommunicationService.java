/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import java.util.Set;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;

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
    Set<NodeIdentifier> getReachableNodes();

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
     * @param nodeId The {@link NodeIdentifier} of the platform the desired service is registered; passing the local node's
     *        {@link NodeIdentifier} is permitted, passing null is not
     * @return An instance of the service if local or a proxy of a service of remote.
     * @throws IllegalArgumentException if the given service is not a {@link RemotableService}
     * @throws IllegalStateException if the given service was requested from the local node, but is not available
     */
    <T> T getRemotableService(Class<T> iface, NodeIdentifier nodeId) throws IllegalArgumentException, IllegalStateException;

    /**
     * Returns an instance of a service registered with the given interface at the local OSGi registry or a proxy of a remote service.
     * 
     * @param <T> return type.
     * @param iface The interface of the service to get.
     * @param nodeId The {@link NodeIdentifier} of the platform the desired service is registered. <code>null</code> serves as the local
     *        one.
     * @param bundleContext The {@link BundleContext} to use for getting the service at the OSGi registry.
     * @return An instance of the service if local or a proxy of a service of remote.
     * @throws IllegalStateException if there is no appropriate service found at the local registry. For the remote case no check that the
     *         remote service exists is provided yet.
     */
    @Deprecated
    // only used by own test anymore
    <T> T getService(Class<T> iface, NodeIdentifier nodeId, BundleContext bundleContext) throws IllegalStateException;

    /**
     * Synchronously connects to a network peer at a given {@link NetworkContactPoint}.
     * 
     * @param contactPointDefinition the String representation of the {@link NetworkContactPoint} to connect to; the exact syntax is
     *        transport-specific, but is typically similar to "transportId:host:port"
     * @throws CommunicationException on connection errors
     */
    @Deprecated
    // TODO move to management service
    void addRuntimeNetworkPeer(String contactPointDefinition) throws CommunicationException;

    /**
     * @param type string identifier defining the desired output
     * @return a human-readable summary of the current network state; intended for logging and administrative output (for example, on an
     *         interactive console)
     */
    String getFormattedNetworkInformation(String type);

}
