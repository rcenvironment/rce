/*
 * Copyright 2019-2020 DLR, Germany
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
    INITIAL,
    /**
     * For client sessions, this represents waiting for the server's response after sending handshake data. For server sessions, this means
     * client handshake data has been received and is being processed.
     */
    CLIENT_HANDSHAKE_REQUEST_READY,
    /**
     * For client sessions, this means that server handshake data has been received and is being processed. For server sessions, this state
     * is not used, as it will be either {@link #ACTIVE} or {@link #REFUSED} after sending its handshake response.
     */
    SERVER_HANDSHAKE_RESPONSE_READY,
    /**
     * The server has refused the connection for some reason. For clients, this is reached either by receiving an error response message
     * instead of a handshake response, or by receiving a handshake response signaling the refusal. For servers, this is reached after
     * processing the client's handshake data and sending its response. This and {@link #FULLY_CLOSED} are the terminal states.
     */
    SESSION_REFUSED_OR_HANDSHAKE_ERROR,
    /**
     * The main operational state after a successful handshake exchange.
     */
    ACTIVE,
    /**
     * The local side of the connection has started to shut it down, but it is not discarded yet.
     */
    PARTIALLY_CLOSED_BY_LOCAL,
    /**
     * The remote side of the connection has started to shut it down, but it is not discarded yet.
     */
    PARTIALLY_CLOSED_BY_REMOTE,
    /**
     * The final, inactive state after a connection was {@link #ACTIVE} at some point. This and {@link #SESSION_REFUSED_OR_HANDSHAKE_ERROR}
     * are the terminal states.
     */
    FULLY_CLOSED
}
