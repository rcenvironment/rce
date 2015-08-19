/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.configuration.CommunicationConfiguration;
import de.rcenvironment.core.communication.configuration.CommunicationIPFilterConfiguration;
import de.rcenvironment.core.communication.configuration.IPWhitelistConnectionFilter;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.messaging.RawMessageChannelTrafficListener;
import de.rcenvironment.core.communication.model.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.model.RawNetworkResponseHandler;
import de.rcenvironment.core.communication.model.internal.NodeInformationRegistryImpl;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.internal.WaitForResponseCallable;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallback;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedCallbackManager;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Default implementation of {@link MessageChannelService} which also provides the {@link RawMessageChannelEndpointHandler} interface.
 * 
 * @author Robert Mischke
 */
public class MessageChannelServiceImpl implements MessageChannelService {

    /**
     * String constant to satisfy CheckStyle; alternatively, introduce several String.format() calls.
     */
    private static final String SINGLE_QUOTE = "'";

    private InitialNodeInformation ownNodeInformation;

    private Map<String, NetworkTransportProvider> transportProviders;

    private AsyncOrderedCallbackManager<MessageChannelLifecycleListener> channelListeners;

    private AsyncOrderedCallbackManager<RawMessageChannelTrafficListener> trafficListeners;

    private NodeInformationRegistryImpl nodeInformationRegistry;

    private ThreadPool threadPool = SharedThreadPool.getInstance();

    private NodeConfigurationService configurationService;

    private RawMessageChannelEndpointHandlerImpl rawMessageChannelEndpointHandler;

    private MessageRoutingService messageRoutingService;

    private final BrokenMessageChannelListenerImpl brokenConnectionListener;

    private MessageEndpointHandler messageEndpointHandler;

    private final Map<String, MessageChannel> activeOutgoingChannels;

    private final Map<MessageChannel, MessageChannelHealthState> connectionStates = Collections
        .synchronizedMap(new WeakHashMap<MessageChannel, MessageChannelHealthState>());

    private final Random random = new Random();

    private final AtomicLong healthCheckTaskCounter = new AtomicLong();

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log logger = LogFactory.getLog(getClass());

    private boolean localNodeIsRelay;

    private final IPWhitelistConnectionFilter globalIPWhitelistFilter;

    private String localNodeIdString;

    private long requestTimeoutMsec;

    private volatile boolean shuttingDown = false;

    /**
     * Main implementation of {@link RawMessageChannelEndpointHandler}.
     * 
     * @author Robert Mischke
     */
    private class RawMessageChannelEndpointHandlerImpl implements RawMessageChannelEndpointHandler {

        @Override
        public InitialNodeInformation exchangeNodeInformation(InitialNodeInformation senderNodeInformation) {
            // logger.debug(String.format("Incoming channel from node '%s' (%s); sending identification '%s' (%s)",
            // senderNodeInformation.getLogName(), senderNodeInformation.getNodeIdentifier(),
            // ownNodeInformation.getLogName(),
            // ownNodeInformation.getNodeIdentifier()));
            nodeInformationRegistry.updateFrom(senderNodeInformation);
            return ownNodeInformation;
        }

        @Override
        public void onRemoteInitiatedChannelEstablished(MessageChannel channel, ServerContactPoint serverContactPoint) {
            if (!channel.getInitiatedByRemote()) {
                throw new IllegalStateException("Consistency error");
            }
            registerNewOutgoingChannel(channel);
            logger.debug(String.format("Remote-initiated channel '%s' established from '%s' to '%s' via local SCP %s",
                channel, ownNodeInformation.getLogDescription(), channel.getRemoteNodeInformation().getLogDescription(),
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
                    logger.debug(String.format("Found matching mirror channel for closing inbound channel %s, closing: %s",
                        idOfInboundChannel, channel));
                    // close the mirror channel in a separate thread to avoid deadlocks if the
                    // caller holds any monitors
                    threadPool.execute(new Runnable() {

                        @Override
                        @TaskDescription("Close mirror channel after inbound channel was closed")
                        public void run() {
                            // only set flag if the local channel is not closing already, i.e. this is not a double indirect closing
                            if (channel.getState() == MessageChannelState.ESTABLISHED) {
                                channel.markAsClosedBecauseMirrorChannelClosed();
                            }
                            closeOutgoingChannel(channel);
                        }
                    }, idOfInboundChannel);
                }
            }
        }

        @Override
        public NetworkResponse onRawRequestReceived(final NetworkRequest request, final NodeIdentifier sourceId) {

            // send "request received" event to listeners
            trafficListeners.enqueueCallback(new AsyncCallback<RawMessageChannelTrafficListener>() {

                @Override
                public void performCallback(RawMessageChannelTrafficListener listener) {
                    listener.onRawRequestReceived(request, sourceId);
                }
            });

            String messageType = request.getMessageType();
            final NetworkResponse response;

            byte[] contentBytes = request.getContentBytes();
            if (contentBytes != null) {
                StatsCounter.count("Message bytes received by type (total)", messageType, contentBytes.length);
            }

            // forward or process?
            NodeIdentifier finalRecipient = request.accessMetaData().getFinalRecipient();
            if (finalRecipient == null || ownNodeInformation.getNodeId().equals(finalRecipient)) {
                // handle locally
                StatsCounter.count("Messages arrived at destination by type", messageType);
                response = messageEndpointHandler.onRequestArrivedAtDestination(request);
            } else {
                if (!localNodeIsRelay) {
                    // non-relays should never need to forward requests; malicious (forged) request?
                    logger.error("Received a network request that would be forwarded, but the local node is not a relay: " + request);
                    // TODO for future security, make sure this response does not differ from a "normal" routing failure - misc_ro
                    return NetworkResponseFactory.generateResponseForNoRouteWhileForwarding(request, ownNodeInformation.getNodeId());
                }
                // forward
                final NetworkRequest forwardingRequest =
                    NetworkRequestFactory.createNetworkRequestForForwarding(request, ownNodeInformation.getNodeId());
                // consistency check: ensure that the request id is maintained on forwarding
                if (!forwardingRequest.getRequestId().equals(request.getRequestId())) {
                    throw new IllegalStateException("Wrong request id on forwarding");
                }
                StatsCounter.count("Messages forwarded by type", messageType);
                response = messageRoutingService.forwardAndAwait(forwardingRequest);
            }

            if (!response.accessMetaData().hasSender()) {
                // logger.debug("Filling in undefined 'sender' for response to message type " +
                // request.getMessageType());
                response.accessMetaData().setSender(ownNodeInformation.getNodeId());
            }

            // send "response generated" event to listeners
            // send "request received" event to listeners
            trafficListeners.enqueueCallback(new AsyncCallback<RawMessageChannelTrafficListener>() {

                @Override
                public void performCallback(RawMessageChannelTrafficListener listener) {
                    listener.onRawResponseGenerated(response, request, sourceId);
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
            new AsyncOrderedCallbackManager<MessageChannelLifecycleListener>(SharedThreadPool.getInstance(),
                AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.trafficListeners =
            new AsyncOrderedCallbackManager<RawMessageChannelTrafficListener>(SharedThreadPool.getInstance(),
                AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);
        this.rawMessageChannelEndpointHandler = new RawMessageChannelEndpointHandlerImpl();
        this.brokenConnectionListener = new BrokenMessageChannelListenerImpl();
        this.activeOutgoingChannels = new HashMap<String, MessageChannel>();
        this.globalIPWhitelistFilter = new IPWhitelistConnectionFilter();
    }

    @Override
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
            @TaskDescription("Connect to remote node (low-level task)")
            public MessageChannel call() throws Exception {
                MessageChannel channel;
                try {
                    channel =
                        transportProvider
                            .connect(ncp, ownNodeInformation, allowDuplex, rawMessageChannelEndpointHandler, brokenConnectionListener);
                } catch (RuntimeException e) {
                    // FIXME add internal event handling of channel failures?
                    logger.error("Failed to connect to " + ncp + " (local node: " + ownNodeInformation.getLogDescription() + ")", e);
                    throw e;
                }
                // on success
                InitialNodeInformation remoteNodeInformation = channel.getRemoteNodeInformation();
                nodeInformationRegistry.updateFrom(remoteNodeInformation);
                logger.debug(String.format("Channel '%s' established from '%s' to '%s' using remote NCP %s", channel,
                    ownNodeInformation.getLogDescription(), remoteNodeInformation.getLogDescription(), ncp));
                return channel;
            }
        };
        return threadPool.submit(connectTask);
    }

    @Override
    public void registerNewOutgoingChannel(final MessageChannel channel) {
        connectionStates.put(channel, new MessageChannelHealthState());
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
    public void sendRequest(final NetworkRequest request,
        final MessageChannel channel, final NetworkResponseHandler outerResponseHandler) {

        // sanity check to produce a useful stacktrace (in case a null channel slips through standard handling)
        if (channel == null) {
            throw new NullPointerException("Null channel passed to sendRequest(); request=" + request);
        }

        RawNetworkResponseHandler responseHandler = new RawNetworkResponseHandler() {

            @Override
            public void onResponseAvailable(NetworkResponse response) {
                outerResponseHandler.onResponseAvailable(response);
            }

            @Override
            public void onChannelBroken(NetworkRequest request, MessageChannel channel) {
                // send a proper response to the caller, instead of causing a timeout
                outerResponseHandler.onResponseAvailable(NetworkResponseFactory.generateResponseForExceptionWhileRouting(request,
                    ownNodeInformation.getNodeIdString(), new ConnectionClosedException("Channel " + channel.getChannelId()
                        + " was broken and has been closed by " + ownNodeInformation.getNodeIdString())));
                handleBrokenChannel(channel);
            }
        };

        // except for specialized unit/integration tests, this is a place where all messaging
        // passes through, so it is a good place to gather statistics - misc_ro
        byte[] contentBytes = request.getContentBytes();
        String messageType = request.getMessageType();
        StatsCounter.count("Messages sent by type", messageType);
        if (contentBytes != null) {
            StatsCounter.count("Message bytes sent by type", messageType, contentBytes.length);
        }

        // check for missing sender information (can be disabled after testing)
        if (request.accessMetaData().getSenderIdString() == null) {
            logger.warn("Sending message of type " + request.getMessageType() + " with empty 'sender' field");
        }

        channel.sendRequest(request, responseHandler, configurationService.getRequestTimeoutMsec());

        trafficListeners.enqueueCallback(new AsyncCallback<RawMessageChannelTrafficListener>() {

            @Override
            public void performCallback(RawMessageChannelTrafficListener listener) {
                listener.onRequestSent(request);
            }
        });
    }

    @Override
    public void sendRequest(NetworkRequest request, String channelId, NetworkResponseHandler responseHandler) {

        // any MessageChannel seen by outside code is guaranteed to have been registered already;
        // it is possible, however, that it has been unregistered in the meantime because it was closed
        MessageChannel channel = getOutgoingChannelById(channelId);
        if (channel != null) {
            sendRequest(request, channel, responseHandler);
        } else {
            logger.warn("No message channel for id " + channelId + "; most likely, it has just been closed and therefore deregistered");
            responseHandler.onResponseAvailable(NetworkResponseFactory.generateResponseForChannelClosedOrBroken(request));
        }
    }

    @Override
    public Future<NetworkResponse> sendRequest(final NetworkRequest request, final MessageChannel channel) {
        WaitForResponseCallable responseCallable = new WaitForResponseCallable(request, requestTimeoutMsec, localNodeIdString);
        // responseCallable.setLogMarker(ownNodeInformation.getWrappedNodeId().getNodeId() +
        // "/sendRequest");
        sendRequest(request, channel, responseCallable);
        return threadPool.submit(responseCallable);
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
    public void addTrafficListener(RawMessageChannelTrafficListener listener) {
        trafficListeners.addListener(listener);
    }

    @Override
    public ServerContactPoint startServer(NetworkContactPoint ncp) throws CommunicationException {
        NetworkTransportProvider transportProvider = getTransportProvider(ncp.getTransportId());
        ServerContactPoint scp = new ServerContactPoint(transportProvider, ncp, rawMessageChannelEndpointHandler, globalIPWhitelistFilter);
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
            outputReceiver.addOutput(String.format("IP filtering is ENABLED; incoming connections are restricted to %d source IPs",
                acceptedIps.size()));
        } else {
            outputReceiver.addOutput(String.format("IP filtering is DISABLED; all incoming connections are accepted"));
        }
    }

    @Override
    public void triggerHealthCheckForAllChannels() {
        synchronized (activeOutgoingChannels) {
            for (final MessageChannel channel : activeOutgoingChannels.values()) {
                String uniqueTaskId =
                    String.format("%s-%s", channel.getChannelId(), Long.toString(healthCheckTaskCounter.incrementAndGet()));
                threadPool.execute(new Runnable() {

                    @Override
                    @TaskDescription("Channel health check")
                    public void run() {
                        // random delay ("jitter") to avoid all connections being checked at once
                        try {
                            try {
                                Thread.sleep(random.nextInt(CommunicationConfiguration.CONNECTION_HEALTH_CHECK_MAX_JITTER_MSEC));
                            } catch (InterruptedException e) {
                                logger.debug("Interrupted while waiting to perform the next connection health check, skipping");
                                return;
                            }
                            // check if the channel was closed or marked as broken in the meantime
                            if (!channel.isReadyToUse()) {
                                logger.debug(String.format("Channel %s is %s; skipping scheduled health check", channel.getChannelId(),
                                    channel.getState()));
                                return;
                            }
                            MessageChannelHealthState connectionState = connectionStates.get(channel);
                            // synchronize on lock object to prevent concurrent checks
                            synchronized (connectionState.healthCheckInProgressLock) {
                                if (verboseLogging) {
                                    logger.debug("Performing health check on " + channel);
                                }
                                boolean checkSuccessful = performConnectionHealthCheck(channel);
                                boolean considerChannelBroken = false;
                                // keep synchronization on state object short
                                synchronized (connectionState) {
                                    if (checkSuccessful) {
                                        // log if this was a recovery
                                        if (connectionState.healthCheckFailureCount > 0) {
                                            logger.info(String.format(
                                                "Channel %s to %s passed its health check after %d previous failures",
                                                channel.getChannelId(),
                                                channel.getRemoteNodeInformation().getNodeId(),
                                                connectionState.healthCheckFailureCount));
                                        }
                                        // reset counter
                                        connectionState.healthCheckFailureCount = 0;
                                    } else {
                                        // increase counter and log
                                        connectionState.healthCheckFailureCount++;
                                        logger.warn(String.format(
                                            "Channel %s to %s failed a health check (%d consecutive failures)",
                                            channel.getChannelId(),
                                            channel.getRemoteNodeInformation().getNodeId(),
                                            connectionState.healthCheckFailureCount));
                                        // limit exceeded? -> consider broken
                                        if (connectionState.healthCheckFailuresAtOrAboveLimit()) {
                                            considerChannelBroken = true;
                                        }
                                    }
                                }
                                if (considerChannelBroken) {
                                    threadPool.execute(new Runnable() {

                                        @Override
                                        @TaskDescription("Close broken channel after health check failure")
                                        public void run() {
                                            handleBrokenChannel(channel);
                                        }
                                    });
                                }
                            }
                        } catch (InterruptedException e) {
                            logger.debug("Interruption during channel health check", e);
                        }
                    }
                }, uniqueTaskId);
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
    public void setMessageEndpointHandler(MessageEndpointHandler newService) {
        this.messageEndpointHandler = newService;
    }

    @Override
    public void setForwardingService(MessageRoutingService newService) {
        this.messageRoutingService = newService;
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
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        ownNodeInformation = configurationService.getInitialNodeInformation();
        localNodeIdString = ownNodeInformation.getNodeId().getIdString();
        localNodeIsRelay = configurationService.isRelay();
        requestTimeoutMsec = configurationService.getRequestTimeoutMsec();
        nodeInformationRegistry = NodeInformationRegistryImpl.getInstance();
        synchronized (transportProviders) {
            int numTransports = transportProviders.size();
            logger.debug(String.format(
                "Activated network channel service; instance log name='%s'; node id='%s'; %d registered transport providers",
                ownNodeInformation.getLogDescription(), ownNodeInformation.getNodeId(), numTransports));
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
     * Performs a single request/response attempt with a random token. The receiver should reply with the same token as the response
     * content.
     * 
     * @param channel the channel to test
     * @return true if the check was successful
     * @throws InterruptedException on thread interruption
     */
    private boolean performConnectionHealthCheck(final MessageChannel channel) throws InterruptedException {
        String randomToken = Integer.toString(random.nextInt());
        byte[] contentBytes = MessageUtils.serializeSafeObject(randomToken);
        NetworkRequest request =
            NetworkRequestFactory.createNetworkRequest(contentBytes, ProtocolConstants.VALUE_MESSAGE_TYPE_HEALTH_CHECK,
                ownNodeInformation.getNodeId(), null);
        Future<NetworkResponse> future = sendRequest(request, channel);
        try {
            NetworkResponse response = future.get(CommunicationConfiguration.CONNECTION_HEALTH_CHECK_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
            if (!response.isSuccess() && response.getResultCode() != ProtocolConstants.ResultCode.CHANNEL_CLOSED) {
                logger.warn("Unexpected result: Received non-sucess response on channel health check for '" + channel + SINGLE_QUOTE);
                return false;
            }
            if (verboseLogging) {
                logger.debug("Health check on channel " + channel + " passed");
            }
            // verify that the response contained the same token; this check *should* never fail
            if (!randomToken.equals(response.getDeserializedContent())) {
                logger.warn("Received unexpected content on channel health check: " + response.getDeserializedContent());
            }
            return true;
        } catch (ExecutionException e) {
            logger.debug("Exception during channel health check for channel " + channel.getChannelId(), e);
        } catch (SerializationException e) {
            logger.debug("Exception during channel health check for channel " + channel.getChannelId(), e);
        } catch (TimeoutException e) {
            logger.debug("Timeout during channel health check for channel " + channel.getChannelId());
        }
        return false;
    }

    private void handleBrokenChannel(MessageChannel channel) {
        String remoteNodeText = "(no node information available)";
        // guard against the case when the handshake has not completed yet
        if (channel.getRemoteNodeInformation() != null) {
            remoteNodeText = channel.getRemoteNodeInformation().getNodeId().toString();
        }
        logger.warn("Closing broken channel to " + remoteNodeText + " (id=" + channel.getChannelId() + ")");
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
                logger.warn(String.format("Unexpected state: Expected to find same registered channel object for "
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
