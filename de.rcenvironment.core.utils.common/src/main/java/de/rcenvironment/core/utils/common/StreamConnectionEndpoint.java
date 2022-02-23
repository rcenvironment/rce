/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents one of the ends of a stream-based connection, e.g. the "client" or "server" end of a TCP connection. Introduced to address the
 * need for additional operations, most importantly closing the connection, which cannot be properly represented by closing the input and/or
 * output stream(s).
 *
 * @author Robert Mischke
 */
public interface StreamConnectionEndpoint {

    InputStream getInputStream();

    OutputStream getOutputStream();

    /**
     * Closes the associated low-level connection from the local side. For a client, this typically means closing an outgoing connection.
     * For a server, this may represent different actions, e.g. closing an incoming connection, closing a server port, or terminating some
     * other kind of server-side representation.
     * 
     * This method should not send any application-level goodbye message; this should have already happened before calling this method.
     * 
     * Any implementation of this method SHOULD take care to flush its output stream before terminating the connection, if possible. In most
     * cases, this is implicitly covered by closing the output stream before (or as part of) closing the underlying connection or session.
     * 
     * Implementations of this method MAY synchronize on the output stream to prevent any race conditions with code that asynchronously
     * writes to the output stream. Conversely, any code that writes to this stream and expects concurrent closing MAY choose to synchronize
     * on the output stream to guard against this. Such code, however, must take care to release this synchronization monitor in a way that
     * rules out deadlocks with this method.
     * 
     * There are no exceptions thrown in normal operation, as this method is supposed to perform the best action possible, and then
     * terminate cleanly either way. For example, if the underlying connection has already failed, an "unclean" termination is not an
     * abnormal situation. If it becomes necessary for any caller to detect this difference, a boolean "normal shutdown" result value could
     * be added in the future. In general, however, it is preferable to handle this on the application protocol level.
     */
    void close();
}
