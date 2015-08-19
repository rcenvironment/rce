/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.protocol;

import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkResponse;

/**
 * Constants related to the higher-level network protocol, for example metadata values. This also
 * includes version information for network compatibility checks.
 * 
 * @author Robert Mischke
 */
public final class ProtocolConstants {

    /**
     * Represents possible return codes contained in {@link NetworkResponse}s.
     * 
     * @author Robert Mischke
     */
    public enum ResultCode {
        /**
         * Value for marking an undefined result code.
         */
        UNDEFINED(0),

        /**
         * Request successful.
         */
        SUCCESS(1),

        /**
         * No {@link NetworkRequestHandler} on the receiving node was able to handle this message.
         */
        NO_MATCHING_HANDLER(101),

        /**
         * An exception occurred while handling the request at its final destination node.
         */
        EXCEPTION_AT_DESTINATION(102),

        /**
         * An exception occurred while forwarding/routing the request towards its final destination
         * node.
         */
        EXCEPTION_WHILE_FORWARDING(103),

        /**
         * The was no valid forwarding route on a node between sender and final recipient.
         */
        NO_ROUTE_TO_DESTINATION_WHILE_FORWARDING(104),

        /**
         * The was no valid route to the destination at the initial sender.
         */
        NO_ROUTE_TO_DESTINATION_AT_SENDER(107),

        /**
         * The channel could not be used as it has already been closed or marked as broken.
         */
        CHANNEL_CLOSED(105),

        /**
         * A timeout occurred while waiting for a response after sending the request into a channel.
         */
        TIMEOUT_WAITING_FOR_RESPONSE(106);

        private final int code;

        private ResultCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        /**
         * @param code a numeric code
         * @return the associated {@link ResultCode} object; if none exists, an
         *         {@link IllegalArgumentException} is thrown
         */
        public static ResultCode fromCode(int code) {
            for (ResultCode rc : values()) {
                if (rc.code == code) {
                    return rc;
                }
            }
            throw new IllegalArgumentException("Invalid code: " + code);
        }

        @Override
        public String toString() {
            return String.format("%s (%d)", name(), code);
        }
    }

    /**
     * An arbitrary protocol version or compatibility string used to determine if two nodes can
     * establish a compatible connection.
     * 
     * Note that there are no "higher/lower version" semantics for the content of this string. On a
     * connection attempt, the strings provided by both nodes are only checked for equality, and
     * possibly displayed as part of the error message if they do not match.
     */
    public static final String PROTOCOL_COMPATIBILITY_VERSION = "6.0.0-final";

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

    private ProtocolConstants() {}
}
