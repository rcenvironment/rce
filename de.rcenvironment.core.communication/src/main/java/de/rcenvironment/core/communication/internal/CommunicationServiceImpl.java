/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.api.RemotableReliableRPCStreamService;
import de.rcenvironment.core.communication.api.ServiceCallContextUtils;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.rpc.internal.ReliableRPCStreamService;
import de.rcenvironment.core.communication.rpc.spi.LocalServiceResolver;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextMemento;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * Implementation of the {@link CommunicationService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class CommunicationServiceImpl implements CommunicationService, AdditionalServicesProvider {

    private static final String SERVICE_NOT_AVAILABLE_ERROR = "The requested service is not available: ";

    private Set<InstanceNodeSessionId> cachedReachableNodes;

    private Set<LogicalNodeId> cachedReachableLogicalNodes;

    private ServiceProxyFactory remoteServiceHandler;

    private PlatformService platformService;

    private CommunicationManagementService newManagementService;

    private NetworkRoutingService routingService;

    private ReliableRPCStreamService reliableRPCStreamService;

    private LocalServiceResolver localServiceResolver;

    private InstanceNodeSessionId localInstanceNodeSessionId;

    private LogicalNodeSessionId localDefaultLogicalNodeSessionId;

    private LiveNetworkIdResolutionServiceImpl idResolutionService;

    // NOTE: used in several locations
    private final boolean forceLocalRPCSerialization = System
        .getProperty(NodeConfigurationService.SYSTEM_PROPERTY_FORCE_LOCAL_RPC_SERIALIZATION) != null;

    private final CounterCategory serviceRequestCounter;

    @SuppressWarnings("unused")
    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    public CommunicationServiceImpl() {
        // not injecting this via OSGi-DS as this service is planned to move to the toolkit layer anyway - misc_ro
        final StatisticsTrackerService statisticsService =
            StaticToolkitHolder.getServiceWithUnitTestFallback(StatisticsTrackerService.class);
        serviceRequestCounter =
            statisticsService.getCounterCategory("Remote services: service proxies fetched via getRemotableService()",
                StatisticsFilterLevel.DEVELOPMENT);
    }

    /**
     * OSGi-DS lifecycle method; also called by integration tests.
     */
    @Activate
    public void activate() {
        this.localInstanceNodeSessionId = platformService.getLocalInstanceNodeSessionId();
        this.localDefaultLogicalNodeSessionId = platformService.getLocalDefaultLogicalNodeSessionId();

        idResolutionService.registerLocalInstanceNodeSessionId(localInstanceNodeSessionId);

        updateOnReachableNetworkChanged(routingService.getReachableNetworkGraph());

        // TODO old code; rework
        // RemoteServiceCallServiceImpl.bindNetworkRoutingService(routingService);
    }

    /**
     * OSGi-DS lifecycle method.
     */
    @Deactivate
    public void deactivate() {
        // TODO for now, triggered from here; move to management service?
        newManagementService.shutDownNetwork();
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

            @Override
            public void onReachableNodesChanged(Set<InstanceNodeSessionId> reachableNodes, Set<InstanceNodeSessionId> addedNodes,
                Set<InstanceNodeSessionId> removedNodes) {
                for (InstanceNodeSessionId node : removedNodes) {
                    log.debug(
                        "Topology change: Node " + node + " is not reachable anymore (local node: " + localInstanceNodeSessionId + ")");
                    idResolutionService.unregisterInstanceNodeSessionId(node);
                }
                for (InstanceNodeSessionId node : addedNodes) {
                    log.debug("Topology change: Node " + node + " is now reachable (local node: " + localInstanceNodeSessionId + ")");
                    idResolutionService.registerInstanceNodeSessionId(node);
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
        cachedReachableNodes = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>(networkGraph.getNodeIds()));
        // FIXME >8.0.0 preliminary - this only supports the *default* logical node ids, not the ones published via other mechanisms;
        // however, as we decided against active LNId publishing in 8.0.0, there is nothing to fix right now
        final Set<LogicalNodeId> tempSet = new HashSet<LogicalNodeId>();
        for (InstanceNodeSessionId instanceSessionId : cachedReachableNodes) {
            tempSet.add(instanceSessionId.convertToDefaultLogicalNodeId());
        }
        cachedReachableLogicalNodes = Collections.unmodifiableSet(tempSet);
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindServiceProxyFactory(ServiceProxyFactory newInstance) {
        remoteServiceHandler = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindLocalServiceResolver(LocalServiceResolver newInstance) {
        localServiceResolver = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindPlatformService(PlatformService newInstance) {
        platformService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindLiveNetworkIdResolutionService(LiveNetworkIdResolutionService newInstance) {
        // cast required as this service uses non-interface methods of specific implementation
        this.idResolutionService = (LiveNetworkIdResolutionServiceImpl) newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindCommunicationManagementService(CommunicationManagementService newInstance) {
        this.newManagementService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindNetworkRoutingService(NetworkRoutingService newInstance) {
        this.routingService = newInstance;
    }

    /**
     * Sets the {@link ReliableRPCStreamService} implementation to use; called by OSGi-DS and unit tests.
     * 
     * @param newInstance the service implementation
     */
    @Reference
    public void bindReliableRPCStreamService(ReliableRPCStreamService newInstance) {
        this.reliableRPCStreamService = newInstance;
    }

    @Override
    public synchronized Set<InstanceNodeSessionId> getReachableInstanceNodes() {
        return cachedReachableNodes;
    }

    @Override
    public synchronized Set<LogicalNodeId> getReachableLogicalNodes() {
        return cachedReachableLogicalNodes;
    }

    @Override
    public <T> T getRemotableService(Class<T> iface, NetworkDestination destination) {
        if (destination == null) {
            throw new IllegalArgumentException("The 'destination' argument can not be null");
        }
        if (!iface.isAnnotationPresent(RemotableService.class)) {
            throw new IllegalArgumentException("The requested interface is not a " + RemotableService.class.getSimpleName() + ": "
                + iface.getName());
        }

        serviceRequestCounter.count(iface.getName()); // not using countClass() as it would always register "java.lang.Class" here

        // TODO once the annotation check is passed, simply delegate (?)

        if (destination instanceof ResolvableNodeId) {
            ResolvableNodeId nodeId = (ResolvableNodeId) destination;
            return getServiceProxy(iface, nodeId, null); // null = no Reliable RPC Stream
        } else if (destination instanceof ReliableRPCStreamHandle) {
            ReliableRPCStreamHandle reliableRPCStream = (ReliableRPCStreamHandle) destination;
            return getServiceProxy(iface, reliableRPCStream.getDestinationNodeId(), reliableRPCStream);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public ReliableRPCStreamHandle createReliableRPCStream(ResolvableNodeId targetNodeId) throws RemoteOperationException {
        // TODO log if attempted for local remote node, as it will be ignored
        LogicalNodeSessionId resolvedTargetNodeId;
        try {
            resolvedTargetNodeId = idResolutionService.resolveToLogicalNodeSessionId(targetNodeId);
        } catch (IdentifierException e) {
            throw new RemoteOperationException("Failed to resolve node id " + targetNodeId + " to a reachable instance: " + e.toString());
        }
        String streamId = getRemotableService(RemotableReliableRPCStreamService.class, resolvedTargetNodeId).createReliableRPCStream();
        return reliableRPCStreamService.createLocalSetupForRemoteStreamId(resolvedTargetNodeId, streamId);
    }

    @Override
    public void closeReliableRPCStream(ReliableRPCStreamHandle streamHandle) throws RemoteOperationException {
        getRemotableService(RemotableReliableRPCStreamService.class, streamHandle.getDestinationNodeId())
            .disposeReliableRPCStream(streamHandle.getStreamId());
    }

    private <T> T getServiceProxy(Class<T> iface, ResolvableNodeId nodeId, ReliableRPCStreamHandle reliableRPCStreamHandle) {
        Objects.requireNonNull(nodeId); // sanity check
        if (platformService.matchesLocalInstance(nodeId)) {
            if (forceLocalRPCSerialization) {
                log.debug("Creating service proxy for local service as the 'force RPC serialization' flag is set: " + iface.getName());
                // note: the "reliable RPC" stream id is still ignored if id serialization is forced
                // note2: do not resolve the id yet; this is done at invocation time
                return createSerializingServiceProxy(iface, nodeId, reliableRPCStreamHandle);
            } else {
                // note: the "reliable RPC" stream id is ignored for local calls; these are always considered reliable
                final T localService = resolveLocalService(iface);
                if (localService == null) {
                    throw new IllegalStateException("Unexpected state: There is no local instance of service " + iface.getName());
                }
                LogicalNodeSessionId targetLogicalNodeInstanceId;
                try {
                    targetLogicalNodeInstanceId = idResolutionService.resolveToLogicalNodeSessionId(nodeId);
                } catch (IdentifierException e) {
                    // should never happen
                    throw new RuntimeException("Internal error: resolution of instance-local node id failed", e);
                }
                return createDirectCallServiceProxy(localDefaultLogicalNodeSessionId, targetLogicalNodeInstanceId,
                    iface, localService);
            }
        } else {
            // note: do not resolve the id yet; this is done at invocation time
            return createSerializingServiceProxy(iface, nodeId, reliableRPCStreamHandle);
        }
    }

    @Override
    public String getFormattedNetworkInformation(String type) {
        return routingService.getFormattedNetworkInformation(type);
    }

    @SuppressWarnings("unchecked")
    private <T> T createSerializingServiceProxy(Class<T> iface, final ResolvableNodeId targetNodeId,
        ReliableRPCStreamHandle reliableRPCStreamHandle) {
        return (T) remoteServiceHandler.createServiceProxy(targetNodeId, iface, null, reliableRPCStreamHandle);
    }

    @SuppressWarnings("unchecked")
    private <T> T createDirectCallServiceProxy(final LogicalNodeSessionId callerLogicalNodeSessionId,
        final LogicalNodeSessionId targetLogicalNodeSessionId, final Class<T> serviceIface, final T serviceImpl) {
        final InvocationHandler handler = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // wrap service invocation into service call context set/restore
                final ThreadContextMemento previousThreadContext =
                    ServiceCallContextUtils.attachServiceCallDataToThreadContext(callerLogicalNodeSessionId,
                        targetLogicalNodeSessionId, serviceIface.getSimpleName(), method.getName());
                try {
                    return method.invoke(serviceImpl, args);
                } finally {
                    previousThreadContext.restore();
                }
            }

        };
        return (T) Proxy.newProxyInstance(serviceIface.getClassLoader(), new Class[] { serviceIface }, handler);
    }

    private <T> T resolveLocalService(Class<? super T> iface) {
        @SuppressWarnings("unchecked") T service = (T) localServiceResolver.getLocalService(iface.getName());
        if (service != null) {
            return service;
        } else {
            throw new IllegalStateException(SERVICE_NOT_AVAILABLE_ERROR + iface.getName());
        }
    }
}
