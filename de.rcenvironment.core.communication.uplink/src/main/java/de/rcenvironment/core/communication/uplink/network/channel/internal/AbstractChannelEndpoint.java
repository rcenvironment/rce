/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.channel.internal;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.network.api.AsyncMessageBlockSender;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.channel.api.ChannelEndpoint;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * The common base class of all Uplink channel endpoints. The responsibilities of this base class are:
 * <ul>
 * <li>Provide a framework for in-order message block processing with channel state tracking (not used for the default channel)
 * <li>Basic error/exception handling and initiating session teardown on errors
 * <li>Generalizing logging with session ids, including providing labeled {@link UplinkProtocolMessageConverter} instances
 * </ul>
 * <p>
 * Threading notice: This base class does not have any mutable state, and performs no synchronization on itself ("this") by default.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractChannelEndpoint implements ChannelEndpoint {

    private static final boolean DEBUG_OUTPUT_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.endpoints");

    protected final AsyncMessageBlockSender asyncMessageBlockSender;

    protected final UplinkProtocolMessageConverter messageConverter;

    protected final String sessionId;

    protected final String channelLogPrefix; // TODO >10.2.4 (p3) use this in more places

    protected final long channelId;

    protected final Log log = LogFactory.getLog(getClass());

    public AbstractChannelEndpoint(AsyncMessageBlockSender asyncMessageBlockSender, String sessionId, long channelId) {
        this.asyncMessageBlockSender = asyncMessageBlockSender;
        this.sessionId = sessionId;
        this.channelLogPrefix = StringUtils.format("[%s//%d] ", sessionId, channelId); // TODO >10.2.4 (p3) inject namespace as well
        this.messageConverter = new UplinkProtocolMessageConverter(sessionId + "/c" + channelId); // >10.2.4 (p3) TODO use common prefix
        this.channelId = channelId;
    }

    @Override
    public final void processMessage(MessageBlock messageBlock) throws IOException {
        if (DEBUG_OUTPUT_ENABLED) {
            log.debug(StringUtils.format("%sProcessing a message of type %s", channelLogPrefix, messageBlock.getType()));
        }
        final MessageType messageType = messageBlock.getType();
        if (messageType == MessageType.HEARTBEAT) {
            // attempt to send a response no matter the local session state
            sendHeartbeatResponse();
            // this could received further processing in the future; for now, only send the response
            return;
        } else if (messageType == MessageType.HEARTBEAT_RESPONSE) {
            // heartbeat messages are currently supposed to be handled on the session level only, so log any that come through
            log.warn("Unexpected " + MessageType.HEARTBEAT_RESPONSE.name() + " message at channel endpoint " + sessionId);
            return;
        }
        try {
            if (!processMessageInternal(messageBlock)) {
                if (DEBUG_OUTPUT_ENABLED) {
                    log.debug(channelLogPrefix + "Uplink channel terminating");
                }
                // TODO additional steps to actually close/delete the channel?
            }
        } catch (IOException e) {
            // wrap with context information
            throw new IOException(
                StringUtils.format("%sError while processing a message of type %s'", channelLogPrefix, messageBlock.getType()), e);
        }
    }

    /**
     * The main entry point for subclass behavior. No synchronization is performed by default; subclasses must provide this if necessary.
     * 
     * @param message the received {@link MessageBlock}
     * @return whether further messages should be processed, i.e. returning false means that this channel shut be shut down
     * @throws IOException on failure to process the message
     */
    protected abstract boolean processMessageInternal(MessageBlock message) throws IOException;

    /**
     * Sends a {@link MessageBlock} the the stored channel id of this channel endpoint with a specified message-sending priority and
     * behavior (fail vs. block) on a full queue for that priority.
     * 
     * @param messageBlock the {@link MessageBlock} to send
     * @param priority the {@link MessageBlockPriority} for selecting which messages to transmit first; the value itself is not transmitted
     * @param allowBlocking controls this method's behavior when the specified queue is full due to backpressure; false = fail internally
     *        and terminate the session, true = block this method until there is space in the queue; the latter must be properly handled by
     *        the calling code to avoid deadlocks
     * @throws IOException on errors or interruption while waiting to enqueue this message
     */
    protected final void enqueueMessageBlockForSending(MessageBlock messageBlock, MessageBlockPriority priority, boolean allowBlocking)
        throws ProtocolException {
        asyncMessageBlockSender.enqueueMessageBlockForSending(channelId, messageBlock, priority, allowBlocking);
    }

    protected final boolean refuseUnexpectedMessageType(MessageBlock message) throws ProtocolException {
        log.error("Received an invalid or unexpected message of type " + message.getType() + " from session "
            + sessionId + "; the session will be terminated");
        return false;
    }

    private void sendHeartbeatResponse() {
        try {
            // Sends the response to the channel the heartbeat was received from; as heartbeat requests are currently sent to the default
            // channel only, this is typically the default channel as well.
            // parameter "false" = terminate session if the high-priority queue is full; in that case, something is severely wrong already
            asyncMessageBlockSender.enqueueMessageBlockForSending(channelId, new MessageBlock(MessageType.HEARTBEAT_RESPONSE),
                MessageBlockPriority.HIGH, false);
        } catch (ProtocolException e) {
            log.debug("Error attempting to send an Uplink heartbeat response: " + e.toString());
        }
    }
}
