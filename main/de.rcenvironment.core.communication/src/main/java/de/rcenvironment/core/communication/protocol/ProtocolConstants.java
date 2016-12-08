/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.protocol;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Constants related to the higher-level network protocol, for example metadata values. This also includes version information for network
 * compatibility checks.
 * 
 * @author Robert Mischke
 */
public final class ProtocolConstants {

    /**
     * An arbitrary protocol version or compatibility string used to determine if two nodes can establish a compatible connection.
     * 
     * Note that there are no "higher/lower version" semantics for the content of this string. On a connection attempt, the strings provided
     * by both nodes are only checked for equality, and possibly displayed as part of the error message if they do not match.
     */
    public static final String PROTOCOL_COMPATIBILITY_VERSION = "8.0.0-final";

    /**
     * Represents possible return codes contained in {@link NetworkResponse}s.
     * 
     * @author Robert Mischke
     */
    public enum ResultCode {
        /**
         * Value for marking an undefined result code.
         */
        UNDEFINED(0, null),

        /**
         * Request successful.
         */
        SUCCESS(1, null),

        /**
         * An exception occurred while handling the request at its final destination node.
         */
        EXCEPTION_AT_DESTINATION(102, "An internal error occured on the destination node while processing this request; "
            + "more information may be available in the destination node's log files"),

        /**
         * An exception occurred while forwarding/routing the request towards its final destination node.
         */
        EXCEPTION_DURING_DELIVERY(103, "There was an error while delivering the request to the destination node"),

        /**
         * The was no valid forwarding route on a node between sender and final recipient.
         */
        NO_ROUTE_TO_DESTINATION_WHILE_FORWARDING(104, "An instance between sender and destination was unable to forward the request; "
            + DESTINATION_MAY_HAVE_BECOME_UNREACHABLE_TEXT),

        /**
         * There was a forwarding route to the final recipient, but when the request was to be sent to the next hop, the message channel was
         * already marked as broken.
         */
        CHANNEL_CLOSED_OR_BROKEN_BEFORE_SENDING_REQUEST(105,
            "A network connection between sender and destination has been closed (by user action or an error) "
                + "while sending/forwarding the request; " + DESTINATION_MAY_HAVE_BECOME_UNREACHABLE_TEXT),

        /**
         * While waiting for the response to a sent request, the non-blocking response listener was shut down.
         */
        CHANNEL_OR_RESPONSE_LISTENER_SHUT_DOWN_WHILE_WAITING_FOR_RESPONSE(106,
            "A network connection between sender and destination has been closed (by user action or an error) "
                + "while awaiting the response; " + DESTINATION_MAY_HAVE_BECOME_UNREACHABLE_TEXT),

        /**
         * The was no valid route to the destination at the initial sender.
         */
        NO_ROUTE_TO_DESTINATION_AT_SENDER(107, "The destination instance for this request is unreachable; it was probably "
            + "contacted because it was reachable at an earlier time"),

        /**
         * A timeout occurred while waiting for a response after sending the request into a channel.
         */
        TIMEOUT_WAITING_FOR_RESPONSE(108,
            "The destination instance for this request did not answer in time, or the response got lost because of a network error"),

        /**
         * A placeholder result code if an invalid code was passed in for conversion via {@link #fromCode(int)}.
         */
        INVALID_RESULT_CODE(999, null);

        private final int code;

        private final String stringForm;

        // note: userMessage may be null for results that are not typically presented to the user
        ResultCode(int code, String userMessage) {
            this.code = code;
            // prepare and store toString() message
            if (userMessage != null) {
                stringForm = StringUtils.format("%s (error code %d)", userMessage, code);
            } else {
                stringForm = StringUtils.format("%s (error code %d)", name(), code);
            }
        }

        public int getCode() {
            return code;
        }

        /**
         * @param code a numeric code
         * @return the associated {@link ResultCode} object; if none exists, an {@link IllegalArgumentException} is thrown
         */
        public static ResultCode fromCode(int code) {
            for (ResultCode rc : values()) {
                if (rc.code == code) {
                    return rc;
                }
            }
            LogFactory.getLog(ProtocolConstants.class).error("Received an invalid result code: " + code);
            return INVALID_RESULT_CODE;
        }

        @Override
        public String toString() {
            return stringForm;
        }
    }

    /**
     * Top-level type for remote service call (RPC) messages.
     */
    public static final String VALUE_MESSAGE_TYPE_RPC = "rpc";

    /**
     * Top-level type for connection health check ("ping") messages.
     */
    public static final String VALUE_MESSAGE_TYPE_HEALTH_CHECK = "healthCheck";

    /**
     * Top-level type for routing meta-information (LSA) messages.
     */
    @Deprecated
    public static final String VALUE_MESSAGE_TYPE_LSA = "lsa";

    /**
     * Top-level type for distributed node property update messages.
     */
    public static final String VALUE_MESSAGE_TYPE_NODE_PROPERTIES_UPDATE = "np";

    /**
     * Top-level type for dummy integration test messages.
     */
    public static final String VALUE_MESSAGE_TYPE_TEST = "test";

    /**
     * The TTL (in msec) set for outgoing messages by transports that support this concept.
     */
    public static final long JMS_MESSAGES_TTL_MSEC = 60 * 1000;

    // common text
    private static final String DESTINATION_MAY_HAVE_BECOME_UNREACHABLE_TEXT = "the destination instance may have become unreachable";

    private ProtocolConstants() {}
}
