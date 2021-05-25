/*
 * Copyright 2021 DLR, Germany
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
     * High-priority messages. Currently only used for "heartbeat" messages; cancellation messages (once implemented) would be a good fit,
     * too.
     */
    HIGH,

    /**
     * The default priority unless specified otherwise. (Surprising, I know.)
     */
    DEFAULT;

    /**
     * @return the zero-based ordinal number of this priority, aliased for readability
     */
    public int getIndex() {
        return this.ordinal();
    }

}
