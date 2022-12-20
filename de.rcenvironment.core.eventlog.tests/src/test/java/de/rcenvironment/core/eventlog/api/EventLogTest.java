/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.eventlog.backends.api.EventLogBackend;
import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;

/**
 * Unit tests for {@link EventLog}.
 *
 * @author Robert Mischke
 */
public class EventLogTest {

    // ISO-8601 with three millisecond digits; these are always attached with a dot, regardless of locale
    private static final Pattern HUMAN_READABLE_TIMESTAMP_VERIFICATION_REGEXP =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * Test backend that captures the output.
     *
     * @author Robert Mischke
     */
    private class TestBackend extends EventLogBackend {

        public final List<Object[]> capture = new ArrayList<>();

        @Override
        public void append(EventLogEntryImpl logEntry) throws IOException {
            capture.add(new Object[] { Long.toString(logEntry.getTimestamp().toEpochMilli()), logEntry.getEventTypeId(),
                logEntry.getAttributeData() });
        }

        @Override
        public void close() throws IOException {}

    }

    /**
     * Tests basic functionality.
     */
    @Test
    @Ignore
    // TODO 10.4.0 (p1): needs review after backend rework; the current test cases do not test anything significant anymore
    public void basicOperation() {
        TestBackend backend = new TestBackend();
        List<Object[]> capture = backend.capture;
        EventLog log = new EventLog(backend);

        // null data
        log.appendInternal(new EventLogEntryImpl(Instant.now(), EventType.CUSTOM, null));
        String timestamp = capture.get(0)[0].toString();

        // verify timestamp length constant and format
        assertEquals(EventLogEntryImpl.EXPECTED_HUMAN_READABLE_TIMESTAMP_LENGTH, timestamp.length());
        assertTrue(timestamp, HUMAN_READABLE_TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());

        assertEquals(capture.get(0)[2], "{}");

        capture.clear();

        // empty data
        log.appendInternal(new EventLogEntryImpl(Instant.now(), EventType.CUSTOM, new HashMap<>()));
        assertEquals(capture.get(0)[2], "{}");
    }

}
