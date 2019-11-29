/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.file.service.legacy.api.RemotableFileStreamAccessService;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;

/**
 * Test cases for {@link ServiceRemoteFileConnection}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
@Deprecated
public class ServiceRemoteFileConnectionTest {

    private static final String TEST_INSTANCE_ID_STRING = NodeIdentifierTestUtils.createTestInstanceNodeIdString();

    private final UUID dmUuid = UUID.randomUUID();

    // TODO review/encapsulate
    private final String uri = "rce://" + TEST_INSTANCE_ID_STRING + "/" + dmUuid + "/7";

    /**
     * Common setup.
     */
    @Before
    public void setup() {
        NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
    }

    /**
     * Common teardown.
     */
    @After
    public void teardown() {
        NodeIdentifierTestUtils.removeTestNodeIdentifierServiceFromCurrentThread();
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void defaultTest() throws Exception {
        RemoteFileConnection connection = new ServiceRemoteFileConnection(new URI(uri), new DummyCommunicationService());
        int read = connection.read();
        final int minusOne = -1;
        assertTrue(read > minusOne);
        byte[] b = new byte[5];
        read = connection.read(b, 0, 5);
        connection.skip(5);
        assertEquals(5, read);
        connection.close();
    }

    /**
     * Dummy {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     */
    private static class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getRemotableService(Class<T> iface, NetworkDestination dest) throws IllegalStateException {
            ResolvableNodeId nodeId = (ResolvableNodeId) dest;
            // TODO could be improved; uses unnecessary conversions
            if (nodeId.isSameInstanceNodeAs(NodeIdentifierUtils
                .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(TEST_INSTANCE_ID_STRING))
                && iface == RemotableFileStreamAccessService.class) {
                return (T) new MockRemotableFileStreamAccessService();
            }
            return null;
        }

    }

    /**
     * Mock {@link RemotableFileStreamAccessService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (adapted for 7.0.0)
     */
    private static class MockRemotableFileStreamAccessService implements RemotableFileStreamAccessService {

        private final String testUUID = "snoopy";

        @Override
        public void close(String uuid) throws IOException {}

        @Override
        public String open(FileType type, String file) throws IOException {
            return testUUID;
        }

        @Override
        public int read(String uuid) throws IOException {
            if (uuid.equals(testUUID)) {
                return 5;
            }
            return 0;
        }

        @Override
        public byte[] read(String uuid, Integer len) throws IOException {
            if (uuid.equals(testUUID)) {
                return new byte[len];
            }
            return new byte[0];
        }

        @Override
        public long skip(String uuid, Long n) throws IOException {
            if (uuid.equals(testUUID)) {
                return n;
            }
            return 0;
        }

    }
}
