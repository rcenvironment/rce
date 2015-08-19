/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceProxyFactory;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.incubator.ListenerDeclaration;
import de.rcenvironment.core.utils.incubator.ListenerProvider;

/**
 * Implementation of the {@link CommunicationService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CommunicationServiceImpl implements CommunicationService, ListenerProvider {

    private static final String SERVICE_NOT_AVAILABLE_ERROR = "The requested service is not available: ";

    private Set<NodeIdentifier> cachedReachableNodes;

    private ServiceProxyFactory remoteServiceHandler;

    private PlatformService platformService;

    private CommunicationManagementService newManagementService;

    private NetworkRoutingService routingService;

    private final Log log = LogFactory.getLog(getClass());

    private NodeIdentifier localNodeId;

    /**
     * OSGi-DS lifecycle method; also called by integration tests.
     */
    public void activate() {
        localNodeId = platformService.getLocalNodeId();

        updateOnReachableNetworkChanged(routingService.getReachableNetworkGraph());

        // TODO old code; rework
        // RemoteServiceCallServiceImpl.bindNetworkRoutingService(routingService);
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void deactivate() {
        // TODO for now, triggered from here; move to management service?
        newManagementService.shutDownNetwork();
    }

    @Override
    public Collection<ListenerDeclaration> defineListeners() {
        List<ListenerDeclaration> result = new ArrayList<ListenerDeclaration>();
        result.add(new ListenerDeclaration(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

            @Override
            public void onReachableNodesChanged(Set<NodeIdentifier> reachableNodes, Set<NodeIdentifier> addedNodes,
                Set<NodeIdentifier> removedNodes) {
                for (NodeIdentifier node : addedNodes) {
                    log.debug("Topology change: Node " + node + " is now reachable (local node: " + localNodeId + ")");
                }
                for (NodeIdentifier node : removedNodes) {
                    log.debug("Topology change: Node " + node + " is not reachable anymore (local node: " + localNodeId + ")");
                }
            }

            @Override
            public void onReachableNetworkChanged(NetworkGraph networkGraph) {
                CommunicationServiceImpl.this.updateOnReachableNetworkChanged(networkGraph);
            }
        }));
        return result;
    }

    protected synchronized void updateOnReachableNetworkChanged(NetworkGraph networkGraph) {
        cachedReachableNodes = Collections.unmodifiableSet(new HashSet<NodeIdentifier>(networkGraph.getNodeIds()));
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindServiceProxyFactory(ServiceProxyFactory newInstance) {
        remoteServiceHandler = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindPlatformService(PlatformService newInstance) {
        platformService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindCommunicationManagementService(CommunicationManagementService newInstance) {
        this.newManagementService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindNetworkRoutingService(NetworkRoutingService newInstance) {
        this.routingService = newInstance;
    }

    @Override
    public synchronized Set<NodeIdentifier> getReachableNodes() {
        return cachedReachableNodes;
    }

    @Override
    // TODO apply generics -- misc_ro
    public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext) {
        return getService(iface, null, nodeId, bundleContext);
    }

    @Override
    // TODO apply generics -- misc_ro
    public Object getService(Class<?> iface, Map<String, String> properties, NodeIdentifier nodeId,
        BundleContext bundleContext) {

        StatsCounter.count("CommunicationService.getService()", iface.getName());

        if (nodeId == null || platformService.isLocalNode(nodeId)) {
            return getLocalService(iface, properties, bundleContext);
        } else {
            return remoteServiceHandler.createServiceProxy(nodeId, iface, null, properties);
        }
    }

    @Override
    @Deprecated
    public void addRuntimeNetworkPeer(String contactPointDefinition) throws CommunicationException {
        throw new UnsupportedOperationException("deprecated method");
        // newManagementService.connectToRuntimePeer(NetworkContactPointUtils.parseStringRepresentation(contactPointDefinition));
    }

    protected Object getLocalService(Class<?> iface, Map<String, String> properties, BundleContext bundleContext) {

        ServiceReference<?> serviceReference;

        if (properties != null && properties.size() > 0) {
            try {
                ServiceReference<?>[] serviceReferences = bundleContext.getServiceReferences(iface.getName(),
                    ServiceUtils.constructFilter(properties));
                if (serviceReferences != null) {
                    serviceReference = serviceReferences[0];
                } else {
                    throw new IllegalStateException(SERVICE_NOT_AVAILABLE_ERROR + iface.getName());
                }
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalStateException(SERVICE_NOT_AVAILABLE_ERROR + iface.getName());
            }
        } else {
            serviceReference = bundleContext.getServiceReference(iface.getName());
        }

        if (serviceReference != null) {
            Object service = bundleContext.getService(serviceReference);
            if (service != null) {
                return service;
            } else {
                throw new IllegalStateException(SERVICE_NOT_AVAILABLE_ERROR + iface.getName());
            }
        } else {
            throw new IllegalStateException(SERVICE_NOT_AVAILABLE_ERROR + iface.getName());
        }
    }

    @Override
    public String getFormattedNetworkInformation(String type) {
        return routingService.getFormattedNetworkInformation(type);
    }

}
