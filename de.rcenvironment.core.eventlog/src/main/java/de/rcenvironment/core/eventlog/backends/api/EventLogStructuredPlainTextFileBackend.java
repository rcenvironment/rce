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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import de.rcenvironment.core.eventlog.api.EventType;
import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;

/**
 * An {@link EventLogBackend} that writes events as structured blocks of events, with one attribute key-value pair per line with fixed
 * indentation. It is intended to be as human-readable as possible.
 *
 * @author Robert Mischke
 */
public class EventLogStructuredPlainTextFileBackend extends EventLogBackend {

    private static final String ATTTRIBUTE_LINE_FIXED_INDENT = "    ";

    // should match the "max_attribute_title_length" property in the YAML specification;
    // TODO include this in code generation?
    private static final int MAX_ATTRIBUTE_ID_LENGTH = 24;

    private static final String ATTRIBUTE_KEY_PADDING_STRING = "                        ";

    private static final String APPLICATION_START_SEPARATOR_LINE =
        "------------------------------------------------------------------------------";

    private final BufferedWriter writer;

    /**
     * A special attribute value for visually separating application runs/sessions.
     */

    public EventLogStructuredPlainTextFileBackend(Path filePath) throws IOException {
        writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (ATTRIBUTE_KEY_PADDING_STRING.length() < MAX_ATTRIBUTE_ID_LENGTH) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void append(EventLogEntryImpl logEntry) throws IOException {

        // note: using the platform-specific newline as this format is intended for human viewing

        final EventType eventType = logEntry.getEventType();

        // special rule: at application start, log a separator line before the event
        if (eventType == EventType.APPLICATION_STARTING) {
            writer.append(EventLogStructuredPlainTextFileBackend.APPLICATION_START_SEPARATOR_LINE);
            writer.newLine();
            writer.newLine();
        }

        writer.append(logEntry.getEventTypeTitle());
        writer.newLine();

        final String timestamp = logEntry.getHumanReadableLocalTimestamp();
        writer.append(generateAttributeLine("Event time", timestamp.replace('T', ' ')));
        writer.newLine();

        Map<String, String> attributeData = logEntry.getAttributeData();
        if (eventType == EventType.CUSTOM) {
            // apply stable ordering to custom (i.e., unknown) keys
            attributeData = new TreeMap<>(attributeData);
            for (Entry<String, String> e : attributeData.entrySet()) {
                // as no title data is available, always use the raw custom keys as titles
                writer.append(generateAttributeLine(e.getKey(), e.getValue()));
                writer.newLine();
            }
        } else {
            // for predefined event types, use the attribute ordering as defined in the YAML specification
            for (String key : eventType.getAttributeKeys()) {
                String value = attributeData.get(key);
                // check for null to handle absent keys and null data the same
                if (value != null) {
                    Optional<String> optionalTitle = eventType.getAttributeTitle(key);
                    String effectiveTitle = optionalTitle.orElse(key); // fall back to raw key
                    writer.append(generateAttributeLine(effectiveTitle, value));
                    writer.newLine();
                }
            }
        }

        writer.newLine();

        // TODO optimize this with delayed flushing; quite inefficient once more events get logged
        writer.flush();
    }

    private String generateAttributeLine(final String title, final String value) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ATTTRIBUTE_LINE_FIXED_INDENT);
        // the length of this string also determines the "gap" between the padded keys and the values
        buffer.append(title);
        buffer.append(":  ");
        // compensate for different keys lengths, but put the padding behind the separator;
        // overly long keys will shift the value to the right, but still maintain the key-value gap
        int paddingLength = Math.max(MAX_ATTRIBUTE_ID_LENGTH - title.length(), 0);
        buffer.append(ATTRIBUTE_KEY_PADDING_STRING.substring(0, paddingLength));
        // do not allow attribute values, which may be chosen by other users, to break lines and break the log structure
        // TODO add test for this behavior
        String sanitizedValue = value.replace("\n", "\\n").replace("\r", "\\r");
        buffer.append(sanitizedValue);
        return buffer.toString();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
