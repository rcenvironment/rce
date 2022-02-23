/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * An enum representing all content message types used in the uplink protocol.
 * <p>
 * "Usage" tag glossary:
 * <ul>
 * <li>I2R - Initiator (of a certain message) to Relay
 * <li>R2I - Relay to Initiator (response to an I2R message)
 * <li>R2D - Relay to Destination (of a certain message)
 * <li>D2R - Destination to Relay (response to an R2D message)
 * <li>C2R - Client to Relay (as the final recipient of the message)
 * <li>R2C - Relay to Client (as the final recipient of the message)
 * </ul>
 * 
 * @author Robert Mischke
 */
public enum MessageType {

    /**
     * The message type for the initial handshake's data exchange. Usage: C2R, R2C.
     */
    HANDSHAKE(UplinkProtocolConstants.MESSAGE_TYPE_HANDSHAKE),

    /**
     * A periodic message for testing the integrity of the bidirectional message exchange. Implementations may choose not to actively send
     * them, but they are required to respond to them (see {@link #HEARTBEAT_RESPONSE}.
     */
    HEARTBEAT(125),

    /**
     * The response type to {@link #HEARTBEAT} messages. Clients must respond with these when receiving a {@link #HEARTBEAT} message.
     */
    HEARTBEAT_RESPONSE(126),
    /**
     * The message type for the special "end of session" goodbye message. The body is empty on a regular shutdown, or a UTF-8-encoded error
     * message on connection failure. Usage: C2R, R2C.
     */
    GOODBYE(UplinkProtocolConstants.MESSAGE_TYPE_GOODBYE),

    /**
     * The code for {@link ToolDescriptorListUpdate} messages. Usage: I2R, R2I.
     */
    TOOL_DESCRIPTOR_LIST_UPDATE(11),

    // Channel Management

    /**
     * Requests the creation of a channel. Usage: I2R.
     */
    CHANNEL_INIT(21),
    /**
     * Offers the creation of a channel by request of a different client (the "initiator"). Usage: R2D.
     */
    CHANNEL_OFFER(22),
    /**
     * Accepts or refuses an offered channel. Usage: D2R.
     */
    CHANNEL_OFFER_RESPONSE(23),
    /**
     * Reports the success or failure of a channel creation request. Usage: R2I.
     */
    CHANNEL_INIT_RESPONSE(24),
    /**
     * The final message sent over a channel to close it. Usage: I2R, R2D, D2R, R2I.
     */
    CHANNEL_CLOSE(24),

    // Tool execution life cycle and events

    /**
     * A {@link ToolExecutionRequest}. Usage: I2D.
     */
    TOOL_EXECUTION_REQUEST(31),
    /**
     * The response to a {@link ToolExecutionRequest}. Usage: D2I.
     */
    TOOL_EXECUTION_REQUEST_RESPONSE(32),
    /**
     * Feedback events during a tool's execution, for example stdout/stderr output, or execution state change events. Usage: D2I.
     */
    TOOL_EXECUTION_EVENTS(33),
    /**
     * Reports the end of the provider-side tool execution, and transport the {@link ToolExecutionResult}. Usage: D2I.
     */
    TOOL_EXECUTION_FINISHED(34),
    /**
     * Signals a request to cancel the tool execution associated with the current channel. Usage: I2D.
     */
    TOOL_CANCELLATION_REQUEST(35),

    // File transfers

    /**
     * Signals the start of a sequence of zero or more sub-sequences, each comprised of a FILE_HEADER block and zero or more FILE_CONTENT
     * blocks. This message block's payload is a JSON object with additional metadata; currently, its only field is the list of all
     * sub-directories of the directory to transfer, to allow early creation before actually transfering files, and allow transfer of empty
     * sub-directories. This object may be used to transport additional data in the future, e.g. maximum data block sizes. Usage: I2D (input
     * files), D2I (output files).
     */
    FILE_TRANSFER_SECTION_START(41),
    /**
     * Metadata about a file to be transferred. Usage: I2D, D2I.
     */
    FILE_HEADER(42),
    /**
     * An announced file's binary content; the number of content blocks is specified by the preceding header data. Usage: I2D, D2I.
     */
    FILE_CONTENT(43),
    /**
     * Signals the end of the sequence of files. Usage: I2D, D2I.
     */
    FILE_TRANSFER_SECTION_END(44),

    /**
     * A request for tool documentation. Type: JSON. Usage: I2D.
     */
    TOOL_DOCUMENTATION_REQUEST(51),
    /**
     * The response for a tool documentation request. Type: JSON. Usage: D2I.
     */
    TOOL_DOCUMENTATION_RESPONSE(52),
    /**
     * The blocks of the data stream following a positive tool documentation response. Type: binary. Usage: D2I.
     */
    TOOL_DOCUMENTATION_CONTENT(53),

    /**
     * A special reserved message, usually for testing.
     */
    TEST(99),

    ;

    private final byte code;

    MessageType(int code) {
        this.code = (byte) code;
        if (this.code < UplinkProtocolConstants.MIN_MESSAGE_TYPE_VALUE || this.code > UplinkProtocolConstants.MAX_MESSAGE_TYPE_VALUE) {
            throw new IllegalArgumentException(); // outside allowed range for content messages
        }
    }

    /**
     * @param code the code to look up
     * @return the {@link MessageType} matching the given code
     * @throws ProtocolException if the type code does not match any known type
     */
    public static MessageType resolve(byte code) throws ProtocolException {
        // simple linear search; optimize when necessary
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ProtocolException("Unrecognized message type: " + code);
    }

    public byte getCode() {
        return code;
    }

    @Override
    public String toString() {
        return StringUtils.format("%s (%d)", name(), code);
    }
}
