/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;

import org.junit.Test;

public class EventLogEntryImplTest {

    // ISO-8601 with three millisecond digits; these are always attached with a dot, regardless of locale
    private static final Pattern HUMAN_READABLE_TIMESTAMP_VERIFICATION_REGEXP =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * Verifies that:
     * <li>the overall timestamp format looks as expected (ISO-8601 with three millisecond digits),
     * <li>in particular, milliseconds are separated by dot, regardless of locale,
     * <li>and that a zero millisecond part is not omitted, but rendered as "000" (regression test for #0017428)
     */
    @Test
    public void timestampFormatting() {
        Instant testInstant;

        // adust the millisecond part so that its representation should be ".100"
        testInstant = Instant.now().with(ChronoField.MILLI_OF_SECOND, 100);

        String timestamp = EventLogEntryImpl.generateHumanReadableLocalTimestamp(testInstant);

        // verify format: local ISO time with milliseconds, no timezone; milliseconds present, separated with a dot
        assertTrue(timestamp, HUMAN_READABLE_TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());
        assertTrue(timestamp, timestamp.endsWith(".100"));

        // set the millisecond part to zero
        testInstant = Instant.now().with(ChronoField.MILLI_OF_SECOND, 0);
        timestamp = EventLogEntryImpl.generateHumanReadableLocalTimestamp(testInstant);

        assertTrue(timestamp, HUMAN_READABLE_TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());
        assertTrue(timestamp, timestamp.endsWith(".000"));
    }

}
