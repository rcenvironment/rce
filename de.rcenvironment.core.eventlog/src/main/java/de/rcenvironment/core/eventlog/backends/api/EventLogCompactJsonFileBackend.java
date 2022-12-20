/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.backends.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeMap;

import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;

/**
 * An {@link EventLogBackend} that writes events as single JSON lines without unnecessary whitespace to a file. Event type ids and
 * timestamps are represented as JSON fields with the {@link #JSON_KEY_EVENT_TYPE_ID} and {@link #JSON_KEY_MSEC_EPOCH_TIMESTAMP} keys.
 * 
 * The output is also intended to be as predictable as possible to support automated application testing. Therefore, the JSON fields are
 * printed in a stable order; currently, according to Java String comparison of their keys.
 *
 * @author Robert Mischke
 */
public class EventLogCompactJsonFileBackend extends EventLogBackend {

    private static final String JSON_KEY_EVENT_TYPE_ID = "_t";

    private static final String JSON_KEY_MSEC_EPOCH_TIMESTAMP = "_ts";

    private final BufferedWriter writer;

    public EventLogCompactJsonFileBackend(Path filePath) throws IOException {
        writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void append(EventLogEntryImpl logEntry) throws IOException {

        Map<String, String> orderedFields = new TreeMap<String, String>();
        orderedFields.put(JSON_KEY_EVENT_TYPE_ID, logEntry.getEventTypeId());
        orderedFields.put(JSON_KEY_MSEC_EPOCH_TIMESTAMP, Long.toString(logEntry.getTimestamp().toEpochMilli()));

        orderedFields.putAll(logEntry.getAttributeData());

        String jsonString = EventLogEntryImpl.generateCompactJsonString(orderedFields);

        writer.append(jsonString);

        // use the platform-specific newline
        writer.newLine();

        // TODO optimize this with delayed flushing; quite inefficient once more events get logged
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
