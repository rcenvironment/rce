/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import java.util.Enumeration;
import java.util.List;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Service that serves as an abstraction of the {@link RemotableLogReaderService}s of the whole distributed system. It provides convenient
 * access to local and remote {@link RemotableLogReaderService}s.
 * 
 * @author Doreen Seider
 */
// TODO rename
public interface DistributedLogReaderService {

    /**
     * Subscribes to LogEntry objects.
     * 
     * This method registers a {@link LogListener} object with a {@link LogReaderService} of the given platform. The
     * LogListener.logged(LogEntry) method will be called for each {@link LogEntry} object placed into the log.
     * 
     * @param logListener The {@link SerializableLogListener} object to register.
     * @param nodeId The {@link NodeIdentifier} of the platform to register.
     * 
     * @see LogReaderService#addLogListener(LogListener).
     */
    void addLogListener(SerializableLogListener logListener, NodeIdentifier nodeId);

    /**
     * Returns an {@link Enumeration} of all {@link LogEntry} objects in the log.
     * 
     * Each element of the enumeration is a {@link LogEntry} object, ordered with the most recent entry first. Whether the enumeration is of
     * all {@link LogEntry} objects since the {@link LogService} was started or some recent past is implementation-specific. Also
     * implementation-specific is whether informational and debug {@link LogEntry} objects are included in the enumeration.
     * 
     * @param nodeId The {@link NodeIdentifier} of the platform to get the log from.
     * @return The {@link List} of {@link SerializableLogEntry} objects.
     * 
     * @see LogReaderService#getLog().
     */
    List<SerializableLogEntry> getLog(NodeIdentifier nodeId);

    /**
     * Unsubscribes from LogEntry objects.
     * 
     * This method unregisters a LogListener object from the Log Reader Service.
     * 
     * @param logListener The {@link SerializableLogListener} object to unregister.
     * @param nodeId The {@link NodeIdentifier} of the platform to unregister.
     * 
     * @see LogReaderService#removeLogListener().
     */
    void removeLogListener(SerializableLogListener logListener, NodeIdentifier nodeId);
}
