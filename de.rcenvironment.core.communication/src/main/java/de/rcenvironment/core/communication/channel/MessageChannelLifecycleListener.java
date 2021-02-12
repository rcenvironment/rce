/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import java.util.Set;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Callback interface for connection-related events. The events of this interface are meant to be produced by the <b>connection</b> layer,
 * and consumed/listened to by layers above (mostly, the topology/routing layer).
 * 
 * @author Robert Mischke
 */
public interface MessageChannelLifecycleListener {

    /**
     * Initial callback to synchronize listeners with the current set of {@link MessageChannel}s.
     * 
     * @param currentChannels the current set of {@link MessageChannel}s
     */
    void setInitialMessageChannels(Set<MessageChannel> currentChannels);

    /**
     * Indicates that a new <b>logical</b> connection has been established to a remote node. This may or may not reflect a new underlying
     * network connection. Especially, "passive" logical connections are typically established over an already-existing network connection
     * that was initiated by the remote node before. When these are established, this event is fired on the node that is the <b>receiver</b>
     * of the actual network connection.
     * 
     * This event is meant to be fired <b>after</b> the initial (transport-specific) handshake has been completed.
     * 
     * TODO use "link" or network vs. logical connection terminology for clarity? -- misc_ro
     * 
     * @param connection the established connection
     */
    void onOutgoingChannelEstablished(MessageChannel connection);

    /**
     * Indicates that a connection was closed/terminated. This might reflect either a controlled shutdown, or a connection breakdown.
     * 
     * @param connection the connection that should no longer be considered usable
     */
    // note: add a "cause" parameter if necessary - misc_ro
    void onOutgoingChannelTerminated(MessageChannel connection);

}
