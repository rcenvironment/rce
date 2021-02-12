/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.api;

import java.io.IOException;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;

/**
 * One of the two endpoints of an Uplink "channel", which represents a virtual point-to-point messsage tunnel. Currently, there are three
 * types of channels:
 * <ul>
 * <li>the "default" channel for communication of each client with the relay,
 * <li>execution channels between two clients, with an "initiator" and a "provider" side,
 * <li>and documentation file transfer channels, with the same roles as execution channels.
 * </ul>
 *
 * @author Robert Mischke
 */
public interface ChannelEndpoint {

    /**
     * Processes the provided {@link MessageBlock}. All processing calls are blocking, and typically non-parallel. In turn, implementations
     * are not supposed to block for a long time - e.g. no waiting for a remote network response.
     * 
     * @param messageBlock the {@link MessageBlock} to process
     * @throws IOException on failure to process the message
     */
    void processMessage(MessageBlock messageBlock) throws IOException;

    /**
     * Notifies this endpoint that it should terminate any pending operations and release all resources.
     * <p>
     * TODO (p1) 11.0: actually call/use this when appropriate
     */
    void dispose();
}
