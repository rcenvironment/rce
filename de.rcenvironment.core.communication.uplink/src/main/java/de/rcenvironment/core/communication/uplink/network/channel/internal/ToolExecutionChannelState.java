/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequestResponse;
import de.rcenvironment.core.communication.uplink.common.internal.MessageType;

/**
 * Represents the operation states that a {@link ToolExecutionChannelInitiatorEndpoint} or {@link ToolExecutionChannelProviderEndpoint} can
 * be in.
 *
 * @author Robert Mischke
 */
public enum ToolExecutionChannelState {
    /**
     * A state where no incoming messages (except {@link MessageType#CHANNEL_CLOSE}, which can be sent at any time) are expected or allowed.
     */
    EXPECTING_NO_MESSAGES,
    /**
     * A provider side state, waiting for the initial {@link ToolExecutionRequest}.
     */
    EXPECTING_EXECUTION_REQUEST,
    /**
     * An initiator side state, waiting for the initial {@link ToolExecutionRequestResponse}.
     */
    EXPECTING_EXECUTION_REQUEST_RESPONSE,
    /**
     * The provider side awaiting input files, or the initiator side awaiting output files.
     */
    EXPECTING_DIRECTORY_DOWNLOAD,
    /**
     * The initiator side expecting execution progress and/or staus events.
     */
    EXPECTING_EXECUTION_EVENTS,
    /**
     * Similar to {@link #EXPECTING_NO_MESSAGES}, but separated for more expressive logging of incoming messages after closing the channel.
     * 
     * TODO not used yet
     */
    CLOSED,
}
