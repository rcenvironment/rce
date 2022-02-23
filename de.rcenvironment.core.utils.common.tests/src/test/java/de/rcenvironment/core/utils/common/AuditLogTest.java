/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import de.rcenvironment.core.utils.common.AuditLog.AuditLogBackend;

/**
 * Unit tests for {@link AuditLog}.
 *
 * @author Robert Mischke
 */
public class AuditLogTest {

    // ISO-8601 with three millisecond digits; these are always attached with a dot, regardless of locale
    private static final Pattern TIMESTAMP_VERIFICATION_REGEXP = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * Test backend that captures the output.
     *
     * @author Robert Mischke
     */
    private class TestBackend extends AuditLogBackend {

        public final List<String[]> capture = new ArrayList<>();

        @Override
        public void append(String timstamp, String eventId, String data) {
            capture.add(new String[] { timstamp, eventId, data });
        }

        @Override
        public void close() throws IOException {}

    }

    /**
     * Tests {@link AuditLogFileBackend}.
     * 
     * @throws IOException on unexpected errors
     */
    @Test
    public void fileBackend() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        File testFileDir = TempFileServiceAccess.getInstance().createManagedTempDir();
        Path testFile = new File(testFileDir, "audit.log").toPath();
        assertFalse(Files.exists(testFile));
        AuditLogFileBackend backend = new AuditLogFileBackend(testFile);
        assertTrue(Files.exists(testFile));
        assertEquals(0, Files.readAllLines(testFile).size());
        backend.append("a", "b", "c");
        backend.append("d", "e", "f");
        // TODO also test reading after delayed flushing
        backend.close();
        // test that separate lines were written
        List<String> lines = Files.readAllLines(testFile);
        assertEquals(2, lines.size());
        // check for field mixups
        String firstLine = lines.get(0);
        assertTrue(firstLine, firstLine.contains("a"));
        assertTrue(firstLine, firstLine.contains("b"));
        assertTrue(firstLine, firstLine.contains("c"));

        // test that an existing file is appended to, not overwritten
        backend = new AuditLogFileBackend(testFile);
        backend.append("g", "h", "i");
        backend.close();
        assertEquals(3, Files.readAllLines(testFile).size());

        // cleanup so the test directory can be removed
        Files.delete(testFile);
    }

    /**
     * Tests basic functionality.
     */
    @Test
    public void basicOperation() {
        TestBackend backend = new TestBackend();
        List<String[]> capture = backend.capture;
        AuditLog log = new AuditLog(backend);

        // null data
        log.appendInternal("test", null);
        String timestamp = capture.get(0)[0];

        // verify timestamp length constant and format
        assertEquals(AuditLog.EXPECTED_TIMESTAMP_LENGTH, timestamp.length());
        assertTrue(timestamp, TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());

        assertEquals(capture.get(0)[2], "{}");

        capture.clear();

        // empty data
        log.appendInternal("test", new HashMap<>());
        assertEquals(capture.get(0)[2], "{}");
    }

    /**
     * Verifies that:
     * <li>the overall timestamp format looks as expected (ISO-8601 with three millisecond digits),
     * <li>in particular, milliseconds are separated by dot, regardless of locale,
     * <li>and that a zero millisecond part is not omitted, but rendered as "000" (regression test for #0017428)
     */
    @Test
    public void timestampFormatting() {
        LocalDateTime time = LocalDateTime.now();
        // adust the millisecond part so that its representation should be ".100"
        time = time.with(ChronoField.MILLI_OF_SECOND, 100);
        String timestamp = new AuditLog(null).formatTimestamp(time);

        // verify format: local ISO time with milliseconds, no timezone; milliseconds present, separated with a dot
        assertTrue(timestamp, TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());
        assertTrue(timestamp, timestamp.endsWith(".100"));

        // set the millisecond part to zero
        time = time.with(ChronoField.MILLI_OF_SECOND, 0);
        timestamp = new AuditLog(null).formatTimestamp(time);

        assertTrue(timestamp, TIMESTAMP_VERIFICATION_REGEXP.matcher(timestamp).matches());
        assertTrue(timestamp, timestamp.endsWith(".000"));
    }

}
