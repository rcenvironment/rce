/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.api;

/**
 * Defines the states a {@link UplinkSession} can be in. These states are used for both the client and server side, but may have slightly
 * different meanings for them; see each state's JavaDoc for details.
 *
 * @author Robert Mischke
 */
public enum UplinkSessionState {

    /**
     * The initial state. For server sessions, this also represents waiting for client handshake data.
     */
    INITIAL(false),

    /**
     * For client sessions, this represents waiting for the server's response after sending handshake data. For server sessions, this means
     * client handshake data has been received and is being processed.
     */
    CLIENT_HANDSHAKE_REQUEST_READY(false),

    /**
     * For client sessions, this means that server handshake data has been received and is being processed. For server sessions, this state
     * is not used, as it will be either {@link #ACTIVE} or {@link #REFUSED} after sending its handshake response.
     */
    SERVER_HANDSHAKE_RESPONSE_READY(false),

    /**
     * The server has refused the connection for some reason. For clients, this is reached either by receiving an error response message
     * instead of a handshake response, or by receiving a handshake response signaling the refusal. For servers, this is reached after
     * processing the client's handshake data and sending its response. This and {@link #FULLY_CLOSED} are the terminal states.
     */
    SESSION_REFUSED_OR_HANDSHAKE_ERROR(true),

    /**
     * The main operational state after a successful handshake exchange.
     */
    ACTIVE(false),

    /**
     * Either side has sent an initial GOODBYE message, and is waiting for the other side to confirm.
     */
    GOODBYE_HANDSHAKE(false),

    /**
     * Both sides have sent a GOODBYE message, and will both close their outgoing streams.
     */
    GOODBYE_HANDSHAKE_COMPLETE(false),

    /**
     * The final, inactive state after a connection was {@link #ACTIVE} at some point. This and {@link #SESSION_REFUSED_OR_HANDSHAKE_ERROR}
     * are the terminal states.
     */
    CLEAN_SHUTDOWN(true),

    /**
     * Represents that the local side has encountered a fatal error, and will close the outgoing stream, but the session has not fully
     * terminated yet.
     */
    UNCLEAN_SHUTDOWN_INITIATED(false),
    /**
     * The terminal state for the end of a session after an error, including an unexpected close of the connection by the remote side, or a
     * stream write error.
     */
    UNCLEAN_SHUTDOWN(true);

    private boolean terminal;

    UplinkSessionState(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
