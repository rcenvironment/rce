/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * Serializable version of {@link LogListener}.
 *
 * @author Doreen Seider
 */
public interface SerializableLogListener extends CallbackObject {

    /**
     * Listener method called for each LogEntry object created.

     * As with all event listeners, this method should return to its caller as
     * soon as possible.
     * 
     * This interface extends {@link CallbackObject} to support remote subscription by simply passing an
     * object of the implementing class.
     * 
     * @param logEntry A {@link SerializableLogEntry} object containing log information.
     * @see {@link LogListener}
     */
    @CallbackMethod
    void logged(SerializableLogEntry logEntry);
}
