/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.log.SerializableLogReaderService;

/**
 * Test cases for {@link DistributedLogReaderServiceImpl}.
 *
 * @author Doreen Seider
 */
public class DistributedLogReaderServiceImplTest {

    private final String removed = "removed";
    private final String added = "added";
    private final NodeIdentifier piLocal = NodeIdentifierFactory.fromHostAndNumberString("localhost:1");
    private final NodeIdentifier piRemote = NodeIdentifierFactory.fromHostAndNumberString("remotehost:1");
    private final NodeIdentifier piNotReachable = NodeIdentifierFactory.fromHostAndNumberString("notReachable:1");
    private final SerializableLogEntry localLogEntry = EasyMock.createNiceMock(SerializableLogEntry.class);
    private final SerializableLogEntry remoteLogEntry = EasyMock.createNiceMock(SerializableLogEntry.class);
    private final SerializableLogListener logListener = new SerializableLogListener() {
        
        private static final long serialVersionUID = 1L;
        
        @Override
        public void logged(SerializableLogEntry entry) {}
        
        @Override
        public Class<? extends Serializable> getInterface() {
            return SerializableLogListener.class;
        }
    };

    private BundleContext context = EasyMock.createNiceMock(BundleContext.class);
    private DistributedLogReaderServiceImpl logReader = new DistributedLogReaderServiceImpl();
    
    /** Set up method. */
    @Before
    public void setUp() {
        logReader.bindCommunicationService(new DummyCommunicationService());
        logReader.activate(context);
    }
    
    /** Test. */
    @Test
    public void testAddListener() {
        logReader.addLogListener(logListener, piLocal);
        logReader.addLogListener(logListener, piRemote);
        logReader.addLogListener(logListener, piNotReachable);
    }

    /** Test. */
    @Test
    public void testGetLog() {
        List<SerializableLogEntry> entries = logReader.getLog(piLocal);
        assertEquals(localLogEntry, entries.get(0));
        assertEquals(1, entries.size());
        
        entries = logReader.getLog(piRemote);
        assertEquals(remoteLogEntry, entries.get(0));
        assertEquals(1, entries.size());
        
        assertEquals(0, logReader.getLog(piNotReachable).size());
    }

    /** Test. */
    @Test
    public void testRemoveListener() {
        logReader.removeLogListener(logListener, piLocal);
        logReader.removeLogListener(logListener, piRemote);
        logReader.removeLogListener(logListener, piNotReachable);
    }
    
    /**
     * Test {@link CommunicationService} implementation.
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            Object service = null;
            if (iface == SerializableLogReaderService.class && piLocal.equals(nodeId) && bundleContext == context) {
                service = new DummyLocalSerializableLogReaderService();
            } else if (iface == SerializableLogReaderService.class && piRemote.equals(nodeId) && bundleContext == context) {
                service = new DummyRemoteSerializableLogReaderService();
            } else if (iface == SerializableLogReaderService.class && piNotReachable.equals(nodeId)
                && bundleContext == context) {
                service = new DummyNotReachableSerializableLogReaderService();
            }
            return service;
        }

    }

    /**
     * Dummy local {@link SerializableLogReaderService} implementation.
     * @author Doreen Seider
     */
    @SuppressWarnings("serial")
    private class DummyLocalSerializableLogReaderService implements SerializableLogReaderService {

        @Override
        public void addLogListener(SerializableLogListener listener) {
            if (listener == logListener) {
                throw new RuntimeException(added);
            }
        }

        @Override
        public List<SerializableLogEntry> getLog() {
            return new LinkedList<SerializableLogEntry>() {

                {
                    add(localLogEntry);
                }
            };
        }

        @Override
        public void removeLogListener(SerializableLogListener listener) {
            if (listener == logListener) {
                throw new RuntimeException(removed);
            }
        }

    }

    /**
     * Dummy remote {@link SerializableLogReaderService} implementation.
     * 
     * @author Doreen Seider
     */
    @SuppressWarnings("serial")
    private class DummyRemoteSerializableLogReaderService implements SerializableLogReaderService {

        @Override
        public void addLogListener(SerializableLogListener listener) {
        }

        @Override
        public List<SerializableLogEntry> getLog() {
            return new LinkedList<SerializableLogEntry>() {

                {
                    add(remoteLogEntry);
                }
            };
        }

        @Override
        public void removeLogListener(SerializableLogListener listener) {}

    }

    /**
     * Dummy not reachable {@link SerializableLogReaderService} implementation.
     * 
     * @author Doreen Seider
     */
    @SuppressWarnings("serial")
    private class DummyNotReachableSerializableLogReaderService implements SerializableLogReaderService {

        @Override
        public void addLogListener(SerializableLogListener listener) {
            throw new UndeclaredThrowableException(new RuntimeException());
        }

        @Override
        public List<SerializableLogEntry> getLog() {
            throw new UndeclaredThrowableException(new RuntimeException());
        }

        @Override
        public void removeLogListener(SerializableLogListener listener) {
            throw new UndeclaredThrowableException(new RuntimeException());
        }

    }
}
