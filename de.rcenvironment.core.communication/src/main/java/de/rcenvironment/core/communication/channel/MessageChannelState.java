/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Indicates the overall state of a {@link MessageChannel}.
 * 
 * @author Robert Mischke
 */
public enum MessageChannelState {

    /**
     * The channel is connecting to a remote contact point.
     */
    CONNECTING,

    /**
     * The channel is ready to use.
     */
    ESTABLISHED,

    /**
     * The channel has been marked as broken due to an error.
     */
    MARKED_AS_BROKEN,

    /**
     * The channel has been closed normally.
     */
    CLOSED;
}
