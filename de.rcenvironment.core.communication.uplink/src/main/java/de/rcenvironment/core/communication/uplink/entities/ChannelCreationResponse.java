/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;

/**
 * A data transfer object for {@link MessageType#CHANNEL_INIT_RESPONSE} and {@link MessageType#CHANNEL_OFFER_RESPONSE} messages.
 *
 * @author Robert Mischke
 */
public class ChannelCreationResponse implements Serializable {

    private static final long serialVersionUID = -4553771418343934859L;

    /**
     * The assigned channel id (on success); also used as an association id when the destination client responds to the relay.
     */
    private long channelId;

    /**
     * The request id sent by the request initiator (for association).
     */
    private String requestId;

    /**
     * Whether the request was accepted and successfully executed.
     */
    private boolean success;

    public ChannelCreationResponse() {
        // for deserialization
    }

    public ChannelCreationResponse(long channelId, String requestId, boolean success) {
        this.channelId = channelId;
        this.requestId = requestId;
        this.success = success;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return success;
    }

}
