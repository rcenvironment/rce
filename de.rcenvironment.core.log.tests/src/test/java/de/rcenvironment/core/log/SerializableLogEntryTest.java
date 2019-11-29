/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Test cases for {@link SerializableLogEntry}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class SerializableLogEntryTest {

    /** Test. */
    @Test
    public void test() {
        final String name = "ernie";
        final int level = 7;
        final String message = "sesamstrasse";
        final long time = 11;
        final String exception = new Exception().toString();

        SerializableLogEntry entry = new SerializableLogEntry(name, level, message, time, exception.toString());

        assertEquals(name, entry.getBundleName());
        assertEquals(level, entry.getLevel());
        assertEquals(message, entry.getMessage());
        assertEquals(time, entry.getTime());
        assertEquals(exception, entry.getException());

        InstanceNodeSessionId pi = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("dummy");
        entry.setPlatformIdentifer(pi);
        assertEquals(pi, entry.getPlatformIdentifer());

        entry.toString();
        new SerializableLogEntry(name, LogService.LOG_DEBUG, message, time, exception.toString()).toString();
        new SerializableLogEntry(name, LogService.LOG_INFO, message, time, exception.toString()).toString();
        new SerializableLogEntry(name, LogService.LOG_WARNING, message, time, exception.toString()).toString();
        new SerializableLogEntry(name, LogService.LOG_ERROR, message, time, exception.toString()).toString();

        final long laterTime = 43;
        SerializableLogEntry laterEntry = new SerializableLogEntry(name, level, message, laterTime, exception.toString());

        final int lower = -1;
        assertEquals(0, entry.compareTo(entry));
        assertEquals(lower, entry.compareTo(laterEntry));
        assertEquals(1, laterEntry.compareTo(entry));

        SerializableLogEntry latestEntry = new SerializableLogEntry(name, level, message, laterTime, null);

        laterEntry.setPlatformIdentifer(pi);
        latestEntry.setPlatformIdentifer(pi);
        latestEntry.compareTo(laterEntry);
    }
}
