/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import de.rcenvironment.core.communication.channel.MessageChannelState;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;

/**
 * Abstraction of a directed connection between two nodes that supports request-response calls. The realization of the actual wire
 * communication depends on the underlying network transport. Implementations of this class must be reusable and thread-safe.
 * 
 * Note that a {@link MessageChannel} may or may not correspond to a persistent, underlying network connection. As a result, a
 * {@link MessageChannel} may continue to exist after the contacted node has already become unreachable. This kind of situation is usually
 * detected on the next communication attempt, similar to a TCP connection.
 * 
 * @author Robert Mischke
 */
public interface MessageChannel {

    /**
     * Returns a JVM-unique id for this connection. If this id is to be shared across JVMs, it should be joined with another id (for
     * example, the node UUID) to make it globally unique.
     * 
     * @return the unique id
     */
    String getChannelId();

    /**
     * @param id a JVM-unique id for this connection
     */
    void setChannelId(String id);

    /**
     * Signal that this channel has been initialized and is ready to use.
     */
    void markAsEstablished();

    /**
     * @return the {@link MessageChannelState} of this connection
     */
    MessageChannelState getState();

    /**
     * @return general information about the node at the remote end of this connection
     */
    InitialNodeInformation getRemoteNodeInformation();

    /**
     * @param nodeInformation information about the node at the remote end of this connection
     */
    void setRemoteNodeInformation(InitialNodeInformation nodeInformation);

    /**
     * @return true if this connection was initiated (on the network level) by the remote node, i.e. if this is a "passive" connection from
     *         the local node's perspective
     * 
     * @see RawMessageChannelEndpointHandler#onRemoteInitiatedChannelEstablished(MessageChannel, ServerContactPoint)
     */
    boolean getInitiatedByRemote();

    /**
     * @param value true if this connection was initiated (on the network level) by the remote node, i.e. if this is a "passive" connection
     *        from the local node's perspective
     */
    void setInitiatedByRemote(boolean value);

    /**
     * Sends a {@link NetworkRequest} to the remote node via this connection. The response is returned asynchronously.
     * 
     * @param request the request to send
     * @param responseHandler the response callback handler
     * @param timeoutMsec the timeout in milliseconds
     */
    void sendRequest(NetworkRequest request, RawNetworkResponseHandler responseHandler, int timeoutMsec);

    /**
     * Closes this channel. Depending on the transport that created this connection, this may or may not result in an action on the network
     * level.
     * 
     * @return true if this call caused the channel's state to switch to "closed" from a usable state, ie if the caller should unregister
     *         this connection
     */
    boolean close();

    /**
     * Indicates that a critical error has occurred while using this channel, and that it should not be used anymore.
     * 
     * @return true if this call caused the channel's state to switch to "broken" from a usable state, ie if the caller should unregister
     *         this connection
     */
    boolean markAsBroken();

    /**
     * Checks whether this channel is ready to use, which is the case when it is fully initialized, not closed, and not marked as broken.
     * 
     * @return true if the channel is ready to send messages through
     */
    boolean isReadyToUse();

    /**
     * Method for associating a {@link ServerContactPoint} with a connection. Usage is transport-specific.
     * 
     * TODO review: push down to subclasses?
     * 
     * @param networkContactPoint the {@link NetworkContactPoint} to associate
     */
    @Deprecated
    void setAssociatedSCP(ServerContactPoint networkContactPoint);

    /**
     * Gets the id of the "mirrored" channel, i.e. the one that goes into the opposite direction, if available.
     * 
     * @return the id of the "mirroring" channel
     */
    String getAssociatedMirrorChannelId();

    /**
     * Sets the id of the "mirrored" channel, i.e. the one that goes into the opposite direction, if available.
     * 
     * @param mirrorChannelId the id of the "mirroring" channel
     */
    void setAssociatedMirrorChannelId(String mirrorChannelId);

    /**
     * @return whether this channel was closed because its "mirror" channel closed before
     */
    boolean isClosedBecauseMirrorChannelClosed();

    /**
     * Signals that this channel is about to be closed because its "mirror" channel closed.
     */
    void markAsClosedBecauseMirrorChannelClosed();

}
