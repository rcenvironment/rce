/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.backends.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.eventlog.api.EventType;
import de.rcenvironment.core.eventlog.internal.EventLogEntryImpl;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

public class EventLogCompactJsonFileBackendTest {

    /**
     * Tests {@link EventLogCompactJsonFileBackend}.
     * 
     * @throws IOException on unexpected errors
     */
    @Test
    public void compactJsonFileBackend() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();

        File testFileDir = TempFileServiceAccess.getInstance().createManagedTempDir();
        Path testFile = new File(testFileDir, "event.log").toPath();
        assertFalse(Files.exists(testFile));

        EventLogCompactJsonFileBackend backend = new EventLogCompactJsonFileBackend(testFile);
        assertTrue(Files.exists(testFile));

        assertEquals(0, Files.readAllLines(testFile).size());

        backend.append(new EventLogEntryImpl(Instant.ofEpochMilli(1000L), EventType.CUSTOM, new HashMap<String, String>()));
        EventLogEntryImpl event2 = new EventLogEntryImpl(Instant.ofEpochMilli(2000L), EventType.CUSTOM, new HashMap<String, String>());
        event2.set("testKey", "testVal");
        backend.append(event2);
        // TODO also test reading after delayed flushing
        backend.close();
        // test that separate lines were written
        List<String> lines = Files.readAllLines(testFile);
        assertEquals(2, lines.size());
        // check for output formatting and field/line mixups
        assertEquals(lines.get(0), "{\"_t\":\"custom\",\"_ts\":\"1000\"}");
        assertEquals(lines.get(1), "{\"_t\":\"custom\",\"_ts\":\"2000\",\"testKey\":\"testVal\"}");

        // test that an existing file is appended to, not overwritten
        backend = new EventLogCompactJsonFileBackend(testFile);
        backend.append(new EventLogEntryImpl(Instant.ofEpochMilli(3000L), EventType.CUSTOM, new HashMap<String, String>()));
        backend.close();

        lines = Files.readAllLines(testFile);
        assertEquals(3, lines.size());
        assertEquals(lines.get(2), "{\"_t\":\"custom\",\"_ts\":\"3000\"}");

        // cleanup so the test directory can be removed
        Files.delete(testFile);
    }

}
