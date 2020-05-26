/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.api;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The service for providers of network connections (e.g., an embedded SSH server) to create uplink relay (i.e., server) sessions on top of
 * input and output streams of incoming network connections.
 *
 * @author Robert Mischke
 */
public interface ServerSideUplinkSessionService {

    /**
     * Creates an uplink relay (i.e., server) session handler for the given input and output streams, which are typically provided by
     * incoming network connections. When the server side considers the session terminated, it will close the provided {@link OutputStream}.
     * Likewise, closing the provided {@link InputStream} from the outside will cause the session to terminate.
     * <p>
     * Note that this session is typically created immediately once a low-level connection to the server has been created, e.g. on a
     * successful SSH login. The client-server handshake and associated checks (potentially including further authorization) are performed
     * <em>within</em> {@link ServerSideUplinkSession#runSession()}, so the session may never become active in the first place.
     * 
     * @param clientInformationString an arbitrary string describing the client's identity; used for log output, and potentially error
     *        messages
     * @param loginAccountName the login name, which will be used in combination with the "session qualifier"/"client id" to define a stable
     *        client identity
     * @param inputStream the {@link InputStream} providing incoming data
     * @param outputStream the {@link OutputStream} to send outgoing data to
     * @return the session object/handle
     */
    ServerSideUplinkSession createServerSideSession(String clientInformationString, String loginAccountName, InputStream inputStream,
        OutputStream outputStream);

}
