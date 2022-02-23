/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.log.RemotableLogReaderService;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link DistributedLogReaderServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (7.0.0 adaptations; 8.0.0 id adaptations)
 */
// TODO consider replacing this with a VirtualInstance test for more realistic service behavior - misc_ro
public class DistributedLogReaderServiceImplTest {

    private final String removed = "removed";

    private final String added = "added";

    private final InstanceNodeSessionId instanceIdLocal = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("local");

    private final InstanceNodeSessionId instanceIdRemote = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("remote");

    private final InstanceNodeSessionId instanceIdNotReachable = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("notReachable");

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
        logReader.addLogListener(logListener, instanceIdLocal);
        logReader.addLogListener(logListener, instanceIdRemote);
        logReader.addLogListener(logListener, instanceIdNotReachable);
    }

    /** Test. */
    @Test
    public void testGetLog() {
        List<SerializableLogEntry> entries = logReader.getLog(instanceIdLocal);
        assertEquals(localLogEntry, entries.get(0));
        assertEquals(1, entries.size());

        entries = logReader.getLog(instanceIdRemote);
        assertEquals(remoteLogEntry, entries.get(0));
        assertEquals(1, entries.size());

        assertEquals(0, logReader.getLog(instanceIdNotReachable).size());
    }

    /** Test. */
    @Test
    public void testRemoveListener() {
        logReader.removeLogListener(logListener, instanceIdLocal);
        logReader.removeLogListener(logListener, instanceIdRemote);
        logReader.removeLogListener(logListener, instanceIdNotReachable);
    }

    /**
     * Test {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) {
            T service = null;
            if (iface == RemotableLogReaderService.class && instanceIdLocal.equals(nodeId)) {
                service = (T) new DummyLocalSerializableLogReaderService();
            } else if (iface == RemotableLogReaderService.class && instanceIdRemote.equals(nodeId)) {
                service = (T) new DummyRemoteSerializableLogReaderService();
            } else if (iface == RemotableLogReaderService.class && instanceIdNotReachable.equals(nodeId)) {
                service = (T) new DummyNotReachableSerializableLogReaderService();
            }
            return service;
        }

    }

    /**
     * Dummy local {@link RemotableLogReaderService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (7.0.0 adaptations)
     */
    @SuppressWarnings("serial")
    private class DummyLocalSerializableLogReaderService implements RemotableLogReaderService {

        @Override
        public void addLogListener(SerializableLogListener listener) throws RemoteOperationException {
            if (listener == logListener) {
                throw new RemoteOperationException(added);
            }
        }

        @Override
        public List<SerializableLogEntry> getLog() throws RemoteOperationException {
            return new LinkedList<SerializableLogEntry>() {

                {
                    add(localLogEntry);
                }
            };
        }

        @Override
        public void removeLogListener(SerializableLogListener listener) throws RemoteOperationException {
            if (listener == logListener) {
                throw new RemoteOperationException(removed);
            }
        }

    }

    /**
     * Dummy remote {@link RemotableLogReaderService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (7.0.0 adaptations)
     */
    @SuppressWarnings("serial")
    private class DummyRemoteSerializableLogReaderService implements RemotableLogReaderService {

        @Override
        public void addLogListener(SerializableLogListener listener) {}

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
     * Dummy not reachable {@link RemotableLogReaderService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (7.0.0 adaptations)
     */
    @SuppressWarnings("serial")
    private class DummyNotReachableSerializableLogReaderService implements RemotableLogReaderService {

        private static final String NOT_REACHABLE = "node unreachable";

        @Override
        public void addLogListener(SerializableLogListener listener) throws RemoteOperationException {
            throw new RemoteOperationException(NOT_REACHABLE);
        }

        @Override
        public List<SerializableLogEntry> getLog() throws RemoteOperationException {
            throw new RemoteOperationException(NOT_REACHABLE);
        }

        @Override
        public void removeLogListener(SerializableLogListener listener) throws RemoteOperationException {
            throw new RemoteOperationException(NOT_REACHABLE);
        }

    }
}
