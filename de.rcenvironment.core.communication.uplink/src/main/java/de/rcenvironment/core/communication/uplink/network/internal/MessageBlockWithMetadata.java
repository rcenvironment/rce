/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

public final class MessageBlockWithMetadata extends MessageBlock {

    private final long channelId;

    // local only; not transmitted
    private final MessageBlockPriority priority;

    // local only; not transmitted
    private long queueStartTime; // for tracking local "time until sent", "time until processed", or similar

    // design note: if message cancellation (e.g. for incomplete bulk transfers) is needed, consider adding the flag here, too

    public MessageBlockWithMetadata(int type, byte[] data, long channelId, MessageBlockPriority priority) throws ProtocolException {
        super(type, data);
        this.channelId = channelId;
        this.priority = priority;
    }

    public MessageBlockWithMetadata(MessageBlock original, long channelId, MessageBlockPriority priority) throws ProtocolException {
        super(original.getType(), original.getData());
        this.channelId = channelId;
        this.priority = priority;
    }

    public long getChannelId() {
        return channelId;
    }

    public MessageBlockPriority getPriority() {
        return priority;
    }

    public long getLocalQueueStartTime() {
        return queueStartTime;
    }

    public void setLocalQueueStartTime(long localProcessingStartTime) {
        this.queueStartTime = localProcessingStartTime;
    }
}
