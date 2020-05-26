/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.MetaData;
import de.rcenvironment.core.datamanagement.commons.MetaDataKeys;
import de.rcenvironment.core.datamanagement.commons.MetaDataSet;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link RemotableFileDataServiceImpl}.
 * 
 * @author Juergen Klein
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class FileDataServiceImplTest {

    private final URI location = URI.create("test");

    private LogicalNodeId nodeId;

    private UUID drId;

    private DataReference dr;

    private BinaryReference binaryReference;

    private RemotableFileDataServiceImpl fileDataService;

    /** Set up. */
    @Before
    public void setUp() {
        nodeId = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        drId = UUID.randomUUID();

        binaryReference = new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1");
        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(binaryReference);

        dr = new DataReference(drId.toString(), nodeId, birefs);

        fileDataService = new RemotableFileDataServiceImpl();
        fileDataService.bindPlatformService(new PlatformServiceDefaultStub() {

            @Override
            public LogicalNodeId getLocalDefaultLogicalNodeId() {
                return nodeId;
            }
        });

        MetaDataBackendService metaDataBackendService = EasyMock.createNiceMock(MetaDataBackendService.class);
        EasyMock.replay(metaDataBackendService);

        BackendSupport backendSupport = new BackendSupport();

        backendSupport.bindDataBackendService(new DummyDataBackend());
        backendSupport.activate(BackendSupportTest.createBundleContext(metaDataBackendService, new DummyDataBackend()));
    }

    /**
     * Test.
     * 
     * @throws IOException on unexpected errors
     */
    @Test
    public void testGetStreamFromDataReference() throws IOException {
        InputStream stream = fileDataService.getStreamFromDataReference(dr, true);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        final int buffersize = 8;
        char[] buffer = new char[buffersize];
        int blockLength = bufferedReader.read(buffer, 0, buffersize);
        String content = new String(buffer, 0, blockLength);
        assertNotNull(content);
        assertNotEquals("", content);
    }

    /**
     * Test.
     * 
     * @throws RemoteOperationException standard remote operation exception
     */
    @Test
    public void testNewReferenceFromStream() throws RemoteOperationException {
        InputStream is = new InputStream() {

            @Override
            public int read() throws IOException {
                return 0;
            }
        };
        MetaDataSet meta = new MetaDataSet();
        meta.setValue(new MetaData(MetaDataKeys.COMPONENT_RUN_ID, true, true), "7");
        DataReference ref = fileDataService.newReferenceFromStream(is, meta);
        assertNotNull(ref.getDataReferenceKey());
        assertTrue(ref.getStorageNodeId().equals(nodeId));
        assertEquals(1, ref.getBinaryReferences().size());
    }

    /**
     * Test implementation of {@link DataBackend}.
     * 
     * @author Doreen Seider
     */
    private class DummyDataBackend implements DataBackend {

        @Override
        public URI suggestLocation(UUID guid) {
            return location;
        }

        @Override
        public long put(URI loc, Object object) {
            return put(loc, object);
        }

        @Override
        public long put(URI loc, Object object, boolean alreadyCompressed) {
            return 0;
        }

        @Override
        public boolean delete(URI loc) {
            return false;
        }

        @Override
        public Object get(URI loc) {
            return get(loc, true);
        }

        @Override
        public Object get(URI loc, boolean decompress) {
            if (location.equals(loc)) {
                return new InputStream() {

                    @Override
                    public int read() throws IOException {
                        return 0;
                    }
                };
            }
            return null;
        }

    }

}
