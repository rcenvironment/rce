/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.internal.HealthCheckNetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.internal.MessageEndpointHandlerImpl;
import de.rcenvironment.core.communication.messaging.internal.RPCNetworkRequestHandler;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.rpc.internal.ReliableRPCStreamService;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.communication.transport.spi.AbstractMessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;

/**
 * Default {@link CommunicationManagementService} implementation.
 * 
 * @author Robert Mischke
 */
@Component
public class CommunicationManagementServiceImpl implements CommunicationManagementService {

    /**
     * The delay between announcing the shutdown to all neighbors, and actually shutting down.
     */
    private static final int DELAY_AFTER_SHUTDOWN_ANNOUNCE_MSEC = 200;

    private MessageChannelService connectionService;

    private NetworkRoutingService networkRoutingService;

    private InitialNodeInformation ownNodeInformation;

    private NodeConfigurationService nodeConfigurationService;

    private List<ServerContactPoint> initializedServerContactPoints = new ArrayList<ServerContactPoint>();

    private ScheduledFuture<?> connectionHealthCheckTaskHandle;

    private RemoteServiceCallHandlerService serviceCallHandler;

    private ReliableRPCStreamService reliableRPCStreamService;

    private NodePropertiesService nodePropertiesService;

    private ConnectionSetupService connectionSetupService;

    private long sessionStartTimeMsec;

    private boolean autoStartNetworkOnActivation = true; // disabled by integration tests

    private boolean started;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public synchronized void startUpNetwork() {

        sessionStartTimeMsec = System.currentTimeMillis();

        // add to local metadata
        Map<String, String> localMetadata = createLocalMetadataContribution();
        nodePropertiesService.addOrUpdateLocalNodeProperties(localMetadata);

        // start server contact points
        log.debug("Starting server contact points");
        for (NetworkContactPoint ncp : nodeConfigurationService.getServerContactPoints()) {
            // log.debug(StringUtils.format("Virtual instance '%s': Starting server at %s",
            // ownNodeInformation.getLogName(), ncp));
            try {
                synchronized (initializedServerContactPoints) {
                    ServerContactPoint newSCP = connectionService.startServer(ncp);
                    initializedServerContactPoints.add(newSCP);
                }
            } catch (CommunicationException e) {
                log.warn("Error while starting server at " + ncp, e);
            }
        }
        // FIXME temporary fix until connection retry (or similar) is implemented;
        // without this, simultaneous startup of instance groups will usually fail,
        // because some instances will try to connect before others have fully started
        try {
            Thread.sleep(nodeConfigurationService.getDelayBeforeStartupConnectAttempts());
        } catch (InterruptedException e1) {
            log.error("Interrupted while waiting during startup; not connecting to neighbors", e1);
            return;
        }

        connectionService.setShutdownFlag(false);

        // trigger connections to initial peers
        log.debug("Starting preconfigured connections");
        for (final NetworkContactPoint ncp : nodeConfigurationService.getInitialNetworkContactPoints()) {
            // TODO add custom display name when available; move string reconstruction into NCP
            final String displayName = StringUtils.format("%s:%s", ncp.getHost(), ncp.getPort());
            boolean connectOnStartup = !"false".equals(ncp.getAttributes().get("connectOnStartup"));

            if (connectionSetupService.connectionAlreadyExists(ncp)) {
                log.debug(StringUtils.format("Redundant preconfigured connection '%s:%s'", ncp.getHost(), ncp.getPort()));
            } else {
                ConnectionSetup setup = connectionSetupService.createConnectionSetup(ncp, displayName,
                    connectOnStartup);
                log.debug(StringUtils.format("Loaded pre-configured network connection \"%s\" (Settings: %s)",
                    setup.getDisplayName(), ncp.getAttributes()));
                if (setup.getConnnectOnStartup()) {
                    setup.signalStartIntent();
                }
            }
        }

        connectionHealthCheckTaskHandle = ConcurrencyUtils.getAsyncTaskService()
            .scheduleAtFixedInterval("Communication Layer: Connection health check (trigger task)", () -> {
                try {
                    connectionService.triggerHealthCheckForAllChannels();
                } catch (RuntimeException e) {
                    log.error("Uncaught exception during connection health check", e);
                }
            }, CommunicationConfiguration.CONNECTION_HEALTH_CHECK_INTERVAL_MSEC);

        started = true;
    }

    @Override
    @Deprecated
    public MessageChannel connectToRuntimePeer(NetworkContactPoint ncp) throws CommunicationException {
        Future<MessageChannel> future = connectionService.connect(ncp, true);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new CommunicationException(e);
        }
    }

    @Override
    @Deprecated
    public void asyncConnectToNetworkPeer(final NetworkContactPoint ncp) {
        ConcurrencyUtils.getAsyncTaskService().execute("Communication Layer: Connect to remote node (trigger task)", () -> {

            try {
                log.debug("Initiating asynchronous connection to " + ncp);
                connectToRuntimePeer(ncp);
            } catch (CommunicationException e) {
                log.warn("Failed to contact initial peer at NCP " + ncp, e);
            }
        });
    }

    @Override
    public synchronized void shutDownNetwork() {

        if (!started) {
            log.debug("Network layer was not started, ignoring request to shut down");
            return;
        }
        started = false;

        connectionService.setShutdownFlag(true);

        connectionHealthCheckTaskHandle.cancel(true);

        // workaround for old tests that assume a network message on shutdown
        // TODO rework to proper solution
        nodePropertiesService.addOrUpdateLocalNodeProperty("state", "shutting down");

        // FIXME dirty hack until the shutdown LSA broadcast waits for a response or timeout itself;
        // without this, the asynchronous sending might not happen before the connections are closed
        // TODO wait for confirmations from all neighbors (with a short timeout) instead?
        try {
            Thread.sleep(DELAY_AFTER_SHUTDOWN_ANNOUNCE_MSEC);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting", e);
        }

        // close outgoing connections
        connectionService.closeAllOutgoingChannels();

        // shut down server contact points
        synchronized (initializedServerContactPoints) {
            for (ServerContactPoint scp : initializedServerContactPoints) {
                // log.debug(StringUtils.format("Virtual instance '%s': Stopping server at %s",
                // ownNodeInformation.getLogName(), ncp));
                scp.shutDown();
            }
            initializedServerContactPoints.clear();
        }

        // fix for issue 0017039: remove all connection setups on shutdown so they don't block new ones from being loaded on restart
        for (ConnectionSetup connectionSetup : connectionSetupService.getAllConnectionSetups()) {
            connectionSetupService.disposeConnectionSetup(connectionSetup);
        }
    }

    @Override
    public void simulateUncleanShutdown() {
        // simulate crash of outgoing channels
        for (MessageChannel channel : connectionService.getAllOutgoingChannels()) {
            ((AbstractMessageChannel) channel).setSimulatingBreakdown(true);
        }
        connectionService.closeAllOutgoingChannels();
        // simulate crash of server contact points
        synchronized (initializedServerContactPoints) {
            for (ServerContactPoint scp : initializedServerContactPoints) {
                scp.setSimulatingBreakdown(true);
            }
            initializedServerContactPoints.clear();
        }

        // fix for issue 0017039: remove all connection setups on shutdown so they don't block new ones from being loaded on restart
        for (ConnectionSetup connectionSetup : connectionSetupService.getAllConnectionSetups()) {
            connectionSetupService.disposeConnectionSetup(connectionSetup);
        }
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newService the service to bind
     */
    @Reference
    public void bindMessageChannelService(MessageChannelService newService) {
        // do not allow rebinding for now
        if (connectionService != null) {
            throw new IllegalStateException();
        }
        connectionService = newService;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newService the service to bind
     */
    @Reference
    public void bindNetworkRoutingService(NetworkRoutingService newService) {
        // do not allow rebinding for now
        if (networkRoutingService != null) {
            throw new IllegalStateException();
        }
        networkRoutingService = newService;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newService the service to bind
     */
    @Reference
    public void bindNodeConfigurationService(NodeConfigurationService newService) {
        // do not allow rebinding for now
        if (this.nodeConfigurationService != null) {
            throw new IllegalStateException();
        }
        this.nodeConfigurationService = newService;
    }

    /**
     * Define the {@link RemoteServiceCallHandlerService} implementation to use for incoming RPC calls; made public for integration testing.
     * 
     * @param newInstance the {@link RemoteServiceCallHandlerService} to use
     */
    @Reference
    public void bindServiceCallHandler(RemoteServiceCallHandlerService newInstance) {
        serviceCallHandler = newInstance;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindNodePropertiesService(NodePropertiesService newInstance) {
        this.nodePropertiesService = newInstance;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindConnectionSetupService(ConnectionSetupService newInstance) {
        this.connectionSetupService = newInstance;
    }

    /**
     * OSGi-DS bind method; public for integration testing.
     *
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindReliableRPCStreamService(ReliableRPCStreamService newInstance) {
        reliableRPCStreamService = newInstance;
    }

    /**
     * OSGi-DS lifecycle method.
     */
    @Activate
    public void activate() {
        ownNodeInformation = nodeConfigurationService.getInitialNodeInformation();

        MessageEndpointHandler messageEndpointHandler = new MessageEndpointHandlerImpl(nodeConfigurationService.getNodeIdentifierService());

        messageEndpointHandler.registerRequestHandler(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC, new RPCNetworkRequestHandler(
            serviceCallHandler, reliableRPCStreamService));
        messageEndpointHandler.registerRequestHandler(ProtocolConstants.VALUE_MESSAGE_TYPE_HEALTH_CHECK,
            new HealthCheckNetworkRequestHandler());

        connectionService.setMessageEndpointHandler(messageEndpointHandler);

        // register LSA protocol handler
        // messageEndpointHandler.registerRequestHandlers(networkRoutingService.getProtocolManager().getNetworkRequestHandlers());

        // register metadata protocol handler
        messageEndpointHandler.registerRequestHandlers(nodePropertiesService.getNetworkRequestHandlers());

        // "autoStartNetworkOnActivation" is true by default; only disabled in test code
        if (autoStartNetworkOnActivation && !CommandLineArguments.isDoNotStartNetworkRequested()) {
            ConcurrencyUtils.getAsyncTaskService().execute("Communication Layer: Main startup", this::startUpNetwork);

        } else {
            log.debug("Network startup is disabled");
        }
    }

    /**
     * OSGi-DS lifecycle method.
     */
    @Deactivate
    public void deactivate() {}

    /**
     * Allows unit or integration tests to prevent {@link #startUpNetwork()} from being called automatically as part of the
     * {@link #activate()} method.
     * 
     * @param autoStartNetworkOnActivation the new value; default is "true"
     */
    public void setAutoStartNetworkOnActivation(boolean autoStartNetworkOnActivation) {
        this.autoStartNetworkOnActivation = autoStartNetworkOnActivation;
    }

    private Map<String, String> createLocalMetadataContribution() {
        Map<String, String> localData = new HashMap<String, String>();
        localData.put(NodePropertyConstants.KEY_NODE_ID, ownNodeInformation.getInstanceNodeSessionIdString());
        localData.put(NodePropertyConstants.KEY_DISPLAY_NAME, ownNodeInformation.getDisplayName());
        localData.put(NodePropertyConstants.KEY_SESSION_START_TIME, Long.toString(sessionStartTimeMsec));

        // TODO @5.0: review: provide options to disable? - misc_ro
        localData.put("debug.sessionStartInfo",
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date(sessionStartTimeMsec))); // temporary
        Version coreVersion = VersionUtils.getVersionOfCoreBundles();
        if (coreVersion != null) {
            localData.put("debug.coreVersion", coreVersion.toString());
        } else {
            localData.put("debug.coreVersion", "<unknown>");
        }
        localData.put("debug.osInfo", StringUtils.format("%s (%s/%s)",
            System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")));
        localData.put("debug.isRelay", Boolean.toString(nodeConfigurationService.isRelay()));
        if (nodeConfigurationService.getLocationCoordinates() != null) {
            localData.put("coordinates",
                "[" + nodeConfigurationService.getLocationCoordinates()[0] + "," + nodeConfigurationService.getLocationCoordinates()[1]
                    + "]");
        }
        localData.put("locationName", nodeConfigurationService.getLocationName());
        localData.put("contact", nodeConfigurationService.getInstanceContact());
        localData.put("additionalInformation", nodeConfigurationService.getInstanceAdditionalInformation());
        return localData;
    }
}
