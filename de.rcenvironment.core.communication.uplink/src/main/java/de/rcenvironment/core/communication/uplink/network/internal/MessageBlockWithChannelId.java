/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import de.rcenvironment.core.utils.common.exception.ProtocolException;

public final class MessageBlockWithChannelId extends MessageBlock {

    private final long channelId;

    public MessageBlockWithChannelId(int type, byte[] data, long channelId) throws ProtocolException {
        super(type, data);
        this.channelId = channelId;
    }

    public long getChannelId() {
        return channelId;
    }
}
