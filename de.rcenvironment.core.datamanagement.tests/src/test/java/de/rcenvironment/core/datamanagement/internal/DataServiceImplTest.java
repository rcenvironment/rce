/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.datamanagement.RemotableFileDataService;
import de.rcenvironment.core.datamanagement.RemotableMetaDataService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.testutils.FileDataServiceDefaultStub;
import de.rcenvironment.core.datamodel.api.CompressionFormat;

/**
 * Test cases for {@link DataServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class DataServiceImplTest {

    private LogicalNodeId pi;

    private UUID drId;

    private RemotableFileDataServiceImpl fileDataService;

    private DataReference dr;

    private DataReference newestDr;

    /** Set up. */
    @Before
    public void setUp() {
        pi = NodeIdentifierTestUtils.createTestDefaultLogicalNodeId();
        drId = UUID.randomUUID();

        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        dr = new DataReference(drId.toString(), pi, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));
        newestDr = new DataReference(UUID.randomUUID().toString(), pi, birefs);

        fileDataService = new RemotableFileDataServiceImpl();
        fileDataService.bindPlatformService(new PlatformServiceDefaultStub());
        fileDataService.activate(EasyMock.createNiceMock(BundleContext.class));

        MetaDataBackendService dummyCatalogBackend = EasyMock.createNiceMock(MetaDataBackendService.class);

        new BackendSupportTest().setUp();
        new BackendSupport().activate(BackendSupportTest.createBundleContext(dummyCatalogBackend, new DummyDataBackend()));
    }

    /** Test. */
    @Test
    public void testDeleteReference() {
        fileDataService.deleteReference(dr.getBinaryReferences().iterator().next().getBinaryReferenceKey());
    }

    /**
     * Test implementation of the {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) {
            T service = null;
            if (iface.equals(RemotableMetaDataService.class)) {
                service = (T) EasyMock.createNiceMock(RemotableMetaDataService.class);
            } else if (iface.equals(RemotableFileDataService.class)) {
                service = (T) new DummyFileDataService();
            }
            return service;
        }

    }

    /**
     * Test implementation of the {@link RemotableFileDataService}.
     * 
     * @author Doreen Seider
     */
    private class DummyFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            if (dataReference.equals(newestDr)) {
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
            return null;
        }

    }
}
