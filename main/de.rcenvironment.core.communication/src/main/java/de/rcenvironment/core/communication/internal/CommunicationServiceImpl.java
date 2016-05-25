/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;

/**
 * Implementation of the {@link CommunicationService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CommunicationServiceImpl implements CommunicationService, AdditionalServicesProvider {

    private static final String SERVICE_NOT_AVAILABLE_ERROR = "The requested service is not available: ";

    private Set<NodeIdentifier> cachedReachableNodes;

    private ServiceProxyFactory remoteServiceHandler;

    private PlatformService platformService;

    private CommunicationManagementService newManagementService;

    private NetworkRoutingService routingService;

    private NodeIdentifier localNodeId;

    // NOTE: used in several locations
    private final boolean forceLocalRPCSerialization = System
        .getProperty(NodeConfigurationService.SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION) != null;

    private final Log log = LogFactory.getLog(getClass());

    private BundleContext ownBundleContext;

    /**
     * OSGi-DS lifecycle method; also called by integration tests.
     */
    public void activate() {
        this.localNodeId = platformService.getLocalNodeId();

        updateOnReachableNetworkChanged(routingService.getReachableNetworkGraph());

        // TODO old code; rework
        // RemoteServiceCallServiceImpl.bindNetworkRoutingService(routingService);

        // TODO currently fetching the local BundleContext here for migration; rework
        Bundle ownBundle = FrameworkUtil.getBundle(getClass());
        if (ownBundle != null) {
            ownBundleContext = ownBundle.getBundleContext();
        } else {
            ownBundleContext = null; // for integration tests
        }
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void deactivate() {
        // TODO for now, triggered from here; move to management service?
        newManagementService.shutDownNetwork();
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

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
    public <T> T getRemotableService(Class<T> iface, NodeIdentifier nodeId) {
        if (!iface.isAnnotationPresent(RemotableService.class)) {
            throw new IllegalArgumentException("The requested interface is not a " + RemotableService.class.getSimpleName() + ": "
                + iface.getName());
        }

        StatsCounter.count("CommunicationService.getRemotableService()", iface.getName());
        // TODO once the annotation check is passed, simply delegate
        return resolveServiceRequest(iface, nodeId, ownBundleContext);
    }

    @Override
    @Deprecated
    // only used by own test anymore
    public <T> T getService(Class<T> iface, NodeIdentifier nodeId, BundleContext callerBundleContext) {
        StatsCounter.count("CommunicationService.getService()", iface.getName());
        return resolveServiceRequest(iface, nodeId, callerBundleContext);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveServiceRequest(Class<T> iface, NodeIdentifier nodeId, BundleContext callerBundleContext) {
        if (nodeId == null || platformService.isLocalNode(nodeId)) {
            if (forceLocalRPCSerialization) {
                log.debug("Creating service proxy for local service as the 'force RPC serialization' flag is set: " + iface.getName());
                return (T) remoteServiceHandler.createServiceProxy(platformService.getLocalNodeId(), iface, null);
            } else {
                T localService = getLocalService(iface, callerBundleContext);
                if (localService == null) {
                    throw new IllegalStateException("Unexpected state: There is no local instance of service " + iface.getName());
                }
                return localService;
            }
        } else {
            return (T) remoteServiceHandler.createServiceProxy(nodeId, iface, null);
        }
    }

    @Override
    @Deprecated
    public void addRuntimeNetworkPeer(String contactPointDefinition) throws CommunicationException {
        throw new UnsupportedOperationException("deprecated method");
        // newManagementService.connectToRuntimePeer(NetworkContactPointUtils.parseStringRepresentation(contactPointDefinition));
    }

    protected <T> T getLocalService(Class<? super T> iface, BundleContext callerBundleContext) {

        // TODO use LocalServiceResolver for consistency instead
        ServiceReference<?> serviceReference;

        // TODO check for uniqueness (as with remote-accessed services?)
        serviceReference = callerBundleContext.getServiceReference(iface.getName());

        if (serviceReference != null) {
            T service = (T) callerBundleContext.getService(serviceReference);
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
