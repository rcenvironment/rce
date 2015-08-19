/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import java.util.Set;
import java.util.concurrent.Future;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.messaging.MessageEndpointHandler;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.messaging.RawMessageChannelTrafficListener;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkResponseHandler;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A service that manages the creation of abstract network connections.
 * 
 * TODO extend documentation after API is settled
 * 
 * @author Robert Mischke
 */
public interface MessageChannelService {

    /**
     * Initiates an asynchronous connection attempt. On success, the new channel is returned, but is not registered internally, so it has no
     * effect on the current network topology. To do this, call {@link #registerNewOutgoingChannel(MessageChannel)} afterwards.
     * 
     * TODO extend documentation after API is settled
     * 
     * @param ncp the {@link NetworkContactPoint} describing the "physical" destination to connect to, and the transport to use
     * @param allowDuplex for transports that support bidirectional message flow over a single established connection, this flag determines
     *        whether a message path (represented as a {@link MessageChannel}) from the destination back to the local node should be
     *        created; if the transport does not support this, the flag has no effect
     * @return a {@link Future} that can be used to wait for the end of the connection attempt, and to retrieve the created
     *         {@link MessageChannel}; TODO specify Future behaviour on connection failure
     * @throws CommunicationException if the {@link NetworkContactPoint} specifies an invalid or unknown transport
     */
    Future<MessageChannel> connect(NetworkContactPoint ncp, boolean allowDuplex) throws CommunicationException;

    /**
     * Registers an established channel as part of the current network topology.
     * 
     * @param channel the new channel to add
     */
    void registerNewOutgoingChannel(MessageChannel channel);

    /**
     * Closes the given channel. Note that channels should always be called via this method; when calling the close() method of a
     * {@link MessageChannel} directly, the proper callbacks will not be triggered.
     * 
     * (TODO allow both ways?)
     * 
     * @param channel the channel to close
     */
    void closeOutgoingChannel(MessageChannel channel);

    /**
     * @return all active outgoing {@link MessageChannel}s; closed or broken channels are not returned
     */
    Set<MessageChannel> getAllOutgoingChannels();

    /**
     * @param id the id of the {@link MessageChannel} to fetch
     * @return the {@link MessageChannel} with the given id, on null if no such channel exists
     */
    MessageChannel getOutgoingChannelById(String id);

    /**
     * Closes all established (logical) {@link MessageChannel}s. Usually called on shutdown.
     */
    void closeAllOutgoingChannels();

    /**
     * Sends the given request into the given connection. The response is returned via the provided {@link NetworkResponseHandler}.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channel the {@link MessageChannel} to send into
     * @param responseHandler the response handler
     */
    void sendRequest(NetworkRequest request, MessageChannel channel, NetworkResponseHandler responseHandler);

    /**
     * Sends the given request into the {@link MessageChannel} identified by the given id. The response is returned via the provided
     * {@link NetworkResponseHandler}.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param channelId the id of the {@link MessageChannel} to send into
     * @param responseHandler the response handler
     */
    void sendRequest(NetworkRequest request, String channelId, NetworkResponseHandler responseHandler);

    /**
     * Convenience method that sends a request containing the given payload and metadata into the given connection, and returns a
     * {@link Future} for the received response.
     * 
     * @param request the {@link NetworkRequest} to send
     * @param connection the connection to send to
     * @return a {@link Future} providing the associated {@link NetworkResponse}
     */
    Future<NetworkResponse> sendRequest(NetworkRequest request, MessageChannel connection);

    /**
     * Adds a new request handler to the processing chain. The handlers are invoked in the order they were added, and the first non-null
     * response generated by a handler is used. Once a non-null response was generated, no further handlers are invoked for the same
     * request.
     * 
     * @param messageType the message type the given handler should be used for; see {@link ProtocolConstants}
     * @param handler the handler to add
     */
    @Deprecated
    // TODO not the proper place to do this; move to management service? - misc_ro
    void registerRequestHandler(String messageType, NetworkRequestHandler handler);

    /**
     * Registers an asynchronous listener for {@link MessageChannel} events.
     * 
     * @param listener the listener to add
     */
    void addChannelLifecycleListener(MessageChannelLifecycleListener listener);

    /**
     * Unregisters an asynchronous listener for {@link MessageChannel} events.
     * 
     * @param listener the listener to remove
     */
    void removeChannelLifecycleListener(MessageChannelLifecycleListener listener);

    /**
     * Starts a local server at the given {@link NetworkContactPoint}. The type of server is determined by the transport id of the contact
     * point.
     * 
     * The host part of the {@link NetworkContactPoint} is taken as the bind address for the service; therefore, the
     * {@link NetworkContactPoint} used for this method may be different from the one that is announced for network peers to connect to. For
     * example, a service may bind to "0.0.0.0", but may use an externally-visible IP or host name for its public contact point.
     * 
     * To shut down the server, use {@link ServerContactPoint#shutDown()}. The reference to the transport that created this
     * {@link ServerContactPoint} is stored internally to avoid the case where it goes away on the OSGi service level before network
     * shutdown occurs.
     * 
     * @param ncp the {@link NetworkContactPoint} used to identify the type of transport, the bind address, and the network port for the
     *        server; if it matches an already-running server, an {@link IllegalStateException} is thrown
     * @return the {@link ServerContactPoint} handle for the started server
     * @throws CommunicationException on startup failure
     */
    ServerContactPoint startServer(NetworkContactPoint ncp) throws CommunicationException;

    /**
     * Checks the health status (i.e., the liveliness) of the outgoing connection that has been idle/unchecked for the longest time. If the
     * connection fails the test, it is closed.
     */
    void triggerHealthCheckForAllChannels();

    /**
     * Sets the {@link MessageEndpointHandler} implementation to use for handling requests that have arrived at their destination.
     * 
     * @param messageEndpointHandler the new handler
     */
    void setMessageEndpointHandler(MessageEndpointHandler messageEndpointHandler);

    /**
     * Sets the {@link MessageRoutingService} to use for forwarding requests that have not reached their destination yet.
     * 
     * @param messageRoutingService the new service
     */
    void setForwardingService(MessageRoutingService messageRoutingService);

    /**
     * Registers a new {@link NetworkTransportProvider}. In a running application, this is called via OSGi-DS; unit tests may call this
     * method directly.
     * 
     * Adding more than one provider for a given transport id is considered an error and results in an {@link IllegalStateException}.
     * 
     * @param provider the transport provider to add
     */
    void addNetworkTransportProvider(NetworkTransportProvider provider);

    /**
     * Adds a {@link RawMessageChannelTrafficListener}.
     * 
     * @param listener the new listener
     */
    void addTrafficListener(RawMessageChannelTrafficListener listener);

    /**
     * Loads the IP filter configuration file, and applies its settings to all current and future {@link ServerContactPoint}s.
     */
    void loadAndApplyIPFilterConfiguration();

    /**
     * Prints end-user information about the current IP filter status.
     * 
     * @param outputReceiver the {@link TextOutputReceiver} to send the output to
     */
    void printIPFilterInformation(TextOutputReceiver outputReceiver);

    /**
     * Sets a signal whether communication layer is currently shutting down, so no new connection attempts are made.
     * 
     * @param shuttingDown true if new connection attempts should be suppressed as shutdown is in progress
     */
    void setShutdownFlag(boolean shuttingDown);

}
