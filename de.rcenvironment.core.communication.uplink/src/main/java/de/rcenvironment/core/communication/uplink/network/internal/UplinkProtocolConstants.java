/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.nio.charset.Charset;

import org.apache.commons.io.Charsets;

/**
 * Constants for the Uplink protocol.
 *
 * @author Robert Mischke
 */
public final class UplinkProtocolConstants {

    /**
     * The version of the low-level/"wire" protocol to send and expect. If this does not match, the connection can only be dropped/refused,
     * as there is no agreed way to transmit an error message.
     */
    public static final String WIRE_PROTOCOL_VERSION = "1.0";

    /**
     * The high-level protocol version to send for compatibility checking. If this does not match, the connection can be refused gracefully,
     * as the matching low-level protocol allows to transmit an error message.
     * <p>
     * Note that currently, the server expects an exact match using this same constant; this could be extended in the future to make the
     * server side accept multiple protocol versions.
     */
    public static final String DEFAULT_PROTOCOL_VERSION = "0.2";

    /**
     * The "high-level protocol version" sent by 10.0 and 10.1 clients. If a client sends this, consider the actual protocol version to be
     * {@link #LEGACY_PROTOCOL_VERSION_0_1_CLIENT_VALUE}.
     */
    public static final String LEGACY_PROTOCOL_VERSION_0_1_CLIENT_VALUE = "10.0.0";

    /**
     * The value the legacy version string {@link #LEGACY_PROTOCOL_VERSION_0_1_CLIENT_VALUE} is rewritten to for consistent parsing.
     */
    public static final String LEGACY_PROTOCOL_VERSION_0_1 = "0.1";

    /**
     * Handshake key for the protocol version(s) offered by the client to the server.
     */
    public static final String HANDSHAKE_KEY_PROTOCOL_VERSION_OFFER = "protocolVersion";

    /**
     * The handshake response key from server to client stating the final protocol version to use. Especially important if/when protocol
     * version negotiation is added in the future.
     */
    public static final String HANDSHAKE_KEY_EFFECTIVE_PROTOCOL_VERSION = "protocolVersion";

    /**
     * Handshake data key for the client's software version; intended as an informational field, e.g. for logging and client version
     * statistics. SHOULD NOT be relied upon by the server for compatibility checking etc.
     * 
     * Standard RCE clients SHOULD send a string like "rce/<core version>". Other clients MUST NOT use the "rce/" prefix, i.e. they should
     * not impersonate a real client, but reflect the actual software being used instead.
     * 
     * Clients MAY omit this from their handshake data completely, or send a static (non-versioned) string, although neither is recommended
     * in production usage.
     */
    public static final String HANDSHAKE_KEY_CLIENT_VERSION_INFO = "clientVersion";

    /**
     * Handshake data key for the the "session qualifier"/"client id" allowing multiple logins using the same account while keeping them
     * distinguishable.
     */
    public static final String HANDSHAKE_KEY_SESSION_QUALIFIER = "sessionQualifier";

    /**
     * Handshake key for the client's namespace id (which is also the prefix to use for all destination ids of that client) assigned by the
     * relay.
     */
    public static final String HANDSHAKE_KEY_ASSIGNED_NAMESPACE_ID = "namespace";

    /**
     * A client can send this to trigger a handshake failure with the associated value used as the message.
     */
    public static final String HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE = "simulateHandshakeFailure";

    /**
     * A client can send this to trigger a connection refusal with the associated value used as the message.
     */
    public static final String HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION = "simulateRefusedConnection";

    /**
     * A client can send this to trigger a long server-side delay before sending the handshake response.
     */
    public static final String HANDSHAKE_KEY_SIMULATE_HANDSHAKE_RESPONSE_DELAY_ABOVE_TIMEOUT = "simulateHandshakeTimeout";

    /**
     * The number of characters of the login account name (starting from left) that are actually relevant for the client's identity.
     * <p>
     * SECURITY ASSESSMENT: Impersonation attacks exploiting this are currently unlikely as accounts on a relay are not created by users,
     * but manually by administrators. If this ever changes, precautions against these attacks must be taken. -- misc_ro, Sept 2019
     */
    public static final int LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS = 8;

    /**
     * The number of characters of the session qualifier/"client id" (starting from left) that are actually relevant for the client's
     * identity.
     * <p>
     * SECURITY ASSESSMENT: As this identifier can be arbitrarily chosen by the client anyway, and is only relevant to distinguish users of
     * the same, already authenticated account, this does not pose an additional risk. -- misc_ro, Sept 2019
     */
    public static final int SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS = 8;

    /**
     * The total length of the (internal) destination id prefix, which is the concatenation of the above parts, each padded to maximum
     * length.
     */
    public static final int DESTINATION_ID_PREFIX_LENGTH =
        LOGIN_ACCOUNT_NAME_SIGNIFICANT_CHARACTERS + SESSION_QUALIFIER_SIGNIFICANT_CHARACTERS;

    /**
     * The character to right-pad the login name and session qualifier/"client id" (individually) before concatenating them into the
     * destination id prefix.
     */
    // TODO (p1) 10.x/11.0: preliminary; align this with SSH login and client identifier rules
    public static final char DESTINATION_ID_PREFIX_PADDING_CHARACTER = '-';

    /**
     * The character to right-pad the login name and session qualifier/"client id" (individually) before concatenating them into the
     * destination id prefix, as a String instead of a character.
     */
    public static final String DESTINATION_ID_PREFIX_PADDING_CHARACTER_AS_STRING =
        Character.toString(DESTINATION_ID_PREFIX_PADDING_CHARACTER);

    /**
     * The default "session qualifier"/"client id" value, both for usage as a GUI default, and as the default value if none is specified via
     * configuration.
     */
    public static final String SESSION_QUALIFIER_DEFAULT = "default";

    /**
     * The average interval between sending heartbeat message from server to client.
     * 
     * Currently, this is also the implicit timeout for the response, as response reception is currently checked before sending the next
     * heartbeat for simplicity.
     * 
     * TODO split these parameters so they can be adjusted independently?
     */
    public static final int SERVER_TO_CLIENT_HEARTBEAT_SEND_INVERVAL_AVERAGE = 30 * 1000; // raised in 10.2.3: 20->30

    /**
     * The maximum time that each side of the handshake exchange waits for the other side's response. Adjust if necessary.
     */
    public static final int HANDSHAKE_RESPONSE_TIMEOUT_MSEC = 2000;

    /**
     * The variation width of the heartbeat interval; added to avoid heartbeat events all arriving at the same time when started in sync.
     */
    public static final int SERVER_TO_CLIENT_HEARTBEAT_SEND_INVERVAL_SPREAD = 2 * 1000;

    /**
     * If the time between sending a heartbeat and receiving the related response exceeds this time, a warning is logged.
     */
    public static final int HEARTBEAT_RESPONSE_TIME_WARNING_THRESHOLD = 5000;

    /**
     * The lowest valid message type value.
     */
    public static final int MIN_MESSAGE_TYPE_VALUE = 1;

    /**
     * The highest valid message type value.
     */
    public static final int MAX_MESSAGE_TYPE_VALUE = 127;

    /**
     * The lowest valid non-reserved message type value.
     */
    public static final int MIN_CONTENT_TYPE_VALUE = 1;

    /**
     * The highest valid non-reserved message type value.
     */
    public static final int MAX_CONTENT_TYPE_VALUE = 99;

    /**
     * The message code for the initial handshake's data exchange. Usage: C2R, R2C.
     * <p>
     * Note that this special message's code MUST be OUTSIDE the "content" message type range defined above
     */
    public static final byte MESSAGE_TYPE_HANDSHAKE = 121;

    /**
     * The message code for the "end of session" goodbye message. Usage: C2R, R2C.
     * <p>
     * Note that this special message's code MUST be OUTSIDE the "content" message type range defined above
     */
    public static final byte MESSAGE_TYPE_GOODBYE = 127;

    /**
     * The "default" message channel; this is the one for exchanging general messages, e.g. the initial connection handshake, tool
     * descriptor list updates, channel management, and session shutdowns.
     */
    public static final long DEFAULT_CHANNEL_ID = 0L;

    /**
     * A placeholder for an undefined or invalid channel id, for example when sending a failed channel creation response.
     */
    public static final long UNDEFINED_CHANNEL_ID = -1L;

    /**
     * The maximum valid length of the data byte array.
     */
    public static final int MAX_MESSAGE_BLOCK_DATA_LENGTH = 256 * 1024; // 256 kb

    /**
     * The maximum data length of FILE_CONTENT message blocks. If there is sufficient data remaining in the source file, this size is always
     * used, so the only messages smaller than that will be the final ones of each file.
     */
    // note: this was lowered in 10.2.3; was using MAX_MESSAGE_BLOCK_DATA_LENGTH before
    public static final long MAX_FILE_TRANSFER_CHUNK_SIZE = 32 * 1024; // maybe go even lower? should be benchmarked to optimize

    /**
     * The maximum expected time between sending one's own handshake data and receiving a response. As this should be a very fast and
     * low-bandwidth operation, this interval should be fairly short even for connections outside of an organization's network.
     */
    protected static final int HANDSHAKE_FORMAT_VERSION = 1;

    // note: header version "0" for development; the first release version should be "1"
    protected static final String HANDSHAKE_HEADER_STRING = "INIT v0 "; // IMPORTANT: must be padded to result in 8 UTF-8 bytes

    protected static final int HANDSHAKE_INIT_STRING_BYTE_LENGTH = 8; // must match the above

    protected static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    /**
     * The lowest valid message type value.
     */
    protected static final int MIN_TYPE_VALUE = 1;

    /**
     * The highest valid message type value.
     */
    protected static final int MAX_TYPE_VALUE = 127;

    private UplinkProtocolConstants() {}
}
