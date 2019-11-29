/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.api;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Represents the main relay backend that the individual {@link ServerSideUplinkSession}s communicate with.
 *
 * @author Robert Mischke
 */
public interface ServerSideUplinkEndpointService {

    /**
     * Registers an active session and assigns a log handle.
     * 
     * @param session the starting session
     * @return the log handle to assign
     */
    String assignSessionId(ServerSideUplinkSession session);

    /**
     * Processes a {@link MessageBlock} received by a {@link ServerSideUplinkSession}.
     * 
     * @param session the receiving session
     * @param channelId the id of the virtual channel that the MessageBlock was marked for
     * @param message the received {@link MessageBlock}
     * @throws ProtocolException on errors during processing
     */
    void onMessageBlock(ServerSideUplinkSession session, long channelId, MessageBlock message) throws ProtocolException;

    /**
     * Sets whether the given session should be considered "active", e.g. whether it should receive forwarded updates from other sessions.
     * When a session becomes inactive, all of its published tool updates are automatically removed, and removal updates sent to all other
     * sessions.
     * 
     * @param session the session
     * @param active the new "active" state - typically set to "true" after handshake completion, and to "false" on session shutdown or
     *        breakdown
     */
    void setSessionActiveState(ServerSideUplinkSession session, boolean active);

    /**
     * Reserves a namespace to prevent multiple sessions using the same namespace, which could lead to destination id collisions and other
     * undefined behavior.
     * 
     * @param namespaceId the id of the namespace to reserve
     * @param session the session to reserve the namespace for
     * @return true on success; false if the namespace is already reserved by another session
     */
    boolean attemptToAssignNamespaceId(String namespaceId, ServerSideUplinkSession session);

    /**
     * Releases the namespace id to allow logins using it again. The session that is expected to be bound to it is provided as a safeguard
     * for consistency checks.
     * 
     * @param namespaceId the namespace id to release
     * @param session the session that should be bound to it
     */
    void releaseNamespaceId(String namespaceId, ServerSideUplinkSession session);

}
