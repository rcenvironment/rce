/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.datamanagement.RemotableFileDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.DistributableInputStream;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamanagement.testutils.FileDataServiceDefaultStub;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.notification.api.RemotableNotificationService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases of {@link FileDataServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adapted for new upload mechanism; id adaptations)
 * @author Brigitte Boden (adapted usage of node ids)
 */
public class DistributedFileDataServiceImplTest {

    private static final String REVISION = "1";

    private static final String CLOSE_BRACKET = ")";

    private static final String OPEN_BRACKET = "(";

    private static final String EQUALS_SIGN = "=";

    // should be >UPLOAD_CHUNK_SIZE for proper testing
    private static final int UPLOAD_TEST_SIZE = 300000;

    // should be <UPLOAD_CHUNK_SIZE for proper testing of single step upload
    private static final int SMALL_UPLOAD_TEST_SIZE = 1024;

    private static String xmlBackendProvider = "snoopy";

    private static String fileBackendProvider = "linus";

    private static String dataScheme = "ftp";

    private static String catalogBackendProvider = "de.rcenvironment.core.datamanagement.backend.metadata.derby";

    private final int read = 7;

    private FileDataServiceImpl fileDataService;

    private LogicalNodeSessionId localLogicalNodeSessionId;

    private LogicalNodeSessionId unreachableLogicalNodeSessionId;

    private UUID referenceID;

    private DataReference reference;

    private DataReference notReachableReference;

    // for mock remote testing
    private volatile DataReference lastMockRemoteDataReference;

    private InputStream is;

    private MetaDataSet mds;

    private LogicalNodeSessionId mockRemoteLogicalNodeSessionId;

    private LogicalNodeId mockRemoteLogicalNodeId;

    private MetaDataBackendService catalogBackend = EasyMock.createNiceMock(MetaDataBackendService.class);

    private DataBackend dataBackend;

    private BackendSupport backendSupport;

    private UUID notReachableReferenceID;

    private LogicalNodeId localDefaultLogicalNodeId;

    private LogicalNodeId unreachableLogicalNodeId;

    public DistributedFileDataServiceImplTest() {
        mockRemoteLogicalNodeSessionId = NodeIdentifierTestUtils.createTestLogicalNodeSessionIdWithDisplayName("mockRemote", true);
        mockRemoteLogicalNodeId = mockRemoteLogicalNodeSessionId.convertToLogicalNodeId();
    }

    /**
     * Set up.
     * 
     * @throws IdentifierException not expected
     */
    @Before
    public void setUp() throws IdentifierException {
        TempFileServiceAccess.setupUnitTestEnvironment();

        localLogicalNodeSessionId = NodeIdentifierTestUtils.createTestLogicalNodeSessionIdWithDisplayName("horst", true);
        localDefaultLogicalNodeId = localLogicalNodeSessionId.convertToLogicalNodeId();
        unreachableLogicalNodeSessionId = NodeIdentifierTestUtils.createTestLogicalNodeSessionIdWithDisplayName("unreachable", true);
        unreachableLogicalNodeId = unreachableLogicalNodeSessionId.convertToLogicalNodeId();
        referenceID = UUID.randomUUID();
        notReachableReferenceID = UUID.randomUUID();
        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));

        reference = new DataReference(referenceID.toString(), localDefaultLogicalNodeId, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));
        notReachableReference =
            new DataReference(notReachableReferenceID.toString(), unreachableLogicalNodeId, birefs);

        is = new InputStream() {

            @Override
            public int read() throws IOException {
                return read;
            }
        };
        mds = new MetaDataSet();

        fileDataService = new FileDataServiceImpl();
        fileDataService.bindCommunicationService(new MockCommunicationService());
        fileDataService.bindPlatformService(new MockPlatformService());
        fileDataService.activate(EasyMock.createNiceMock(BundleContext.class));

        dataBackend = EasyMock.createNiceMock(DataBackend.class);
        EasyMock.expect(dataBackend.get(EasyMock.anyObject(URI.class))).andReturn(new InputStream() {

            @Override
            public int read() throws IOException {
                return 0;
            }
        });
        EasyMock.replay(dataBackend);

        backendSupport = new BackendSupport();
        backendSupport.bindConfigurationService(new DummyConfigurationService());
        backendSupport.bindDataBackendService(dataBackend);
        backendSupport.activate(createBundleContext(catalogBackend, dataBackend));
    }

    /**
     * Tests successful access to a remote data reference.
     * 
     * @throws IOException if an error occurs.
     * @throws CommunicationException on communication error
     */
    @Test
    public void testGetStreamFromDataReferenceSuccess() throws IOException, CommunicationException {
        InputStream stream = fileDataService.getStreamFromDataReference(reference);
        assertEquals(read, stream.read());
    }

    /**
     * Test access attempt on an unreachable reference.
     * 
     * @throws IOException if an error occurs.
     * @throws CommunicationException on communication error
     */
    @Test
    public void testGetStreamFromDataReferenceFailure() throws IOException, CommunicationException {
        try {
            fileDataService.getStreamFromDataReference(notReachableReference);
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("DataReference is not equal")); // TODO somewhat brittle
        }
    }

    /**
     * Test.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testLocalNewReferenceFromStream() throws Exception {
        // test local
        DataReference dr = fileDataService.newReferenceFromStream(is, mds, localLogicalNodeSessionId);
        assertEquals(reference, dr);
        // test "null" (should be local)
        dr = fileDataService.newReferenceFromStream(is, mds, null);
        assertEquals(reference, dr);
    }

    /**
     * Test.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testDistributedNewReferenceFromStream() throws Exception {
        assertFalse(localLogicalNodeSessionId.equals(mockRemoteLogicalNodeSessionId));
        assertFalse(localDefaultLogicalNodeId.equals(mockRemoteLogicalNodeId));
        InputStream testStream = new ByteArrayInputStream(new byte[UPLOAD_TEST_SIZE]);
        DataReference remoteRef = fileDataService.newReferenceFromStream(testStream, mds, mockRemoteLogicalNodeSessionId);
        assertNotNull(remoteRef);
        assertEquals(lastMockRemoteDataReference, remoteRef);
        assertEquals(mockRemoteLogicalNodeId, remoteRef.getStorageNodeId());
    }

    /**
     * Test for small uploads.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testDistributedNewReferenceFromStreamSmallUpload() throws Exception {
        assertFalse(localLogicalNodeSessionId.equals(mockRemoteLogicalNodeSessionId));
        assertFalse(localDefaultLogicalNodeId.equals(mockRemoteLogicalNodeId));
        InputStream testStream = new ByteArrayInputStream(new byte[SMALL_UPLOAD_TEST_SIZE]);
        DataReference remoteRef = fileDataService.newReferenceFromStream(testStream, mds, mockRemoteLogicalNodeSessionId);
        assertNotNull(remoteRef);
        assertEquals(lastMockRemoteDataReference, remoteRef);
        assertEquals(mockRemoteLogicalNodeId, remoteRef.getStorageNodeId());
    }

    /**
     * Test for small uploads, stream not returning full buffer.
     * 
     * @throws Exception on uncaught errors
     */
    @Test
    public void testDistributedNewReferenceFromStreamNoFullBuffer() throws Exception {
        assertFalse(localLogicalNodeSessionId.equals(mockRemoteLogicalNodeSessionId));
        assertFalse(localDefaultLogicalNodeId.equals(mockRemoteLogicalNodeId));
        InputStream testStream = new MockInputStream(new byte[SMALL_UPLOAD_TEST_SIZE]);
        DataReference remoteRef = fileDataService.newReferenceFromStream(testStream, mds, mockRemoteLogicalNodeSessionId);
        assertNotNull(remoteRef);
        assertEquals(lastMockRemoteDataReference, remoteRef);
        assertEquals(mockRemoteLogicalNodeId, remoteRef.getStorageNodeId());
    }

    /**
     * Test implementation of the {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class MockCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) {
            if (iface == RemotableNotificationService.class
                && nodeId.equals(localLogicalNodeSessionId)) {
                return (T) new MockLocalFileDataService();
            } else if (nodeId.equals(unreachableLogicalNodeSessionId)) {
                return (T) new MockUnreachableFileDataService();
            } else {
                return (T) new MockRemoteFileDataService();
            }
        }
    }

    /**
     * Test implementation of the {@link RemotableFileDataService}.
     * 
     * @author Doreen Seider
     */
    private class MockLocalFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            if (dataReference.equals(reference)) {
                return is;
            } else {
                throw new RuntimeException();
            }
        }

        @Override
        public DataReference newReferenceFromStream(InputStream inputStream,
            MetaDataSet metaDataSet) throws AuthorizationException {
            if (inputStream.equals(is)
                || inputStream instanceof DistributableInputStream) {
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
        public String initializeUpload() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long appendToUpload(String id, byte[] data) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void finishUpload(String id, MetaDataSet metaDataSet) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataReference pollUploadForDataReference(String id) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Mock of a remote {@link RemotableFileDataService} based on the actual {@link RemotableFileDataServiceImpl} implementation.
     * 
     * @author Robert Mischke (I don't think so; copy/paste artifact? - misc_ro)
     */
    private class MockRemoteFileDataService extends RemotableFileDataServiceImpl {

        @Override
        public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, Boolean alreadyCompressed) {
            Set<BinaryReference> birefs = new HashSet<BinaryReference>();
            birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, REVISION));

            lastMockRemoteDataReference =
                new DataReference(referenceID.toString(), mockRemoteLogicalNodeSessionId.convertToLogicalNodeId(), birefs);
            return lastMockRemoteDataReference;
        }

        @Override
        public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote, Boolean decompress)
            throws AuthorizationException {
            if (dataReference.equals(reference)) {
                return is;
            } else {
                // TODO review: can this really only happen on programming errors? otherwise, it shouldn't be an RTE - misc_ro
                throw new RuntimeException("DataReference is not equal: " + dataReference + " / " + reference);
            }
        }

    }

    /**
     * Not reachable test implementation of the {@link RemotableFileDataService}. Used for remote upload testing.
     * 
     * @author Doreen Seider
     */
    private class MockUnreachableFileDataService implements RemotableFileDataService {

        @Override
        public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet)
            throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public String initializeUpload() throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public long appendToUpload(String id, byte[] data) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public void finishUpload(String id, MetaDataSet metaDataSet) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference pollUploadForDataReference(String id) {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public void deleteReference(String dataReference) throws AuthorizationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet) throws IOException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote, Boolean decompress)
            throws RemoteOperationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference newReferenceFromStream(InputStream inputStream, MetaDataSet metaDataSet, Boolean alreadyCompressed)
            throws RemoteOperationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public void finishUpload(String id, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException,
            RemoteOperationException {
            throw new UndeclaredThrowableException(null);
        }

        @Override
        public DataReference uploadInSingleStep(byte[] data, MetaDataSet metaDataSet, Boolean alreadyCompressed) throws IOException,
            RemoteOperationException {
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
        public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
            return localLogicalNodeSessionId.convertToInstanceNodeSessionId();
        }

        @Override
        public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
            return localLogicalNodeSessionId.isSameInstanceNodeAs(nodeId);
        }

    }

    /**
     * Test implementation for simulating the situation where read() does not return a full buffer.
     * 
     * @author Brigitte Boden
     */
    private class MockInputStream extends ByteArrayInputStream {

        private static final int CHUNK_SIZE = 256;

        MockInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (b.length > CHUNK_SIZE) {
                return super.read(b, 0, CHUNK_SIZE);
            }
            return super.read(b);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            if (len > CHUNK_SIZE) {
                return super.read(b, off, CHUNK_SIZE);
            }
            return super.read(b, off, len);
        }
    }

    /**
     * Helper method creating a {@link BundleContext} object which retrieves the given backend services.
     * 
     * @param catalogBackend {@link MetaDataBackendService} to retrieve.
     * @param dataBackend {@link DataBackend} to retrieve.
     * @return the {@link BundleContext}.
     */
    public static BundleContext createBundleContext(MetaDataBackendService catalogBackend, DataBackend dataBackend) {

        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);

        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn("huebscherName").anyTimes();
        EasyMock.replay(bundleMock);

        EasyMock.expect(bundleContext.getBundle()).andReturn(bundleMock).anyTimes();

        // catalog backend
        String catalogFilterString = OPEN_BRACKET + MetaDataBackendService.PROVIDER + EQUALS_SIGN
            + catalogBackendProvider + CLOSE_BRACKET;
        ServiceReference<?> catalogServiceRef = EasyMock.createNiceMock(ServiceReference.class);
        ServiceReference<?>[] catalogServiceRefs = { catalogServiceRef };
        try {
            EasyMock.expect(bundleContext.getServiceReferences(MetaDataBackendService.class.getName(), catalogFilterString))
                .andReturn(catalogServiceRefs).anyTimes();
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        bundleContext.getService(catalogServiceRef);
        EasyMock.expectLastCall().andReturn(catalogBackend).anyTimes();

        // data backend
        String dataFilterStringForScheme = OPEN_BRACKET + DataBackend.PROVIDER + EQUALS_SIGN + fileBackendProvider + CLOSE_BRACKET;
        String dataFilterStringForProvider = OPEN_BRACKET + DataBackend.PROVIDER + EQUALS_SIGN + xmlBackendProvider + CLOSE_BRACKET;
        String dataSchemefilterString = OPEN_BRACKET + DataBackend.SCHEME + EQUALS_SIGN + dataScheme + CLOSE_BRACKET;
        ServiceReference<?> dataServiceRef = EasyMock.createNiceMock(ServiceReference.class);
        ServiceReference<?>[] dataServiceRefs = { dataServiceRef };
        try {
            EasyMock.expect(bundleContext.getServiceReferences(DataBackend.class.getName(), dataFilterStringForProvider))
                .andReturn(dataServiceRefs).anyTimes();
            EasyMock.expect(bundleContext.getServiceReferences(DataBackend.class.getName(), dataFilterStringForScheme))
                .andReturn(dataServiceRefs).anyTimes();
            EasyMock.expect(bundleContext.getServiceReferences(DataBackend.class.getName(), dataSchemefilterString))
                .andReturn(dataServiceRefs).anyTimes();
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        bundleContext.getService(dataServiceRef);
        EasyMock.expectLastCall().andReturn(dataBackend).anyTimes();

        // for failures
        EasyMock.expect(bundleContext.getServiceReference((String) null)).andReturn(null).anyTimes();

        EasyMock.replay(bundleContext);

        return bundleContext;
    }

    /**
     * Test implementation of {@link ConfigurationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyConfigurationService extends MockConfigurationService.ThrowExceptionByDefault {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getConfiguration(String identifier, Class<T> clazz) {
            DataManagementConfiguration config = new DataManagementConfiguration();
            config.setMetaDataBackend(catalogBackendProvider);
            config.setFileDataBackend(fileBackendProvider);

            return (T) config;
        }

    }

}
