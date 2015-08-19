/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper service around the OSGi {@link LogReaderService}, which methods have not 
 * serializable return values and/or arguments. But this is required to access it from remote.
 * 
 * @author Doreen Seider
 */
public interface SerializableLogReaderService extends Serializable {

    /**
     * Subscribes to {@link LogEntry} objects.
     * 
     * This method registers a {@link LogListener} object with a {@link LogReaderService} of the
     * given platform. The LogListener.logged(LogEntry) method will be called for each
     * {@link LogEntry} object placed into the log.
     * 
     * @param listener The {@link SerializableLogListener} object to register.
     * 
     * @see LogReaderService#addLogListener(LogListener).
     */
    void addLogListener(SerializableLogListener listener);

    /**
     * Returns an {@link Enumeration} of all {@link LogEntry} objects in the log.
     * 
     * Each element of the enumeration is a {@link LogEntry} object, ordered with the most recent
     * entry first. Whether the enumeration is of all {@link LogEntry} objects since the
     * {@link LogService} was started or some recent past is implementation-specific. Also
     * implementation-specific is whether informational and debug {@link LogEntry} objects are
     * included in the enumeration.
     * 
     * @return The {@link List} of {@link SerializableLogEntry} objects.
     * 
     * @see LogReaderService#getLog().
     */
    List<SerializableLogEntry> getLog();

    /**
     * Unsubscribes to LogEntry objects.
     * 
     * This method unregisters a LogListener object from the Log Reader Service.
     * 
     * @param listener The {@link SerializableLogListener} object to unregister.
     * 
     * @see LogReaderService#removeLogListener().
     */
    void removeLogListener(SerializableLogListener listener);
}
