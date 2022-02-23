/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import de.rcenvironment.core.communication.model.NetworkResponse;

/**
 * Constants used as part of JMS-based transport network protocols.
 * 
 * @author Robert Mischke
 */
public abstract class JmsProtocolConstants {

    /**
     * JMS property key for the message type.
     */
    public static final String MESSAGE_FIELD_MESSAGE_TYPE = "messageType";

    /**
     * JMS property key for the protocol compatibility check string.
     */
    public static final String MESSAGE_FIELD_PROTOCOL_VERSION = "protocol.version";

    /**
     * JMS property key for the metadata map.
     */
    public static final String MESSAGE_FIELD_METADATA = "metadata";

    /**
     * JMS property key for the integer result code of a {@link NetworkResponse}.
     */
    public static final String MESSAGE_FIELD_RESULT_CODE = "result.code";

    /**
     * JMS property key for transporting the queue name to use for remote-initiated requests; used in both directions.
     * 
     * Note that "remote-initiated request" is always relative to the side that sends this message; do not confuse this with
     * "remote-initiated message channel", which is relative to who initiated the underlying network (usually, TCP) connection.
     */
    public static final String MESSAGE_FIELD_REMOTE_INITIATED_REQUEST_INBOX = "queuename.requests.incoming";

    /**
     * JMS property key for transporting the client-to-broker request queue name during the initial handshake.
     */
    public static final String MESSAGE_FIELD_C2S_REQUEST_INBOX = "queuename.requests.c2s";

    /**
     * JMS property key for transporting a C2B channel name. Used for C2B shutdown messages.
     */
    public static final String MESSAGE_FIELD_CHANNEL_ID = "channel.id";

    /**
     * Message type value for the initial handshake request.
     */
    public static final String MESSAGE_TYPE_INITIAL = "initial";

    /**
     * Message type value for general requests.
     */
    public static final String MESSAGE_TYPE_REQUEST = "request";

    /**
     * Message type value for queue shutdown signals.
     */
    public static final String MESSAGE_TYPE_QUEUE_SHUTDOWN = "shutdown.queue.s2c";

    /**
     * Message type value for client-to-broker channel shutdown signals.
     */
    public static final String MESSAGE_TYPE_CHANNEL_CLOSING = "shutdown.channel";

    /**
     * The JMS queue name for the initial handshake inbox.
     */
    public static final String QUEUE_NAME_INITIAL_BROKER_INBOX = "initial/c2b";

    /**
     * The JMS queue name for the common request inbox.
     */
    public static final String QUEUE_NAME_C2B_REQUEST_INBOX = "requests/c2b/common";

    /**
     * The time to wait after sending a channel shutdown notice before actually closing the JMS connection. This prevents unnecessary JMS
     * exceptions on the remote side.
     */
    public static final long WAIT_AFTER_SENDING_SHUTDOWN_MESSAGE_MSEC = 1000;

}
