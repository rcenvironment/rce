/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import de.rcenvironment.core.communication.uplink.client.session.internal.ClientSideUplinkSessionParameters;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * The local service for creating the session layer ({@link ClientSideUplinkSession}) on top of an established network connection.
 *
 * @author Robert Mischke
 */
public interface LocalUplinkSessionService {

    /**
     * Creates a new {@link ClientSideUplinkSession} based on an established stream-based connection.
     * 
     * @param connectionEndpoint the underlying streaming connection
     * @param sessionParameters the client-side parameters to use in the session
     * @param eventHandler the callback implementation for handling session events
     * @return the new session
     * @throws OperationFailureException on failure to establish the session, e.g. on client-server protocol mismatch
     */
    ClientSideUplinkSession createSession(StreamConnectionEndpoint connectionEndpoint, ClientSideUplinkSessionParameters sessionParameters,
        ClientSideUplinkSessionEventHandler eventHandler)
        throws OperationFailureException;
}
