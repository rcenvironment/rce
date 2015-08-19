/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;


/**
 * Test cases for {@link SerializableLogEntry}.
 *
 * @author Doreen Seider
 */
public class SerializableLogEntryTest {

    /** Test. */
    @Test
    public void test() {
        final String name = "ernie";
        final int level = 7;
        final String message = "sesamstrasse";
        final long time = 11;
        final Throwable exception = new Exception();
        
        SerializableLogEntry entry = new SerializableLogEntry(name, level, message, time, exception);
        
        assertEquals(name, entry.getBundleName());
        assertEquals(level, entry.getLevel());
        assertEquals(message, entry.getMessage());
        assertEquals(time, entry.getTime());
        assertEquals(exception, entry.getException());
        
        NodeIdentifier pi = NodeIdentifierFactory.fromHostAndNumberString("horst:3");
        entry.setPlatformIdentifer(pi);
        assertEquals(pi, entry.getPlatformIdentifer());
        
        entry.toString();
        new SerializableLogEntry(name, LogService.LOG_DEBUG, message, time, exception).toString();
        new SerializableLogEntry(name, LogService.LOG_INFO, message, time, exception).toString();
        new SerializableLogEntry(name, LogService.LOG_WARNING, message, time, exception).toString();
        new SerializableLogEntry(name, LogService.LOG_ERROR, message, time, exception).toString();
        
        final long laterTime = 43;
        SerializableLogEntry laterEntry = new SerializableLogEntry(name, level, message, laterTime, exception);
        
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
