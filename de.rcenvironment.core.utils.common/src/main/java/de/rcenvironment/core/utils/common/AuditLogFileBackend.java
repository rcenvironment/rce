/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.rcenvironment.core.utils.common.AuditLog.AuditLogBackend;

/**
 * A simple {@link AuditLogBackend} that writes events as single lines to a file. Event data is serialized to single-line JSON.
 *
 * @author Robert Mischke
 */
public class AuditLogFileBackend extends AuditLogBackend {

    private static final String FIELD_SEPARATOR = "   ";

    private static final String EVENT_ID_PADDING_LENGTH = "30";

    private final BufferedWriter writer;

    public AuditLogFileBackend(Path filePath) throws IOException {
        writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void append(String timestamp, String eventId, String data) throws IOException {
        writer.append(String.format("%s" + FIELD_SEPARATOR + "%-" + EVENT_ID_PADDING_LENGTH + "s"
            + FIELD_SEPARATOR + "%s", timestamp, eventId, data));

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
