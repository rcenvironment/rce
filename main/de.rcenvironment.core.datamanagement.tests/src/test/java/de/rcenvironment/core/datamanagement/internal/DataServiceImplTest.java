/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.MetaDataService;
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
 */
public class DataServiceImplTest {

    private User certificate;

    private NodeIdentifier pi;

    private UUID drId;

    private FileDataServiceImpl fileDataService;

    private DataReference dr;

    private DataReference newestDr;

    /** Set up. */
    @Before
    public void setUp() {
        pi = NodeIdentifierFactory.fromNodeId("na klar:6");
        drId = UUID.randomUUID();

        certificate = EasyMock.createNiceMock(User.class);
        EasyMock.expect(certificate.isValid()).andReturn(true).anyTimes();
        EasyMock.replay(certificate);

        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        dr = new DataReference(drId.toString(), pi, birefs);
        birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));
        newestDr = new DataReference(UUID.randomUUID().toString(), pi, birefs);

        fileDataService = new FileDataServiceImpl();
        fileDataService.bindCommunicationService(new DummyCommunicationService());
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

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            Object service = null;
            if (iface.equals(MetaDataService.class)) {
                service = EasyMock.createNiceMock(MetaDataService.class);
            } else if (iface.equals(FileDataService.class)) {
                service = new DummyFileDataService();
            }
            return service;
        }

    }

    /**
     * Test implementation of the {@link FileDataService}.
     * 
     * @author Doreen Seider
     */
    private class DummyFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(User proxyCertificate, DataReference dataReference, Boolean calledFromRemote)
            throws AuthorizationException {
            if (proxyCertificate == certificate && dataReference.equals(newestDr)) {
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
            return 0;
        }

        @Override
        public boolean delete(URI loc) {
            return false;
        }

        @Override
        public Object get(URI loc) {
            return null;
        }

    }
}
