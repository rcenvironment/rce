/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.api;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Represents an abstract asynchronous in-order sender of {@link MessageBlock}s.
 *
 * @author Robert Mischke
 */
public interface AsyncMessageBlockSender {

    /**
     * Stores the given {@link MessageBlock} in an internal queue for sending at the given priority and returns immediately. The message
     * ordering within each priority is preserved.
     * 
     * @param channelId the id of the virtual channel to send this {@link MessageBlock} to
     * @param messageBlock the {@link MessageBlock} to send
     * @param priority the {@link MessageBlockPriority} for selecting which messages to transmit first; the value itself is not transmitted
     * @param allowBlocking controls this method's behavior when the specified queue is full due to backpressure; false = fail internally
     *        and terminate the session, true = block this method until there is space in the queue; the latter must be properly handled by
     *        the calling code to avoid deadlocks
     * @throws ProtocolException on an invalid message or channel id
     */
    void enqueueMessageBlockForSending(long channelId, MessageBlock messageBlock, MessageBlockPriority priority, boolean allowBlocking)
        throws ProtocolException;
}
