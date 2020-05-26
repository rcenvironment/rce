/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.session.api;

import java.util.Optional;

import de.rcenvironment.core.communication.uplink.network.api.AsyncMessageBlockSender;

/**
 * A common interface for shared aspects of client- and server-side Uplink sessions.
 *
 * @author Robert Mischke
 */
public interface UplinkSession extends AsyncMessageBlockSender {

    /**
     * @return the current {@link UplinkSessionState} of this session
     */
    UplinkSessionState getState();

    /**
     * @return the locally-assigned session id (separate for client and relay side) for mapping and logging purposes
     */
    String getLocalSessionId();

    /**
     * @return the string to identify this session in log output; always contains the local session id, and additionally the relay-assigned
     *         namespace id once it is available
     */
    String getLogDescriptor();

    /**
     * @return the relay-assigned namespace id once it is available (typically after the client-server handshake has successfully
     *         completed); throws an {@link IllegalStateException} if it is requested before
     */
    String getAssignedNamespaceId();

    /**
     * @return the relay-assigned namespace id once it is available (typically after the client-server handshake has successfully
     *         completed); returns {@link Optional#empty()} if none has been assigned (yet)
     */
    Optional<String> getAssignedNamespaceIdIfAvailable();

    /**
     * @return the relay-assigned destination id prefix, which is currently equal to the assigned namespace id; will throw an
     *         {@link IllegalStateException} if the namespace id is not available yet
     */
    String getDestinationIdPrefix();

    /**
     * @return whether the session in state {@link UplinkSessionState#ACTIVE}, i.e. ready to send and receive messages
     */
    boolean isActive();

    /**
     * Requests to close the local end of this session.
     * <p>
     * If the session is {@link UplinkSessionState#ACTIVE}, this will typically cause it to enter state
     * {@link UplinkSessionState#PARTIALLY_CLOSED_BY_LOCAL}; if it is {@link UplinkSessionState#PARTIALLY_CLOSED_BY_REMOTE}, this will
     * typically cause it to enter state {@link UplinkSessionState#FULLY_CLOSED}.
     */
    void close();

    /**
     * Reports an event that is equivalent to the remote side closing the connection, including connection errors.
     * <p>
     * If the session is {@link UplinkSessionState#ACTIVE}, this will typically cause it to enter state
     * {@link UplinkSessionState#PARTIALLY_CLOSED_BY_REMOTE}; if it is {@link UplinkSessionState#PARTIALLY_CLOSED_BY_LOCAL}, this will
     * typically cause it to enter state {@link UplinkSessionState#FULLY_CLOSED}.
     */
    void markAsCloseRequestedByRemoteEvent();

}
