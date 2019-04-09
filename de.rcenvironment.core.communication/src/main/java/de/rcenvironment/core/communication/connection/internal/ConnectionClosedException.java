/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.internal;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * An exception type for situations when a message should be sent, but the target
 * {@link MessageChannel} is already closed.
 * 
 * @author Robert Mischke
 */
public class ConnectionClosedException extends CommunicationException {

    private static final long serialVersionUID = 712167269948498160L;

    public ConnectionClosedException(String string) {
        super(string);
    }

}
