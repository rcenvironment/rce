/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.io.IOException;
import java.util.function.Consumer;

import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;

/**
 * An interface representing a low-level connection to an uplink server, typically corresponding to a TCP connection.
 *
 * @author Robert Mischke
 */
public interface UplinkConnection extends StreamConnectionEndpoint {

    /**
     * Opens an uplink connection. Note that depending on the underlying transport mechanism, this may already require an actual network
     * connection to exist, ie no new connection may be established here.
     * 
     * @param errorConsumer a receiver for any error messages; typically, any message appearing here signals a fatal connection error
     * 
     * @throws IOException on connection failure for technical reasons, e.g. a breakdown of the underlying connection
     */
    void open(Consumer<String> errorConsumer) throws IOException;

}
