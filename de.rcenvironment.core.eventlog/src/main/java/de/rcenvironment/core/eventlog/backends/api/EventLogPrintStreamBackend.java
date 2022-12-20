/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.backends.api;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;

/**
 * A simple event log backend to write events to a provided PrintStream, typically stdout/stderr. Whether the stream should be closed on
 * {@link #close()} is configurable.
 * 
 * TODO 10.4.0 (p2): this backend exists for historical reasons, and is currently used for console output; clarify its exact use case and
 * output format, or replace it with other backends
 *
 * @author Robert Mischke
 */
public class EventLogPrintStreamBackend extends EventLogBackend {

    private static final String FIELD_SEPARATOR = "   ";

    private static final String EVENT_ID_PADDING_LENGTH = "30";

    private final Writer writer;

    private boolean closeStream;

    public EventLogPrintStreamBackend(PrintStream printStream, boolean closeStream) {
        this.writer = new PrintWriter(printStream);
        this.closeStream = closeStream;
    }

    @Override
    public void close() throws IOException {
        if (closeStream) {
            writer.close();
        }
    }

    @Override
    public void append(EventLogEntryImpl logEntry) throws IOException {
        // TODO
        writer.write(String.format("%s" + FIELD_SEPARATOR + "%-" + EVENT_ID_PADDING_LENGTH + "s"
            + FIELD_SEPARATOR + "%s%s", logEntry.getHumanReadableLocalTimestamp(), logEntry.getEventTypeId(),
            logEntry.getAttributeDataAsConpactJsonString(), System.lineSeparator()));

        // TODO optimize this with delayed flushing; quite inefficient once more events get logged
        writer.flush();
    }

}
