/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote service similar to {@link LogReaderService} with serialization-safe method variants.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (7.0.0 adaptations)
 */
@RemotableService
public interface RemotableLogReaderService extends Serializable {

    /**
     * Subscribes to {@link LogEntry} objects.
     * 
     * This method registers a {@link LogListener} object with a {@link LogReaderService} of the given platform. The
     * LogListener.logged(LogEntry) method will be called for each {@link LogEntry} object placed into the log.
     * 
     * @param listener The {@link SerializableLogListener} object to register.
     * 
     * @see LogReaderService#addLogListener(LogListener).
     * @throws RemoteOperationException standard remote operation exception
     */
    void addLogListener(SerializableLogListener listener) throws RemoteOperationException;

    /**
     * Returns an {@link Enumeration} of all {@link LogEntry} objects in the log.
     * 
     * Each element of the enumeration is a {@link LogEntry} object, ordered with the most recent entry first. Whether the enumeration is of
     * all {@link LogEntry} objects since the {@link LogService} was started or some recent past is implementation-specific. Also
     * implementation-specific is whether informational and debug {@link LogEntry} objects are included in the enumeration.
     * 
     * @return The {@link List} of {@link SerializableLogEntry} objects.
     * 
     * @see LogReaderService#getLog().
     * @throws RemoteOperationException standard remote operation exception
     */
    List<SerializableLogEntry> getLog() throws RemoteOperationException;

    /**
     * Unsubscribes to LogEntry objects.
     * 
     * This method unregisters a LogListener object from the Log Reader Service.
     * 
     * @param listener The {@link SerializableLogListener} object to unregister.
     * 
     * @see LogReaderService#removeLogListener().
     * @throws RemoteOperationException standard remote operation exception
     */
    void removeLogListener(SerializableLogListener listener) throws RemoteOperationException;
}
