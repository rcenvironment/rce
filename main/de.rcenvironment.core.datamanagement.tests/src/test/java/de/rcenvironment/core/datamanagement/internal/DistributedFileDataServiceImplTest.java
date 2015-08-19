/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.datamanagement.DataService;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.DistributableInputStream;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.testutils.FileDataServiceDefaultStub;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Test cases of {@link DistributedFileDataServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adapted for new upload mechanism)
 */
public class DistributedFileDataServiceImplTest {

    private static final String REVISION = "1";

    // should be >UPLOAD_CHUNK_SIZE for proper testing
    private static final int UPLOAD_TEST_SIZE = 300000;

    private final int read = 7;

    private DistributedFileDataServiceImpl distrFDS;

    private User certificateMock;

    private NodeIdentifier localNodeId;

    private NodeIdentifier unreachableNodeId;

    private UUID referenceID;

    private URI location = URI.create("test");

    private DataReference reference;

    private DataReference notReachableReference;

    // for mock remote testing
    private volatile DataReference lastMockRemoteDataReference;

    private InputStream is;

    private MetaDataSet mds;

    private NodeIdentifier mockRemoteNodeId;

    public DistributedFileDataServiceImplTest() {
        mockRemoteNodeId = NodeIdentifierFactory.fromHostAndNumberString("horst:7");
    }

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();

        certificateMock = EasyMock.createNiceMock(User.class);
        EasyMock.expect(certificateMock.isValid()).andReturn(true);
        EasyMock.replay(certificateMock);

        localNodeId = NodeIdentifierFactory.fromHostAndNumberString("horst:1");
        referenceID = UUID.randomUUID();
        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));

        reference = new DataReference(referenceID.toString(), localNodeId, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));
        notReachableReference =
            new DataReference(referenceID.toString(), NodeIdentifierFactory.fromHostAndNumberString("notreachable:1"), birefs);

        is = new InputStream() {

            @Override
            public int read() throws IOException {
                return read;
            }
        };
        mds = new MetaDataSet();

        distrFDS = new DistributedFileDataServiceImpl();
        distrFDS.bindCommunicationService(new MockCommunicationService());
        distrFDS.bindPlatformService(new MockPlatformService());
        distrFDS.bindFileDataService(new MockLocalFileDataService());
        distrFDS.activate(EasyMock.createNiceMock(BundleContext.class));
    }

    /**
     * Test.
     * 
     * @throws IOException if an error occurs.
     */
    @Test
    public void testGetStreamFromDataReference() throws IOException {
        InputStream stream = distrFDS.getStreamFromDataReference(certificateMock, reference);
        assertEquals(read, stream.read());
        stream = distrFDS.getStreamFromDataReference(certificateMock, notReachableReference);
        assertNull(stream);
    }

    /**
     * Test.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testLocalNewReferenceFromStream() throws Exception {
        // test local
        DataReference dr = distrFDS.newReferenceFromStream(certificateMock, is, mds, localNodeId);
        assertEquals(reference, dr);
        // test "null" (should be local)
        dr = distrFDS.newReferenceFromStream(certificateMock, is, mds, null);
        assertEquals(reference, dr);
    }

    /**
     * Test.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testDistributedNewReferenceFromStream() throws Exception {
        assertFalse(localNodeId.equals(mockRemoteNodeId));
        InputStream testStream = new ByteArrayInputStream(new byte[UPLOAD_TEST_SIZE]);
        DataReference remoteRef = distrFDS.newReferenceFromStream(certificateMock, testStream, mds, mockRemoteNodeId);
        assertNotNull(remoteRef);
        assertEquals(lastMockRemoteDataReference, remoteRef);
        assertEquals(mockRemoteNodeId, remoteRef.getNodeIdentifier());
    }

    /**
     * Test implementation of the {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class MockCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            return getService(iface, new HashMap<String, String>(), nodeId, bundleContext);
        }

        @Override
        public Object getService(Class<?> iface, Map<String, String> properties, NodeIdentifier nodeId,
            BundleContext bundleContext) throws IllegalStateException {
            if (nodeId.equals(localNodeId)) {
                return new MockLocalFileDataService();
            } else if (nodeId.equals(unreachableNodeId)) {
                return new MockUnreachableFileDataService();
            } else {
                return new MockRemoteFileDataService();
            }
        }
    }

    /**
     * Dummy implementation of {@link DataService}.
     * 
     * @author Doreen Seider
     */
    private class MockDataService implements DataService {

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {}

    }

    /**
     * Test implementation of the {@link FileDataService}.
     * 
     * @author Doreen Seider
     */
    private class MockLocalFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            if (proxyCertificate.equals(certificateMock) && dataReference.equals(reference)) {
                return is;
            } else {
                throw new RuntimeException();
            }
        }

        @Override
        public DataReference newReferenceFromStream(User proxyCertificate, InputStream inputStream,
            MetaDataSet metaDataSet) throws AuthorizationException {
            if (proxyCertificate.equals(certificateMock) && inputStream.equals(is)
                || proxyCertificate.equals(certificateMock) && inputStream instanceof DistributableInputStream) {
                return reference;
            } else {
                throw new RuntimeException();
            }
        }

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {
            if (!dataReference.equals(reference.getBinaryReferences().iterator().next().getBinaryReferenceKey())) {
                throw new RuntimeException();
            }
        }

        @Override
        public String initializeUpload(User user) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long appendToUpload(User user, String id, byte[] data) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void finishUpload(User user, String id, MetaDataSet metaDataSet) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataReference pollUploadForDataReference(User user, String id) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Mock of a remote {@link FileDataService} based on the actual {@link FileDataServiceImpl} implementation.
     * 
     * @author Robert Mischke
     */
    private class MockRemoteFileDataService extends FileDataServiceImpl {

        @Override
        @AllowRemoteAccess
        public DataReference newReferenceFromStream(User user, InputStream inputStream, MetaDataSet metaDataSet)
            throws AuthorizationException {
            Set<BinaryReference> birefs = new HashSet<BinaryReference>();
            birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));

            lastMockRemoteDataReference = new DataReference(referenceID.toString(), localNodeId, birefs);
            return lastMockRemoteDataReference;
        }
    }

    /**
     * Not reachable test implementation of the {@link FileDataService}. Used for remote upload testing.
     * 
     * @author Doreen Seider
     */
    private class MockUnreachableFileDataService implements FileDataService {

        @Override
        public InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference newReferenceFromStream(User proxyCertificate, InputStream inputStream, MetaDataSet metaDataSet)
            throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public String initializeUpload(User user) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public long appendToUpload(User user, String id, byte[] data) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public void finishUpload(User user, String id, MetaDataSet metaDataSet) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference pollUploadForDataReference(User user, String id) {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

    }

    /**
     * Test implementation of the {@link PlatformService}.
     * 
     * @author Doreen Seider
     * @author Robert Mischke
     */
    private class MockPlatformService extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return localNodeId;
        }

        @Override
        public boolean isLocalNode(NodeIdentifier nodeId) {
            return localNodeId.equals(nodeId);
        }

    }

}
