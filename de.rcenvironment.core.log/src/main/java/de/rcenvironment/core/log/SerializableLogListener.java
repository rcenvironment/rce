/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Serializable version of {@link LogListener}.
 * 
 * @author Doreen Seider
 */
public interface SerializableLogListener extends CallbackObject {

    /**
     * Listener method called for each LogEntry object created.
     * 
     * As with all event listeners, this method should return to its caller as soon as possible.
     * 
     * This interface extends {@link CallbackObject} to support remote subscription by simply passing an object of the implementing class.
     * 
     * @param logEntry A {@link SerializableLogEntry} object containing log information.
     * @see {@link LogListener}
     * @throws RemoteOperationException standard remote operation exception; not directly called from remote but may be thrown from proxy
     */
    @CallbackMethod
    void logged(SerializableLogEntry logEntry) throws RemoteOperationException;
}
