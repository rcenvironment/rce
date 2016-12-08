/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeId;
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

    private InstanceNodeId nodeId;

    private UUID drId;

    private DataReference dr;

    private DataReference anotherDr;

    private RemotableFileDataServiceImpl fileDataService;

    /** Set up. */
    @Before
    public void setUp() {
        nodeId = NodeIdentifierTestUtils.createTestInstanceNodeIdWithDisplayName("dummy");
        drId = UUID.randomUUID();

        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        dr = new DataReference(drId.toString(), nodeId, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));
        anotherDr = new DataReference(UUID.randomUUID().toString(), nodeId, birefs);

        fileDataService = new RemotableFileDataServiceImpl();
        fileDataService.bindPlatformService(new PlatformServiceDefaultStub());

        MetaDataBackendService metaDataBackendService = EasyMock.createNiceMock(MetaDataBackendService.class);
        EasyMock.replay(metaDataBackendService);

        new BackendSupportTest().setUp();
        new BackendSupport().activate(BackendSupportTest.createBundleContext(metaDataBackendService, new DummyDataBackend()));
    }

    /** Test. */
    @Test
    public void testGetStreamFromDataReference() {
        fileDataService.getStreamFromDataReference(dr, true);
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
        fileDataService.newReferenceFromStream(is, meta);
    }

    /**
     * Test implementation of {@link DataBackend}.
     * 
     * @author Doreen Seider
     */
    private class DummyDataBackend implements DataBackend {

        @Override
        public URI suggestLocation(UUID guid) {
            return null;
        }

        @Override
        public long put(URI loc, Object object) {
            return 0;
        }

        @Override
        public boolean delete(URI loc) {
            return false;
        }

        @Override
        public Object get(URI loc) {
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
