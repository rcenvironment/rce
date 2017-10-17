/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.connection.impl.ConnectionSetupServiceImpl;
import de.rcenvironment.core.communication.connection.internal.MessageChannelServiceImpl;
import de.rcenvironment.core.communication.internal.CommunicationServiceImpl;
import de.rcenvironment.core.communication.internal.LiveNetworkIdResolutionServiceImpl;
import de.rcenvironment.core.communication.internal.PlatformServiceImpl;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.management.RemoteBenchmarkService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.communication.management.internal.BenchmarkServiceImpl;
import de.rcenvironment.core.communication.management.internal.CommunicationManagementServiceImpl;
import de.rcenvironment.core.communication.management.internal.WorkflowHostServiceImpl;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.internal.NodePropertiesServiceImpl;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.NetworkRoutingServiceImpl;
import de.rcenvironment.core.communication.routing.internal.v2.DistributedLinkStateManager;
import de.rcenvironment.core.communication.routing.internal.v2.LinkStateKnowledgeChangeListener;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.api.RemoteServiceCallSenderService;
import de.rcenvironment.core.communication.rpc.internal.CallbackProxyServiceImpl;
import de.rcenvironment.core.communication.rpc.internal.RemoteServiceCallSenderServiceImpl;
import de.rcenvironment.core.communication.rpc.internal.ServiceCallHandlerServiceImpl;
import de.rcenvironment.core.communication.rpc.internal.ServiceProxyFactoryImpl;
import de.rcenvironment.core.communication.rpc.spi.LocalServiceResolver;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.common.service.MockAdditionalServicesRegistrationService;

/**
 * A factory for {@link VirtualCommunicationBundle} instances intended for integration tests.
 * 
 * @author Robert Mischke
 */
public final class VirtualCommunicationBundleFactory {

    /**
     * {@link VirtualCommunicationBundle} implementation.
     * 
     * @author Robert Mischke
     */
    private static final class VirtualCommunicationBundleImpl implements VirtualCommunicationBundle {

        private MessageChannelServiceImpl messageChannelService;

        private NetworkRoutingServiceImpl routingService;

        private NodePropertiesServiceImpl nodePropertiesService;

        private MockAdditionalServicesRegistrationService listenerRegistrationService;

        private final VirtualServiceRegistry serviceRegistry;

        private DistributedLinkStateManager distributedLinkStateManager;

        private boolean autoStartNetworkOnActivation = true;

        private CommunicationManagementServiceImpl communicationManagementService;

        VirtualCommunicationBundleImpl(NodeConfigurationService nodeConfigurationService) {

            serviceRegistry = new VirtualServiceRegistry();

            listenerRegistrationService = new MockAdditionalServicesRegistrationService();

            serviceRegistry.registerProvidedService(nodeConfigurationService, NodeConfigurationService.class);
            serviceRegistry.registerProvidedService(nodeConfigurationService.getNodeIdentifierService(), NodeIdentifierService.class);

            messageChannelService = new MessageChannelServiceImpl();
            serviceRegistry.registerManagedService(messageChannelService, MessageChannelService.class);

            ConnectionSetupServiceImpl connectionSetupService = new ConnectionSetupServiceImpl();
            serviceRegistry.registerManagedService(connectionSetupService, false, ConnectionSetupService.class);
            listenerRegistrationService.registerAdditionalServicesProvider(connectionSetupService);

            nodePropertiesService = new NodePropertiesServiceImpl();
            serviceRegistry.registerManagedService(nodePropertiesService, NodePropertiesService.class);

            routingService = new NetworkRoutingServiceImpl();
            // bind for both implemented interface
            serviceRegistry.registerManagedService(routingService, NetworkRoutingService.class, MessageRoutingService.class);
            listenerRegistrationService.registerAdditionalServicesProvider(routingService);

            // keep reference to prevent automatic network startup before activation
            communicationManagementService = new CommunicationManagementServiceImpl();
            serviceRegistry.registerManagedService(communicationManagementService, CommunicationManagementService.class);

            serviceRegistry.registerManagedService(new PlatformServiceImpl(), PlatformService.class);

            serviceRegistry.registerManagedService(new LiveNetworkIdResolutionServiceImpl(), false, LiveNetworkIdResolutionService.class);
            
            serviceRegistry.registerManagedService(new RemoteServiceCallSenderServiceImpl(), false, RemoteServiceCallSenderService.class);

            // register stubs; replace when RPC callbacks should be made testable
            serviceRegistry.registerProvidedService(new CallbackServiceDefaultStub(), CallbackService.class);
            serviceRegistry.registerProvidedService(new CallbackProxyServiceImpl(), CallbackProxyService.class);

            serviceRegistry.registerManagedService(new ServiceProxyFactoryImpl(), false, ServiceProxyFactory.class);

            serviceRegistry.registerManagedService(new WorkflowHostServiceImpl(), WorkflowHostService.class);

            CommunicationServiceImpl communicationService = new CommunicationServiceImpl();
            serviceRegistry.registerManagedService(communicationService, CommunicationService.class);
            listenerRegistrationService.registerAdditionalServicesProvider(communicationService);

            distributedLinkStateManager = new DistributedLinkStateManager();
            serviceRegistry.registerManagedService(distributedLinkStateManager, true, DistributedLinkStateManager.class);
            listenerRegistrationService.registerAdditionalServicesProvider(distributedLinkStateManager);

            // register a virtual LocalServiceResolver stub
            serviceRegistry.registerProvidedService(new LocalServiceResolverStub(), LocalServiceResolver.class);
            // register the standard RPC handler (which uses the resolver stub)
            serviceRegistry.registerManagedService(new ServiceCallHandlerServiceImpl(), false, RemoteServiceCallHandlerService.class);

            // add a simple remote service for RPC testing
            serviceRegistry.registerProvidedService(new BenchmarkServiceImpl(), RemoteBenchmarkService.class);
        }

        @Override
        public void registerNetworkTransportProvider(NetworkTransportProvider newProvider) {
            messageChannelService.addNetworkTransportProvider(newProvider);
        }

        @Override
        public void activate() {
            // transfer autostart setting
            communicationManagementService.setAutoStartNetworkOnActivation(autoStartNetworkOnActivation);

            serviceRegistry.bindAndActivateServices();

            // connect dynamic listeners
            for (NetworkTopologyChangeListener listener : listenerRegistrationService.
                getListeners(NetworkTopologyChangeListener.class)) {
                routingService.addNetworkTopologyChangeListener(listener);
            }
            for (RawNodePropertiesChangeListener listener : listenerRegistrationService.
                getListeners(RawNodePropertiesChangeListener.class)) {
                nodePropertiesService.addRawNodePropertiesChangeListener(listener);
            }
            for (MessageChannelLifecycleListener listener : listenerRegistrationService.
                getListeners(MessageChannelLifecycleListener.class)) {
                messageChannelService.addChannelLifecycleListener(listener);
            }
            for (LinkStateKnowledgeChangeListener listener : listenerRegistrationService.
                getListeners(LinkStateKnowledgeChangeListener.class)) {
                distributedLinkStateManager.addLinkStateKnowledgeChangeListener(listener);
            }

            // TODO auto-detect unbound listeners?
        }

        @Override
        public void setAutoStartNetworkOnActivation(boolean autoStartNetworkOnActivation) {
            this.autoStartNetworkOnActivation = autoStartNetworkOnActivation;
        }

        @Override
        public <T> T getService(Class<T> clazz) {
            T implementation = serviceRegistry.getService(clazz);
            if (implementation == null) {
                throw new NullPointerException("No activated service provides the interface " + clazz.getName());
            }
            return implementation;
        }

        @Override
        public <T> void injectService(Class<T> clazz, T implementation) {
            // note: services are not implicitly activated
            serviceRegistry.registerProvidedService(implementation, clazz);
        }

        /**
         * A {@link LocalServiceResolver} implementation that accesses this instance's {@link VirtualServiceRegistry}.
         * 
         * @author Robert Mischke
         */
        private final class LocalServiceResolverStub implements LocalServiceResolver {

            @Override
            public Object getLocalService(String serviceName) {
                try {
                    Object impl = serviceRegistry.getService(Class.forName(serviceName));
                    if (impl == null) {
                        LogFactory.getLog(getClass()).error("No such service available: " + serviceName);
                        return null;
                    }
                    return impl;
                } catch (ClassNotFoundException e) {
                    LogFactory.getLog(getClass()).error("Failed to resolve service class " + serviceName, e);
                    return null;
                }
            }

        }

    }

    private VirtualCommunicationBundleFactory() {}

    /**
     * Creates a {@link VirtualCommunicationBundle} from an existing {@link NodeConfigurationService}.
     * 
     * @param nodeConfigurationService the {@link NodeConfigurationService} to pull configuration value from
     * @return the new instance
     */
    public static VirtualCommunicationBundle createFromNodeConfigurationService(NodeConfigurationService nodeConfigurationService) {
        return new VirtualCommunicationBundleImpl(nodeConfigurationService);
    }

}
