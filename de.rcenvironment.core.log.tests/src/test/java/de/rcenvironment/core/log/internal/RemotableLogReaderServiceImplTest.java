/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;

/**
 * Test cases for {@link RemotableLogReaderServiceImp}.
 *
 * @author Doreen Seider
 */
public class RemotableLogReaderServiceImplTest {

    private static final String BUNDLE_SYMBOLIC_NAME = "de.rce.comp.id";

    private final String removed = "removed";

    private final String added = "added";

    private final LogEntry logEntry = EasyMock.createNiceMock(LogEntry.class);

    private final SerializableLogListener logListener = new SerializableLogListener() {

        private static final long serialVersionUID = 1L;

        @Override
        public void logged(SerializableLogEntry entry) {}

        @Override
        public Class<? extends Serializable> getInterface() {
            return SerializableLogListener.class;
        }
    };

    private RemotableLogReaderServiceImpl logReader = new RemotableLogReaderServiceImpl();

    /** Set up method. */
    @Before
    public void setUp() {
        logReader.bindLogReaderService(new DummyLogReaderService());

        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
        EasyMock.replay(bundleMock);
        BundleContext bundleContextMock = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContextMock.getBundle()).andReturn(bundleMock).anyTimes();
        EasyMock.replay(bundleContextMock);
        ComponentContext contextMock = EasyMock.createNiceMock(ComponentContext.class);
        EasyMock.expect(contextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
        EasyMock.replay(contextMock);

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("bundle");
        EasyMock.replay(bundle);
        EasyMock.expect(logEntry.getBundle()).andReturn(bundle);
        EasyMock.expect(logEntry.getMessage()).andReturn("message");
        EasyMock.replay(logEntry);
    }

    /** Test. */
    @Test
    public void testAddAndRemoveListener() {
        try {
            logReader.addLogListener(logListener);
            fail();
        } catch (RuntimeException e) {
            assertTrue(added.equals(e.getMessage()));
        }
        try {
            logReader.removeLogListener(logListener);
            fail();
        } catch (RuntimeException e) {
            assertTrue(removed.equals(e.getMessage()));
        }
    }

    /** Test. */
    @Test
    public void testGetLog() {
        List<SerializableLogEntry> entries = logReader.getLog();
        assertEquals(1, entries.size());

    }

    /**
     * Dummy local {@link LogReaderService} implementation.
     * @author Doreen Seider
     */
    private class DummyLogReaderService implements LogReaderService {

        @Override
        public void addLogListener(LogListener listener) {
            if (listener != logListener) {
                throw new RuntimeException(added);
            }
        }

        @Override
        public Enumeration<LogEntry> getLog() {
            @SuppressWarnings("serial")
            List<LogEntry> logEntries = new Vector<LogEntry>() {{ add(logEntry); }};
            return ((Vector<LogEntry>) logEntries).elements();
        }

        @Override
        public void removeLogListener(LogListener listener) {
            if (listener != logListener) {
                throw new RuntimeException(removed);
            }
        }

    }

}
