/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.NodeIdentifierService;
import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.MessageChannelTrafficListener;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierContextHolder;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.IPWhitelistConnectionFilter;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.internal.WaitForResponseBlocker;
import de.rcenvironment.core.communication.transport.spi.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.communication.transport.spi.MessageChannelResponseHandler;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default implementation of {@link MessageChannelService} which also provides the {@link MessageChannelEndpointHandler} interface.
 * 
 * @author Robert Mischke
 */
public class MessageChannelServiceImpl implements MessageChannelService {

    /**
     * String constant to satisfy CheckStyle; alternatively, introduce several StringUtils.format() calls.
     */
    private static final String SINGLE_QUOTE = "'";

    private InitialNodeInformation ownNodeInformation;

    private Map<String, NetworkTransportProvider> transportProviders;

    private AsyncOrderedCallbackManager<MessageChannelLifecycleListener> channelListeners;

    private AsyncOrderedCallbackManager<MessageChannelTrafficListener> trafficListeners;

    private AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private NodeConfigurationService configurationService;

    private NodeIdentifierService nodeIdentifierService;

    // stored here to allow overriding in integration tests
    private String protocolVersion = ProtocolConstants.PROTOCOL_COMPATIBILITY_VERSION;

    private RawMessageChannelEndpointHandlerImpl rawMessageChannelEndpointHandler;

    private MessageRoutingService messageRoutingService;

    private final BrokenMessageChannelListenerImpl brokenConnectionListener;

    private MessageEndpointHandler messageEndpointHandler;

    private final Map<String, MessageChannel> activeOutgoingChannels;

    private final Map<MessageChannel, MessageChannelHealthState> connectionHealthStates = Collections
        .synchronizedMap(new WeakHashMap<MessageChannel, MessageChannelHealthState>());

    private final AtomicLong healthCheckTaskCounter = new AtomicLong();

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log logger = LogFactory.getLog(getClass());

    private boolean localNodeIsRelay;

    private final IPWhitelistConnectionFilter globalIPWhitelistFilter;

    private InstanceNodeSessionId localNodeId;

    private long requestTimeoutMsec;

    private volatile boolean shuttingDown = false;

    /**
     * Main implementation of {@link MessageChannelEndpointHandler}.
     * 
     * @author Robert Mischke
     */
    private class RawMessageChannelEndpointHandlerImpl implements MessageChannelEndpointHandler {

        @Override
        public InitialNodeInformation exchangeNodeInformation(InitialNodeInformation senderNodeInformation) {
            // logger.debug(StringUtils.format("Incoming channel from node '%s' (%s); sending identification '%s' (%s)",
            // senderNodeInformation.getLogName(), senderNodeInformation.getNodeIdentifier(),
            // ownNodeInformation.getLogName(),
            // ownNodeInformation.getNodeIdentifier()));
            mergeRemoteHandshakeInformationIntoGlobalNodeKnowledge(senderNodeInformation);
            return ownNodeInformation;
        }

        @Override
        public void onRemoteInitiatedChannelEstablished(MessageChannel channel, ServerContactPoint serverContactPoint) {
            if (!channel.getInitiatedByRemote()) {
                throw new IllegalStateException("Consistency error");
            }
            registerNewOutgoingChannel(channel);
            logger.debug(StringUtils.format("Remote-initiated channel '%s' established from '%s' to '%s' via local SCP %s", channel,
                ownNodeInformation.getLogDescription(), channel.getRemoteNodeInformation().getLogDescription(),
                serverContactPoint));
        }

        @Override
        public void onInboundChannelClosing(String idOfInboundChannel) {
            if (idOfInboundChannel == null) {
                throw new NullPointerException(idOfInboundChannel);
            }
            logger.debug("Inbound message channel is closing, checking for mirror channels; id=" + idOfInboundChannel);
            for (final MessageChannel channel : getAllOutgoingChannels()) {
                if (idOfInboundChannel.equals(channel.getAssociatedMirrorChannelId())) {
                    logger.debug(StringUtils.format("Found matching mirror channel for closing inbound channel %s, closing: %s",
                        idOfInboundChannel, channel));
                    // close the mirror channel in a separate thread to avoid deadlocks if the
                    // caller holds any monitors
                    threadPool.execute("Communication Layer: Close mirror channel after inbound channel was closed", idOfInboundChannel,
                        () -> {
                            // only set flag if the local channel is not closing already, i.e. this is not a double indirect closing
                            if (channel.getState() == MessageChannelState.ESTABLISHED) {
                                channel.markAsClosedBecauseMirrorChannelClosed();
                            }
                            closeOutgoingChannel(channel);
                        });
                }
            }
        }

        @Override
        public NetworkResponse onRawRequestReceived(final NetworkRequest request, final String sourceIdString) {
            try {
                NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(nodeIdentifierService);
                return onRawRequestReceivedInternal(request, sourceIdString);
            } finally {
                NodeIdentifierContextHolder.setDeserializationServiceForCurrentThread(null);
            }
        }

        private NetworkResponse onRawRequestReceivedInternal(final NetworkRequest request, final String sourceIdString) {

            final InstanceNodeSessionId sourceId;
            try {
                sourceId = nodeIdentifierService.parseInstanceNodeSessionIdString(sourceIdString);
            } catch (IdentifierException e) {
                throw NodeIdentifierUtils.wrapIdentifierException(e);
            }

            // send "request received" event to listeners
            trafficListeners.enqueueCallback(new AsyncCallback<MessageChannelTrafficListener>() {

                @Override
                public void performCallback(MessageChannelTrafficListener listener) {
                    listener.onRequestReceivedFromChannel(request, sourceId);
                }
            });

            String messageType = request.getMessageType();
            final NetworkResponse response;

            byte[] contentBytes = request.getContentBytes();
            if (contentBytes != null) {
                StatsCounter.registerValue("Messaging: Request payload bytes received by type", messageType, contentBytes.length);
            }

            // forward or process?
            String finalRecipientIdString = request.accessMetaData().getFinalRecipientIdString();
            if (finalRecipientIdString == null || ownNodeInformation.getInstanceNodeSessionIdString().equals(finalRecipientIdString)) {
                // handle locally
                StatsCounter.count("Messages arrived at destination by type", messageType);
                response = messageEndpointHandler.onRequestArrivedAtDestination(request);
            } else {
                if (!localNodeIsRelay) {
                    // non-relays should never need to forward requests; malicious (forged) request?
                    logger.error("Received a network request that would be forwarded, but the local node is not a relay: " + request);
                    // TODO for future security, make sure this response does not differ from a "normal" routing failure - misc_ro
                    return NetworkResponseFactory.generateResponseForNoRouteWhileForwarding(request,
                        ownNodeInformation.getInstanceNodeSessionId());
                }
                // forward
                final NetworkRequest forwardingRequest =
                    NetworkRequestFactory.createNetworkRequestForForwarding(request, ownNodeInformation.getInstanceNodeSessionId());
                // consistency check: ensure that the request id is maintained on forwarding
                if (!forwardingRequest.getRequestId().equals(request.getRequestId())) {
                    throw new IllegalStateException("Wrong request id on forwarding");
                }
                // TODO restore TTL/hop count checking here?
                StatsCounter.count("Messages forwarded by type", messageType);
                response = messageRoutingService.forwardAndAwait(forwardingRequest);
            }

            if (!response.accessMetaData().hasSender()) {
                // logger.debug("Filling in undefined 'sender' for response to message type " +
                // request.getMessageType());
                response.accessMetaData().setSender(ownNodeInformation.getInstanceNodeSessionId());
            }

            // send "response generated" event to listeners
            // send "request received" event to listeners
            trafficListeners.enqueueCallback(new AsyncCallback<MessageChannelTrafficListener>() {

                @Override
                public void performCallback(MessageChannelTrafficListener listener) {
                    listener.onResponseSentIntoChannel(response, request, sourceId);
                }
            });

            return response;
        }

    }

    /**
     * Listener interface for unexpected channel failures.
     * 
     * @author Robert Mischke
     */
    private class BrokenMessageChannelListenerImpl implements BrokenMessageChannelListener {

        @Override
        public void onChannelBroken(MessageChannel channel) {
            if (channel.getInitiatedByRemote()) {
                logger.warn("onChannelBroken called for remote-initiated channel " + channel.getChannelId() + "; ignoring");
                return;
            }
            handleBrokenChannel(channel);
        }

    }

    /**
     * Internal status information for a single channel.
     * 
     * @author Robert Mischke
     */
    private static final class MessageChannelHealthState {

        private int healthCheckFailureCount;

        /**
         * Lock object to prevent concurrent active health checks.
         */
        private final Object healthCheckInProgressLock = new Object();

        public boolean healthCheckFailuresAtOrAboveLimit() {
            return healthCheckFailureCount >= CommunicationConfiguration.CONNECTION_HEALTH_CHECK_FAILURE_LIMIT;
        }

    }

    /**
     * Default constructor.
     */
    public MessageChannelServiceImpl() {
        this.transportProviders = new HashMap<String, NetworkTransportProvider>();
        this.channelListeners =
            ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.trafficListeners =
            ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.rawMessageChannelEndpointHandler = new RawMessageChannelEndpointHandlerImpl();
        this.brokenConnectionListener = new BrokenMessageChannelListenerImpl();
        this.activeOutgoingChannels = new HashMap<String, MessageChannel>();
        this.globalIPWhitelistFilter = new IPWhitelistConnectionFilter();
    }

    @Override
    // TODO rework to synchronous call? - misc_ro, Nov 2015
    public Future<MessageChannel> connect(final NetworkContactPoint ncp, final boolean allowDuplex) throws CommunicationException {

        if (shuttingDown) {
            throw new CommunicationException("Ignoring a request to connect to " + ncp
                + " as the network layer is shutting down");
        }

        final NetworkTransportProvider transportProvider = getTransportProvider(ncp.getTransportId());
        if (transportProvider == null) {
            throw new CommunicationException("Unknown transport id: " + ncp.getTransportId());
        }
        Callable<MessageChannel> connectTask = new Callable<MessageChannel>() {

            @Override
            @TaskDescription("Communication Layer: Connect to remote node (low-level task)")
            public MessageChannel call() throws Exception {
                MessageChannel channel;
                try {
                    channel =
                        transportProvider.connect(ncp, ownNodeInformation, protocolVersion, allowDuplex, rawMessageChannelEndpointHandler,
                            brokenConnectionListener);
                } catch (RuntimeException e) {
                    // FIXME add internal event handling of channel failures?
                    logger.error("Failed to connect to " + ncp + " (local node: " + ownNodeInformation.getLogDescription() + ")", e);
                    throw e;
                }
                // on success
                InitialNodeInformation remoteNodeInformation = channel.getRemoteNodeInformation();
                mergeRemoteHandshakeInformationIntoGlobalNodeKnowledge(remoteNodeInformation);
                logger.debug(StringUtils.format("Channel '%s' established from '%s' to '%s' using remote NCP %s", channel,
                    ownNodeInformation.getLogDescription(), remoteNodeInformation.getLogDescription(), ncp));
                return channel;
            }
        };
        return threadPool.submit(connectTask);
    }

    @Override
    public void registerNewOutgoingChannel(final MessageChannel channel) {
        connectionHealthStates.put(channel, new MessageChannelHealthState());
        synchronized (activeOutgoingChannels) {
            activeOutgoingChannels.put(channel.getChannelId(), channel);
        }
        channelListeners.enqueueCallback(new AsyncCallback<MessageChannelLifecycleListener>() {

            @Override
            public void performCallback(MessageChannelLifecycleListener listener) {
                listener.onOutgoingChannelEstablished(channel);
            }
        });
    }

    @Override
    public void closeOutgoingChannel(MessageChannel channel) {
        if (channel.close()) {
            unregisterClosedOrBrokenChannel(channel);
        }
    }

    @Override
    public Set<MessageChannel> getAllOutgoingChannels() {
        Set<MessageChannel> detachedCopy;
        synchronized (activeOutgoingChannels) {
            detachedCopy = new HashSet<MessageChannel>(activeOutgoingChannels.values());
        }
        return detachedCopy;
    }

    @Override
    public MessageChannel getOutgoingChannelById(String id) {
        synchronized (activeOutgoingChannels) {
            return activeOutgoingChannels.get(id);
        }
    }

    @Override
    public void setShutdownFlag(boolean newShuttingDownState) {
        this.shuttingDown = newShuttingDownState;
    }

    @Override
    public void closeAllOutgoingChannels() {
        Set<MessageChannel> tempCopyofSet;
        synchronized (activeOutgoingChannels) {
            // create a copy as the original set will be modified
            tempCopyofSet = new HashSet<MessageChannel>(activeOutgoingChannels.values());
        }
        for (MessageChannel channel : tempCopyofSet) {
            closeOutgoingChannel(channel);
        }
        // verify that all connections have been closed and unregistered
        synchronized (activeOutgoingChannels) {
            for (MessageChannel channel : activeOutgoingChannels.values()) {
                logger.warn("Channel list not empty after closing all outgoing connections: " + channel);
            }
        }
    }

    @Override
    public void sendDirectMessageAsync(final NetworkRequest request,
        final MessageChannel channel, final NetworkResponseHandler outerResponseHandler) {
        // default timeout
        sendDirectMessageAsync(request, channel, outerResponseHandler, configurationService.getRequestTimeoutMsec());
    }

    @Override
    public void sendDirectMessageAsync(final NetworkRequest request,
        final MessageChannel channel, final NetworkResponseHandler outerResponseHandler, int timeoutMsec) {

        // sanity check to produce a useful stacktrace (in case a null channel slips through standard handling)
        if (channel == null) {
            throw new NullPointerException("Null channel passed to sendRequest(); request=" + request);
        }

        // except for specialized unit/integration tests, this is a place where all messaging
        // passes through, so it is a good place to gather statistics - misc_ro
        final byte[] contentBytes = request.getContentBytes();
        final String messageType = request.getMessageType();
        // StatsCounter.count("Messages sent by type", messageType);

        MessageChannelResponseHandler responseHandler = new MessageChannelResponseHandler() {

            @Override
            public void onResponseAvailable(NetworkResponse response) {
                outerResponseHandler.onResponseAvailable(response);
                byte[] contentBytes = response.getContentBytes();
                if (contentBytes != null) {
                    StatsCounter.registerValue("Messaging: Response payload bytes received by type", messageType, contentBytes.length);
                }
            }

            @Override
            public void onChannelBroken(NetworkRequest request, MessageChannel channel) {
                // send a proper response to the caller, instead of causing a timeout
                // note: currently not generating an error id, as there is no useful/helpful information to log here
                outerResponseHandler.onResponseAvailable(NetworkResponseFactory.generateResponseForChannelCloseWhileWaitingForResponse(
                    request, ownNodeInformation.getInstanceNodeSessionId(), null));
                handleBrokenChannel(channel);
            }
        };

        // check for missing sender information (can be disabled after testing)
        if (request.accessMetaData().getSenderIdString() == null) {
            logger.warn("Sending message of type " + request.getMessageType() + " with empty 'sender' field");
        }

        channel.sendRequest(request, responseHandler, timeoutMsec);

        trafficListeners.enqueueCallback(new AsyncCallback<MessageChannelTrafficListener>() {

            @Override
            public void performCallback(MessageChannelTrafficListener listener) {
                listener.onRequestSentIntoChannel(request);
            }
        });

        if (contentBytes != null) {
            StatsCounter.registerValue("Messaging: Request payload bytes sent by type", messageType, contentBytes.length);
        }
    }

    @Override
    public void sendDirectMessageAsync(NetworkRequest request, String channelId, NetworkResponseHandler responseHandler) {

        // any MessageChannel seen by outside code is guaranteed to have been registered already;
        // it is possible, however, that it has been unregistered in the meantime because it was closed
        MessageChannel channel = getOutgoingChannelById(channelId);
        if (channel != null) {
            sendDirectMessageAsync(request, channel, responseHandler);
        } else {
            // no relevant additional information, so don't create an error id
            logger.debug("No message channel for id " + channelId + "; most likely, it has just been closed and therefore deregistered");
            responseHandler.onResponseAvailable(NetworkResponseFactory.generateResponseForCloseOrBrokenChannelDuringRequestDelivery(
                request, localNodeId, null));
        }
    }

    @Override
    public NetworkResponse sendDirectMessageBlocking(final NetworkRequest request, final MessageChannel channel, int timeout) {
        WaitForResponseBlocker responseBlocker = new WaitForResponseBlocker(request, localNodeId);
        // responseBlocker.setLogMarker(ownNodeInformation.getWrappedNodeId().getNodeId() + "/sendRequest");
        sendDirectMessageAsync(request, channel, responseBlocker, timeout);
        return responseBlocker.await(timeout);
    }

    @Override
    public NetworkResponse handleLocalForcedSerializationRPC(NetworkRequest request, InstanceNodeSessionId sourceId) {
        return rawMessageChannelEndpointHandler.onRawRequestReceived(request, sourceId.getInstanceNodeSessionIdString());
    }

    @Override
    public void registerRequestHandler(String messageType, NetworkRequestHandler handler) {
        messageEndpointHandler.registerRequestHandler(messageType, handler);
    }

    @Override
    public synchronized void addChannelLifecycleListener(MessageChannelLifecycleListener listener) {
        synchronized (activeOutgoingChannels) {
            final Set<MessageChannel> detachedCopy = Collections.unmodifiableSet(
                new HashSet<MessageChannel>(activeOutgoingChannels.values()));
            // send initial callback
            channelListeners.addListenerAndEnqueueCallback(listener, new AsyncCallback<MessageChannelLifecycleListener>() {

                @Override
                public void performCallback(MessageChannelLifecycleListener listener) {
                    listener.setInitialMessageChannels(detachedCopy);
                }
            });
        }
    }

    @Override
    public void removeChannelLifecycleListener(MessageChannelLifecycleListener listener) {
        channelListeners.removeListener(listener);
    }

    @Override
    public void addTrafficListener(MessageChannelTrafficListener listener) {
        trafficListeners.addListener(listener);
    }

    @Override
    public ServerContactPoint startServer(NetworkContactPoint ncp) throws CommunicationException {
        NetworkTransportProvider transportProvider = getTransportProvider(ncp.getTransportId());
        ServerContactPoint scp =
            new ServerContactPoint(transportProvider, ncp, protocolVersion, rawMessageChannelEndpointHandler, globalIPWhitelistFilter);
        scp.start();
        return scp;
    }

    @Override
    public void loadAndApplyIPFilterConfiguration() {
        CommunicationIPFilterConfiguration ipFilterConfiguration = configurationService.getIPFilterConfiguration();
        if (ipFilterConfiguration.getEnabled()) {
            globalIPWhitelistFilter.configure(ipFilterConfiguration.getAllowedIPs());
        } else {
            globalIPWhitelistFilter.configure(null);
        }
    }

    @Override
    public void printIPFilterInformation(TextOutputReceiver outputReceiver) {
        Set<String> acceptedIps = globalIPWhitelistFilter.getAcceptedIps();
        if (acceptedIps != null) {
            outputReceiver.addOutput(StringUtils.format("IP filtering is ENABLED; incoming connections are restricted to %d source IPs",
                acceptedIps.size()));
        } else {
            outputReceiver.addOutput(StringUtils.format("IP filtering is DISABLED; all incoming connections are accepted"));
        }
    }

    @Override
    public void triggerHealthCheckForAllChannels() {
        synchronized (activeOutgoingChannels) {
            for (final MessageChannel channel : activeOutgoingChannels.values()) {
                String uniqueTaskId =
                    StringUtils.format("%s-%s", channel.getChannelId(), Long.toString(healthCheckTaskCounter.incrementAndGet()));
                threadPool.execute("Communication Layer: Channel health check", uniqueTaskId, () -> {
                    try {
                        try {
                            // random delay ("jitter") to avoid all connections being checked at once
                            Thread.sleep(ThreadLocalRandom.current().nextInt(
                                CommunicationConfiguration.CONNECTION_HEALTH_CHECK_MAX_JITTER_MSEC));
                        } catch (InterruptedException e1) {
                            logger.debug("Interrupted while waiting to perform the next connection health check, skipping");
                            return;
                        }
                        performHealthCheckAndActOnResult(channel);
                    } catch (InterruptedException e2) {
                        logger.debug("Interruption during channel health check", e2);
                    }
                });
            }
        }
    }

    @Override
    public void addNetworkTransportProvider(NetworkTransportProvider provider) {
        logger.debug("Registering transport provider for id '" + provider.getTransportId() + SINGLE_QUOTE);
        synchronized (transportProviders) {
            String id = provider.getTransportId();
            NetworkTransportProvider previous = transportProviders.put(id, provider);
            if (previous != null) {
                transportProviders.put(id, previous);
                throw new IllegalStateException("Duplicate transport for id '" + id + SINGLE_QUOTE);
            }
        }
    }

    /**
     * Removes a registered {@link NetworkTransportProvider}. Trying to remove a non-existing provider results in an
     * {@link IllegalStateException}.
     * 
     * @param provider the transport provider to remove
     */
    public void removeNetworkTransportProvider(NetworkTransportProvider provider) {
        logger.debug("Unregistering transport provider for id '" + provider.getTransportId() + SINGLE_QUOTE);
        synchronized (transportProviders) {
            // consistency check
            NetworkTransportProvider removed = transportProviders.remove(provider.getTransportId());
            if (removed != provider) {
                throw new IllegalStateException("Transport to remove was not actually registered: " + provider);
            }
        }
    }

    /**
     * FIXME Should be called 'getNetworkTransportProvider'. --krol_ph
     * 
     * @param transportId
     * @return
     */
    private NetworkTransportProvider getTransportProvider(String transportId) {
        synchronized (transportProviders) {
            NetworkTransportProvider provider = transportProviders.get(transportId);
            if (provider == null) {
                throw new IllegalStateException("No transport registered for id " + transportId);
            }
            return provider;
        }
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newService the service to bind
     */
    @Override
    public void setMessageEndpointHandler(MessageEndpointHandler newService) {
        this.messageEndpointHandler = newService;
    }

    @Override
    public void setForwardingService(MessageRoutingService newService) {
        this.messageRoutingService = newService;
    }

    @Override
    public void overrideProtocolVersion(String newVersion) {
        this.protocolVersion = newVersion;
    }

    /**
     * OSGi-DS bind method; public for integration test access.
     * 
     * @param newService the service to bind
     */
    public void bindNodeConfigurationService(NodeConfigurationService newService) {
        // do not allow rebinding for now
        if (this.configurationService != null) {
            throw new IllegalStateException();
        }
        this.configurationService = newService;
        this.nodeIdentifierService = configurationService.getNodeIdentifierService();
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        ownNodeInformation = configurationService.getInitialNodeInformation();
        localNodeId = ownNodeInformation.getInstanceNodeSessionId();
        localNodeIsRelay = configurationService.isRelay();
        requestTimeoutMsec = configurationService.getRequestTimeoutMsec();
        synchronized (transportProviders) {
            int numTransports = transportProviders.size();
            logger.debug(StringUtils.format(
                "Activated network channel service; instance log name='%s'; node id='%s'; %d registered transport providers",
                ownNodeInformation.getLogDescription(), ownNodeInformation.getInstanceNodeSessionId(), numTransports));
        }
        loadAndApplyIPFilterConfiguration();
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void deactivate() {
        logger.debug("Deactivating");
    }

    /**
     * Field access for unit tests.
     * 
     * @param nodeInformation
     */
    protected void setNodeInformation(InitialNodeInformation nodeInformation) {
        ownNodeInformation = nodeInformation;
    }

    /**
     * Field access for unit tests.
     * 
     * @return the {@link NodeIdentifierService} of the message-receiving node
     */
    public NodeIdentifierService getNodeIdentifierService() {
        return nodeIdentifierService;
    }

    /**
     * Updates the associated information for a node from a received or locally-generated {@link InitialNodeInformation} object.
     * 
     * @param remoteNodeInformation the object to update from
     */
    private void mergeRemoteHandshakeInformationIntoGlobalNodeKnowledge(InitialNodeInformation remoteNodeInformation) {
        nodeIdentifierService.associateDisplayName(remoteNodeInformation.getInstanceNodeSessionId(),
            remoteNodeInformation.getDisplayName());
    }

    private void performHealthCheckAndActOnResult(final MessageChannel channel) throws InterruptedException {

        final MessageChannelHealthState connectionState = connectionHealthStates.get(channel);
        if (connectionState == null) {
            logger.error("Internal error: Found no health state object for channel " + channel + "; closing channel");
            triggerAsyncClosingOfBrokenChannel(channel);
            return;
        }

        // synchronize on lock object to prevent concurrent checks
        synchronized (connectionState.healthCheckInProgressLock) {
            // check if the channel was closed or marked as broken in the meantime
            if (!channel.isReadyToUse()) {
                logger.debug(StringUtils.format("Channel %s is %s; skipping scheduled health check",
                    channel.getChannelId(), channel.getState()));
                return;
            }
            if (verboseLogging) {
                logger.debug("Performing health check on " + channel);
            }
            boolean checkSuccessful = performConnectionHealthCheckRequestResponse(channel);
            boolean considerChannelBroken = false;
            // keep synchronization on state object short
            synchronized (connectionState) {
                if (checkSuccessful) {
                    // log if this was a recovery
                    if (connectionState.healthCheckFailureCount > 0) {
                        logger.debug(StringUtils.format(
                            "Channel %s to %s passed its health check after %d previous failures",
                            channel.getChannelId(),
                            channel.getRemoteNodeInformation().getInstanceNodeSessionId(),
                            connectionState.healthCheckFailureCount));
                    }
                    // reset counter
                    connectionState.healthCheckFailureCount = 0;
                } else {
                    // increase counter and log
                    connectionState.healthCheckFailureCount++;
                    // TODO try to merge with other log message ("Received no response for a health check ...")
                    logger.debug(StringUtils.format(
                        "Channel %s to %s failed a health check (%d consecutive failures)",
                        channel.getChannelId(),
                        channel.getRemoteNodeInformation().getInstanceNodeSessionId(),
                        connectionState.healthCheckFailureCount));
                    // limit exceeded? -> consider broken
                    if (connectionState.healthCheckFailuresAtOrAboveLimit()) {
                        considerChannelBroken = true;
                    }
                }
            }
            if (considerChannelBroken) {
                triggerAsyncClosingOfBrokenChannel(channel);
            }
        }
    }

    private void triggerAsyncClosingOfBrokenChannel(final MessageChannel channel) {
        threadPool.execute("Communication Layer: Close broken channel after health check failure", () -> handleBrokenChannel(channel));
    }

    /**
     * Performs a single request/response attempt with a random token. The receiver should reply with the same token as the response
     * content.
     * 
     * @param channel the channel to test
     * @return true if the check was successful
     * @throws InterruptedException on thread interruption
     */
    private boolean performConnectionHealthCheckRequestResponse(final MessageChannel channel) throws InterruptedException {
        String randomToken = Integer.toString(ThreadLocalRandom.current().nextInt());
        byte[] contentBytes = MessageUtils.serializeSafeObject(randomToken);
        NetworkRequest request =
            NetworkRequestFactory.createNetworkRequest(contentBytes, ProtocolConstants.VALUE_MESSAGE_TYPE_HEALTH_CHECK,
                ownNodeInformation.getInstanceNodeSessionId(), null);
        NetworkResponse response =
            sendDirectMessageBlocking(request, channel, CommunicationConfiguration.CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC);

        if (response.isSuccess()) {
            try {
                Serializable deserializedContent = response.getDeserializedContent();
                // verify that the response contained the same token; this check *should* never fail
                if (!randomToken.equals(deserializedContent)) {
                    logger.warn(StringUtils.format(
                        "Received successful response for a health check on channel '%s', but it contained unexpected content: %s",
                        channel.getChannelId(), deserializedContent));
                    return false;
                }
            } catch (SerializationException e) {
                logger.warn(StringUtils.format(
                    "Received successful response for a health check on channel '%s', but there was an error deserializing its content",
                    channel.getChannelId()), e);
                return false;
            }
            if (verboseLogging) {
                logger.debug("Health check on channel " + channel + " passed");
            }
            return true;
        } else {
            final int numericResultCode = response.getResultCode().getCode();
            Serializable deserializedContent = null;
            try {
                deserializedContent = response.getDeserializedContent();
            } catch (SerializationException e) {
                logger.warn(
                    StringUtils.format(
                        "Received non-successful response for a health check on channel '%s' (error code %d), "
                            + "and there was also an error deserializing its content",
                        channel.getChannelId(), numericResultCode),
                    e);
            }
            if (response.getResultCode() == ResultCode.TIMEOUT_WAITING_FOR_RESPONSE) {
                logger.warn(StringUtils.format(
                    "Received no response for a health check on message channel '%s' within %,d milliseconds; "
                        + "the connection or the remote instance may be overloaded",
                    channel.getChannelId(), CommunicationConfiguration.CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC));
            } else if (response.getResultCode() == ResultCode.CHANNEL_OR_RESPONSE_LISTENER_SHUT_DOWN_WHILE_WAITING_FOR_RESPONSE) {
                // in this case, the channel should be shutting down anyway, so don't log a health check failure
                logger.debug(StringUtils.format(
                    "Message channel '%s' was closed while waiting for a health check response; "
                        + "not counting as an additional health check failure",
                    channel.getChannelId(), CommunicationConfiguration.CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC));
                return true;
            } else {
                logger.warn(StringUtils.format("Received non-success response for a health check on channel '%s': "
                    + "error code %d, content: %s", channel.getChannelId(), numericResultCode, deserializedContent));
            }
            return false;
        }

    }

    private void handleBrokenChannel(MessageChannel channel) {
        String remoteNodeText = "(no node information available)";
        // guard against the case when the handshake has not completed yet
        if (channel.getRemoteNodeInformation() != null) {
            remoteNodeText = channel.getRemoteNodeInformation().getInstanceNodeSessionId().toString();
        }
        logger.debug("Closing broken channel to " + remoteNodeText + " (id=" + channel.getChannelId() + ")");
        if (channel.markAsBroken()) {
            unregisterClosedOrBrokenChannel(channel);
        }
        // TODO possible optimization: find related requests waiting for response and cancel them
    }

    private void unregisterClosedOrBrokenChannel(final MessageChannel channel) {
        synchronized (activeOutgoingChannels) {
            MessageChannel removed = activeOutgoingChannels.remove(channel.getChannelId());
            // consistency check
            if (removed != channel) {
                logger.warn(StringUtils.format("Unexpected state: Expected to find same registered channel object for "
                    + "closed or broken channel %s, but found '%s' instead", channel.getChannelId(), removed));
            }
        }
        logger.debug("Notifying listeners of shutdown of channel " + channel.getChannelId());
        channelListeners.enqueueCallback(new AsyncCallback<MessageChannelLifecycleListener>() {

            @Override
            public void performCallback(MessageChannelLifecycleListener listener) {
                listener.onOutgoingChannelTerminated(channel);
            }
        });
    }

}
