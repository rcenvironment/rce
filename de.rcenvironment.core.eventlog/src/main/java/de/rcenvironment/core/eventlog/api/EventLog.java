/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.api;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.eventlog.backends.api.EventLogBackend;
import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;
import de.rcenvironment.core.eventlog.internal.EventLogFallbackBackend;
import de.rcenvironment.core.eventlog.internal.EventLogNOPBackend;

/**
 * Provides a central point for logging semantic event information to a file or other backends.
 *
 * @author Robert Mischke
 */
public final class EventLog {

    private static EventLog sharedInstance = new EventLog(new EventLogFallbackBackend());

    private final EventLogBackend[] backends;

    // protected access for unit tests
    protected EventLog(EventLogBackend... backends) {
        this.backends = backends;
    }

    /**
     * Initializes the singleton instance with one or more provided storage backends.
     * 
     * @param backends the storage backends to use
     */
    public static synchronized void initialize(EventLogBackend... backends) {
        // only allow replacing the default instance
        if (sharedInstance != null && !(sharedInstance.backends[0] instanceof EventLogFallbackBackend)) {
            throw new IllegalStateException("Already initialized with custom backend");
        }
        if (backends.length == 0) {
            throw new IllegalStateException("At least one backend is required");
        }
        sharedInstance = new EventLog(backends);
    }

    @Deprecated
    public static synchronized void disableForIntegrationTesting() {
        sharedInstance = new EventLog(new EventLogNOPBackend());
    }

    public static EventLogEntry newEntry(EventType eventType) {
        return new EventLogEntryImpl(eventType);
    }

    /**
     * Adds a log entry.
     * 
     * @param entry a {@link EventLogEntry} object containing the event id and optionally, arbitrary string key-value data to add to the
     *        event entry
     */
    public static synchronized void append(EventLogEntry entry) {
        if (sharedInstance == null) {
            throw new IllegalStateException("Not initialized");
        }
        sharedInstance.appendInternal(entry);
    }

    /**
     * Adds a log entry.
     * 
     * @param eventType the event type to log
     * @param data a map of arbitrary string data to add to the event entry; can be empty or null (both are equivalent)
     */
    public static synchronized void append(EventType eventType, Map<String, String> data) {
        if (sharedInstance == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (data == null) {
            data = new HashMap<>();
        }
        sharedInstance.appendInternal(new EventLogEntryImpl(Instant.now(), eventType, data));
    }

    /**
     * Convenience variant of {@link #append(String, Map)} for event entries with a single key-value pair as data.
     * 
     * @param eventType the event type enum
     * @param key the data key
     * @param value the data value
     */
    public static synchronized void append(EventType eventType, String key, String value) {
        Map<String, String> data = new HashMap<>();
        data.put(key, value);
        append(eventType, data);
    }

    /**
     * Shuts down the backend. Subsequent {@link #append()} calls will cause an {@link IllegalStateException}.
     */
    public static synchronized void close() {
        if (sharedInstance == null) {
            getStandardLog().warn("Event log requested to close without being initialized first; ignoring call");
            return;
        }
        for (EventLogBackend backend : sharedInstance.backends) {
            try {
                backend.close();
            } catch (IOException e) {
                getStandardLog().error("Error closing event log backend: " + e.toString());
            }
        }
        sharedInstance = null;
    }

    // protected access for unit tests
    protected void appendInternal(EventLogEntry logEntry) {
        // TODO make logging asynchronous and parallelized
        for (EventLogBackend backend : backends) {
            try {
                backend.append((EventLogEntryImpl) logEntry);
            } catch (IOException e) {
                getStandardLog().error("Error writing to event log: " + e.toString());
            }
        }
    }

    private static Log getStandardLog() {
        return LogFactory.getLog(EventLog.class);
    }

}
