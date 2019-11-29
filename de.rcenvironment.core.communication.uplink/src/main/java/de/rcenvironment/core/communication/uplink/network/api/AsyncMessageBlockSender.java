/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.api;

import java.io.IOException;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;

/**
 * Represents an abstract asynchronous in-order sender of {@link MessageBlock}s.
 *
 * @author Robert Mischke
 */
public interface AsyncMessageBlockSender {

    /**
     * Stores the given {@link MessageBlock} in an internal queue for sending and returns immediately. Message order between concurrent
     * callers of this method is preserved.
     * 
     * @param channelId the id of the virtual channel to send this {@link MessageBlock} to
     * @param messageBlock the {@link MessageBlock} to send
     * @throws IOException on errors or interruption while waiting to enqueue this message
     */
    void enqueueMessageBlockForSending(long channelId, MessageBlock messageBlock) throws IOException;
}
