/*
 * Copyright 2006-2020 DLR, Germany
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.utils.common.AuditLog.AuditLogBackend;

/**
 * Unit tests for {@link AuditLog}.
 *
 * @author Robert Mischke
 */
public class AuditLogTest {

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

        // verify format: local ISO time with milliseconds, no timezone; for now, do not enforce dot vs. comma separator
        assertTrue(timestamp.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[\\.,]\\d{3}$"));

        assertEquals(capture.get(0)[2], "{}");

        capture.clear();

        // empty data
        log.appendInternal("test", new HashMap<>());
        assertEquals(capture.get(0)[2], "{}");

    }

}
