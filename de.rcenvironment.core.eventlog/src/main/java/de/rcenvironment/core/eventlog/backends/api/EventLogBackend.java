/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.backends.api;

import java.io.Closeable;
import java.io.IOException;

import de.rcenvironment.core.eventlog.api.EventLog;
import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;

/**
 * Represents a processing/logging backend for event data. Typical output targets are files, databases, or remote network receivers.
 *
 * @author Robert Mischke
 */
public abstract class EventLogBackend implements Closeable {

    /**
     * Appends an event's rendered string data provided by {@link EventLog} to the specific storage.
     * 
     * @param logEntry the log entry instance
     * @throws IOException on write errors
     */
    public abstract void append(EventLogEntryImpl logEntry) throws IOException;

}
