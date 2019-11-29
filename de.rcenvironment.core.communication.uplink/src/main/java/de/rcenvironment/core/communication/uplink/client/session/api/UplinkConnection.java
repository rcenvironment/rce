/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * An interface representing a low-level connection to an uplink server, typically corresponding to a TCP connection.
 *
 * @author Robert Mischke
 */
public interface UplinkConnection {

    /**
     * Opens an uplink connection. Note that depending on the underlying transport mechanism, this may already require an actual network
     * connection to exist, ie no new connection may be established here.
     * 
     * @param incomingStreamConsumer a receiver for the stream that server-to-client data will be received from
     * @param errorConsumer a receiver for any error messages; typically, any message appearing here signals a fatal connection error
     * 
     * @return the {@link OutputStream} to write client-to-server data to
     * 
     * @throws IOException on connection failure for technical reasons, e.g. a breakdown of the underlying connection
     */
    OutputStream open(Consumer<InputStream> incomingStreamConsumer, Consumer<String> errorConsumer) throws IOException;

    /**
     * Closes the uplink connection; this should be typically done before closing any underlying network connection. There is no harm in
     * always trying to close this connection even if an error occurred before, or when it may have already been closed.
     */
    void close();
}
