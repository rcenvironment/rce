/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

import de.rcenvironment.core.utils.common.AuditLog.AuditLogBackend;

/**
 * A simple event log backend to write events to a provided PrintStream, typically stdout/stderr. Whether the stream should be closed on
 * {@link #close()} is configurable.
 *
 * @author Robert Mischke
 */
public class EventLogPrintStreamBackend extends AuditLogBackend {

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
    public void append(String timestamp, String eventId, String data) throws IOException {
        writer.write(String.format("%s" + FIELD_SEPARATOR + "%-" + EVENT_ID_PADDING_LENGTH + "s"
            + FIELD_SEPARATOR + "%s%s", timestamp, eventId, data, System.lineSeparator()));

        // TODO optimize this with delayed flushing; quite inefficient once more events get logged
        writer.flush();
    }

}
