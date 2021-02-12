/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;

/**
 * A data transfer object for {@link MessageType#CHANNEL_INIT} and {@link MessageType#CHANNEL_OFFER} messages.
 *
 * @author Robert Mischke
 */
public class ChannelCreationRequest implements Serializable {

    private static final long serialVersionUID = 7102332183603983609L;

    /**
     * The requested channel type.
     */
    private String type;

    /**
     * The request id (for association) sent from the initiating client to the relay; it is unused when this message is sent by the relay,
     * as there is a channel id assigned at that point.
     */
    private String requestId;

    /**
     * The (preliminary) channel id when this message is sent from the relay to the destination session's client; otherwise, it is unused
     * and should be set to {@link UplinkProtocolConstants#UNDEFINED_CHANNEL_ID}.
     */
    private long channelId;

    private String destinationId;

    public ChannelCreationRequest() {
        // for deserialization
    }

    public ChannelCreationRequest(String type, String destinationId, long channelId, String requestId) {
        this.type = type;
        this.destinationId = destinationId;
        this.requestId = requestId;
        this.channelId = channelId;
    }

    public String getType() {
        return type;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getRequestId() {
        return requestId;
    }

}
