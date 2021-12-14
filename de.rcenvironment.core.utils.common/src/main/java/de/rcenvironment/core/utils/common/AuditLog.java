/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides a central point for logging semantic event information to a file or other backends.
 *
 * @author Robert Mischke
 */
public final class AuditLog {

    protected static final int EXPECTED_TIMESTAMP_LENGTH = 23;

    private static AuditLog sharedInstance = new AuditLog(new FallbackLoggingBackend());

    private final AuditLogBackend backend;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    // "immutable and thread-safe", according to JavaDoc
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");

    /**
     * Convenience and type safety wrapper for constructing new log entries.
     *
     * @author Robert Mischke
     */
    public static final class LogEntry {

        private final String eventId;

        private final Map<String, String> data = new TreeMap<>(); // sort entries by key

        public LogEntry(String eventId) {
            this.eventId = eventId;
        }

        public LogEntry set(String key, String value) {
            data.put(key, value);
            return this;
        }

        /**
         * Convenience method for adding long values. Note that these are (currently) not logged as "native" JSON integers, but converted to
         * string values.
         * 
         * @param key the string key
         * @param value the long value to be converted to and used as a string value
         * 
         * @return this instance (for command chaining)
         */
        public LogEntry set(String key, long value) {
            set(key, Long.toString(value));
            return this;
        }

        /**
         * Convenience method for adding integer values. Note that these are (currently) not logged as "native" JSON integers, but converted
         * to string values.
         * 
         * @param key the string key
         * @param value the integer value to be converted to and used as a string value
         * 
         * @return this instance (for command chaining)
         */
        public LogEntry set(String key, int value) {
            set(key, Integer.toString(value));
            return this;
        }

        /**
         * Convenience method for adding a map of values. All provided entries are added as they are; existing entries with matching keys
         * are silently replaced.
         * 
         * @param dataMap the map of entries to add to the log entry
         * 
         * @return this instance (for command chaining)
         */
        public LogEntry setAll(Map<String, String> dataMap) {
            data.putAll(dataMap);
            return this;
        }

    }

    /**
     * Represents a storage backend for event data. Typical options are files, databases, or remote network receivers.
     *
     * @author Robert Mischke
     */
    public abstract static class AuditLogBackend implements Closeable {

        /**
         * Appends an event's rendered string data provided by {@link AuditLog} to the specific storage.
         * 
         * @param timestamp the timestamp string
         * @param eventId the event id string
         * @param data the data string
         * @throws IOException on write errors
         */
        public abstract void append(String timestamp, String eventId, String data) throws IOException;

    }

    /**
     * A backend that logs all events as warnings. This is set as the default backend to log any events that are triggered at runtime before
     * initialization, as these would indicate a startup ordering problem. During unit/integration testing, this is typically left in as the
     * default backend; therefore, all events triggered during tests will be logged by this backend, and can be safely ignored.
     * 
     * @author Robert Mischke
     */
    private static final class FallbackLoggingBackend extends AuditLogBackend {

        @Override
        public void close() throws IOException {}

        @Override
        public void append(String timestamp, String eventId, String data) throws IOException {
            LogFactory.getLog(AuditLog.class).warn(StringUtils.format("Received an event while no log receiver is configured; "
                + "this is normal during unit/integration testing. Data: %s, %s, %s", timestamp, eventId, data));
        }
    }

    /**
     * A no-operation backend to disable logging, typically during unit/integration testing.
     * 
     * @author Robert Mischke
     */
    private static final class NOPLogBackend extends AuditLogBackend {

        @Override
        public void close() throws IOException {}

        @Override
        public void append(String timestamp, String eventId, String data) throws IOException {}
    }

    // protected access for unit tests
    protected AuditLog(AuditLogBackend backend) {
        this.backend = backend;
    }

    /**
     * Initializes the singleton instance with the provided storage backend.
     * 
     * @param backend the storage backend to use; see {@link AuditLogFileBackend} for a simple implementation
     */
    public static synchronized void initialize(AuditLogBackend backend) {
        // only allow replacing the default instance
        if (sharedInstance != null && !(sharedInstance.backend instanceof FallbackLoggingBackend)) {
            throw new IllegalStateException("Already initialized with custom backend");
        }
        sharedInstance = new AuditLog(backend);
    }

    @Deprecated
    public static synchronized void disableForIntegrationTesting() {
        sharedInstance = new AuditLog(new NOPLogBackend());
    }

    public static LogEntry newEntry(String eventId) {
        return new LogEntry(eventId);
    }

    /**
     * Adds a log entry.
     * 
     * @param entry a {@link LogEntry} object containing the event id and optinally, arbitrary string key-value data to add to the event
     *        entry
     */
    public static synchronized void append(LogEntry entry) {
        if (sharedInstance == null) {
            throw new IllegalStateException("Not initialized");
        }
        sharedInstance.appendInternal(entry.eventId, entry.data);
    }

    /**
     * Adds a log entry.
     * 
     * @param eventId the event id to log
     * @param data a map of arbitrary string data to add to the event entry; can be empty or null (both are equivalent)
     */
    public static synchronized void append(String eventId, Map<String, String> data) {
        if (sharedInstance == null) {
            throw new IllegalStateException("Not initialized");
        }
        sharedInstance.appendInternal(eventId, data);
    }

    /**
     * Convenience variant of {@link #append(String, Map)} for event entries with a single key-value pair as data.
     * 
     * @param eventId the event id
     * @param key the data key
     * @param value the data value
     */
    public static synchronized void append(String eventId, String key, String value) {
        Map<String, String> data = new HashMap<>();
        data.put(key, value);
        append(eventId, data);
    }

    /**
     * Shuts down the backend. Subsequent {@link #append()} calls will cause an {@link IllegalStateException}.
     */
    public static synchronized void close() {
        if (sharedInstance == null) {
            getStandardLog().warn("Audit log requested to close without being initialized first; ignoring call");
            return;
        }
        try {
            sharedInstance.backend.close();
        } catch (IOException e) {
            getStandardLog().error("Error closing audit log backend: " + e.toString());
        }
        sharedInstance = null;
    }

    // protected access for unit tests
    protected void appendInternal(String eventId, Map<String, String> data) {
        LocalDateTime time = LocalDateTime.now();
        final String timestamp = formatTimestamp(time);

        String dataString;
        if (data == null || data.isEmpty()) {
            dataString = "{}";
        } else {
            try {
                dataString = jsonMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                // fallback in unlikely case of JSON encoding error
                dataString = "##JSON Write error:" + e.toString() + "##" + data.toString();
            }
        }

        try {
            backend.append(timestamp, eventId, dataString);
        } catch (IOException e) {
            getStandardLog().error("Error writing to audit log: " + e.toString());
        }
    }

    // protected access for unit tests
    protected String formatTimestamp(LocalDateTime time) {
        String formatted = time.format(timestampFormatter);
        int len = formatted.length();
        if (len == EXPECTED_TIMESTAMP_LENGTH) {
            return formatted;
        } else {
            throw new IllegalStateException(Integer.toString(len)); // sanity check failed
        }
    }

    private static Log getStandardLog() {
        return LogFactory.getLog(AuditLog.class);
    }

}
