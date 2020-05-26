/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents the error codes that can be sent as part of the Uplink protocol. Currently, all error codes are sent from the server to the
 * client, but this may change in the future.
 *
 * @author Robert Mischke
 */
public enum UplinkProtocolErrorType {
    /**
     * A pseudo type for I/O errors occurring "below" the protocol level. Auto-retry is generally considered useful in this case, as all
     * kinds of network disruptions will manifest in this way. There are also counter-examples where retry is not useful, for example a
     * connection being actively blocked on the network level, but it is infeasible to detect these in general.
     * <p>
     * NB: The special case of a low-level protocol version mismatch between client and server will be detected in the current
     * implementation by providing that version information "out of band", ie before the Uplink connection is even fully established, using
     * the SSH server banner feature. This event should be mapped to {@link #PROTOCOL_VERSION_MISMATCH} to prevent a useless retry loop.
     */
    LOW_LEVEL_CONNECTION_ERROR(0, true),

    /**
     * A "high-level" protocol version mismatch between client and server; typically detected by the server, which then refuses the
     * connection.
     */
    PROTOCOL_VERSION_MISMATCH(1, false),

    /**
     * Thrown by the server when a second client attempts to log in with an account+qualifier combination that is already in use.
     */
    CLIENT_NAMESPACE_COLLISION(2, false),

    /**
     * Thrown during a connection attempt when the server is not accepting new connections, or when the server closes existing connections
     * when it is shutting down.
     */
    SERVER_SHUTTING_DOWN(10, true),

    /**
     * Thrown by the server when the client's handshake data is missing a required field, or some field's data is invalid.
     */
    INVALID_HANDSHAKE_DATA(91, false),

    /**
     * Thrown by the server on internal errors.
     */
    INTERNAL_SERVER_ERROR(92, false),

    /**
     * Fallback for when an error message cannot be recognized as a known type.
     */
    UNKNOWN_ERROR(99, false);

    private static final Pattern PARSE_PATTERN = Pattern.compile("E(\\d+): (.*)");

    private int code;

    private boolean clientRetry;

    UplinkProtocolErrorType(int code, boolean clientRetry) {
        this.code = code;
        // TODO Auto-generated constructor stub
        this.clientRetry = clientRetry;
    }

    public int getCode() {
        return code;
    }

    /**
     * @return whether it makes sense for a client to initiate auto-retry when receiving this error
     */
    public boolean getClientRetryFlag() {
        return clientRetry;
    }

    String wrapErrorMessage(String message) {
        return StringUtils.format("E%d: %s", code, message);
    }

    static UplinkProtocolErrorType typeOfWrappedErrorMessage(String message) {
        final Matcher matcher = PARSE_PATTERN.matcher(message);
        if (matcher.matches()) {
            int inputCode = Integer.parseInt(matcher.group(1));
            for (UplinkProtocolErrorType type : values()) {
                if (type.code == inputCode) {
                    return type;
                }
            }
            LogFactory.getLog(UplinkProtocolErrorType.class).warn("Failed to recognize error code of message; raw text: " + message);
            return UNKNOWN_ERROR;
        } else {
            LogFactory.getLog(UplinkProtocolErrorType.class).warn("Failed to parse error code of message; raw text: " + message);
            return UNKNOWN_ERROR;
        }
    }

    static String unwrapErrorMessage(String message) {
        final Matcher matcher = PARSE_PATTERN.matcher(message);
        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            // this seems preferable to failing on unexpected/malformed input
            return "<Unexpected error formatting>: " + message;
        }
    }
}
