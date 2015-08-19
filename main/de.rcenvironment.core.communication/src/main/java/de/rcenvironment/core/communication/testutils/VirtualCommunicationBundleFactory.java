/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.connection.impl.ConnectionSetupServiceImpl;
import de.rcenvironment.core.communication.connection.internal.MessageChannelServiceImpl;
import de.rcenvironment.core.communication.internal.CommunicationServiceImpl;
import de.rcenvironment.core.communication.internal.PlatformServiceImpl;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
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
import de.rcenvironment.core.communication.rpc.RemoteServiceCallService;
import de.rcenvironment.core.communication.rpc.ServiceCallHandler;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceProxyFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.internal.CallbackProxyServiceImpl;
import de.rcenvironment.core.communication.rpc.internal.RemoteServiceCallServiceImpl;
import de.rcenvironment.core.communication.rpc.internal.ServiceProxyFactoryImpl;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.incubator.MockListenerRegistrationService;

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

        private MockListenerRegistrationService listenerRegistrationService;

        private final VirtualServiceRegistry serviceRegistry;

        private DistributedLinkStateManager distributedLinkStateManager;

        private boolean autoStartNetworkOnActivation = true;

        private CommunicationManagementServiceImpl communicationManagementService;

        public VirtualCommunicationBundleImpl(NodeConfigurationService nodeConfigurationService) {

            serviceRegistry = new VirtualServiceRegistry();

            listenerRegistrationService = new MockListenerRegistrationService();

            serviceRegistry.registerProvidedService(nodeConfigurationService, NodeConfigurationService.class);

            messageChannelService = new MessageChannelServiceImpl();
            serviceRegistry.registerManagedService(messageChannelService, MessageChannelService.class);

            ConnectionSetupServiceImpl connectionSetupService = new ConnectionSetupServiceImpl();
            serviceRegistry.registerManagedService(connectionSetupService, false, ConnectionSetupService.class);
            listenerRegistrationService.registerListenerProvider(connectionSetupService);

            nodePropertiesService = new NodePropertiesServiceImpl();
            serviceRegistry.registerManagedService(nodePropertiesService, NodePropertiesService.class);

            routingService = new NetworkRoutingServiceImpl();
            // bind for both implemented interface
            serviceRegistry.registerManagedService(routingService, NetworkRoutingService.class, MessageRoutingService.class);
            listenerRegistrationService.registerListenerProvider(routingService);

            // register provided ServiceCallHandler stub service
            serviceRegistry.registerProvidedService(new ServiceCallHandlerStub(), ServiceCallHandler.class);

            // keep reference to prevent automatic network startup before activation
            communicationManagementService = new CommunicationManagementServiceImpl();
            serviceRegistry.registerManagedService(communicationManagementService, CommunicationManagementService.class);

            serviceRegistry.registerManagedService(new PlatformServiceImpl(), PlatformService.class);

            serviceRegistry.registerManagedService(new RemoteServiceCallServiceImpl(), false, RemoteServiceCallService.class);

            // register stubs; replace when RPC should be made testable
            serviceRegistry.registerProvidedService(new CallbackServiceDefaultStub(), CallbackService.class);
            serviceRegistry.registerProvidedService(new CallbackProxyServiceImpl(), CallbackProxyService.class);

            serviceRegistry.registerManagedService(new ServiceProxyFactoryImpl(), false, ServiceProxyFactory.class);

            serviceRegistry.registerManagedService(new WorkflowHostServiceImpl(), WorkflowHostService.class);

            CommunicationServiceImpl communicationService = new CommunicationServiceImpl();
            serviceRegistry.registerManagedService(communicationService, CommunicationService.class);
            listenerRegistrationService.registerListenerProvider(communicationService);

            distributedLinkStateManager = new DistributedLinkStateManager();
            serviceRegistry.registerManagedService(distributedLinkStateManager, true, DistributedLinkStateManager.class);
            listenerRegistrationService.registerListenerProvider(distributedLinkStateManager);
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

    }

    /**
     * {@link ServiceCallHandler} stub that throws an exception on incoming RPC requests.
     * 
     * @author Robert Mischke
     */
    private static final class ServiceCallHandlerStub implements ServiceCallHandler {

        @Override
        public ServiceCallResult handle(ServiceCallRequest serviceCallRequest) throws CommunicationException {
            throw new UnsupportedOperationException("Virtual communication bundles do not support RPC (yet?)");
        }
    }

    private VirtualCommunicationBundleFactory() {}

    /**
     * Creates a {@link VirtualCommunicationBundle} from a node id and a display/log name.
     * 
     * @param nodeId the node id
     * @param name the name of the node
     * @param isRelay whether the "is relay" flag of this node should be set
     * @return the new instance
     */
    public static VirtualCommunicationBundle createFromSettings(String nodeId, String name, boolean isRelay) {
        return new VirtualCommunicationBundleImpl(new NodeConfigurationServiceTestStub(nodeId, name, isRelay));
    }

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
