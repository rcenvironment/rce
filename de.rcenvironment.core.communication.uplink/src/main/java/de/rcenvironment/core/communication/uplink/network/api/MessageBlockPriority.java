/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.api;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;

/**
 * Marks the priority level of a {@link MessageBlock}. Only used locally, i.e., this value is not transmitted as part of the wire protocol.
 * The effective priority is defined by the {@link Enum} being used as a {@link Comparable}, so the ordering of the Enum fields (first =
 * highest) IS relevant!
 * <p>
 * Currently, these values are only used to prioritize messages for sending; they could be used to prioritize processing of incoming
 * messages in the future, too. (It is not clear whether the latter is actually useful, though, as it would have to be derived from the
 * message's type or content, at which point custom processing can be initiated anyway.)
 * <p>
 * IMPORTANT: When implementing protocol flows, keep in mind that messages within the same priority are guaranteed to be sequential in
 * relation to each other, but NOT in relation to other priorities. Therefore, when a certain sequence of messages must be maintained
 * (typically for a sub-protocol within a channel), make sure that they are sent using the same {@link #SendPriority()}. Switching to a
 * different priority can be done, but should only occur in response to a synchronizing event, e.g. receiving a certain response message
 * from the remote side. -- misc_ro
 *
 * @author Robert Mischke
 */
public enum MessageBlockPriority implements Comparable<MessageBlockPriority> {

    /**
     * High-priority messages. Currently only used for "heartbeat" messages; cancellation messages (once implemented) and "goodbye" markers
     * would be a good fit, too. However, the latter are currently sent directly, overriding the priority system, so this is only relevant
     * on future rework.
     */
    HIGH,

    /**
     * The default priority unless specified otherwise. (Surprising, I know.)
     * <p>
     * As a point of reference, the exchange of main state/status data, especially the distribution of tool list updates, takes place on
     * this priority. Messages on this priority level should typically be non-blockable.
     */
    DEFAULT,

    /**
     * A low priority level for messages that are non-blockable (e.g. due to timeout limits), but can wait a little longer in case of low
     * bandwidth or high system load. As a guideline, if a request-response timeout can be handled gracefully without risk of inconsistency,
     * the related messages should probably go here. Examples include channel creation requests and tool execution requests.
     */
    LOW_NON_BLOCKABLE,

    /**
     * The priority level for messages that are forwarded/relayed by the server, with potentially unknown content. As these will include
     * bulk data transfers, any forwarded messages must be prepared to encounter delays, i.e. the initiating clients must not expect low
     * timeouts to be kept. For this reason, this category of messages is considered non-urgent on the server side, as any kind of assurance
     * would require knowing and handling the content of these messages.
     */
    FORWARDING,

    /**
     * The lowest priority for requests that can be blocked by backpressure without problems, and can wait indefinitely without causing
     * timeouts. Most notably, these are tool execution preparations, bulk data transfers, and non-critical tool execution events (e.g.
     * console output or status changes). This is represented as a different priority level than {@link #FORWARDING} as these require highly
     * different message limits: Forwarding on the server side should be able to buffer a fair amount, while bulk data transfers on the
     * client side should not buffer (i.e. read ahead) excessively to conserve memory.
     */
    LOW_BLOCKABLE;

    /**
     * An alias for all tool descriptor update messages.
     */
    public static final MessageBlockPriority TOOL_DESCRIPTOR_UPDATES = DEFAULT;

    /**
     * An alias for channel requests (e.g. "create a new channel to instance X for downloading a tool's documentation"). The actual
     * operations should typically use {@link #BLOCKABLE_CHANNEL_OPERATION}.
     */
    public static final MessageBlockPriority CHANNEL_INITIATION = LOW_NON_BLOCKABLE;

    /**
     * A generic alias for operations within a created channel that can be blocked without risk of timeouts. These include tool
     * documentation transfer, tool execution preparation and tool input/output transfer.
     */
    public static final MessageBlockPriority BLOCKABLE_CHANNEL_OPERATION = LOW_BLOCKABLE;

    /**
     * @return the zero-based ordinal number of this priority, aliased for readability
     */
    public int getIndex() {
        return this.ordinal();
    }

}
