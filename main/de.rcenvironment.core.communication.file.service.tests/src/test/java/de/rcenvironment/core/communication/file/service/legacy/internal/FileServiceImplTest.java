/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authorization.AuthorizationException;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;
import de.rcenvironment.core.datamanagement.FileDataService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.testutils.FileDataServiceDefaultStub;
import de.rcenvironment.core.datamodel.api.CompressionFormat;

/**
 * Test cases for the {@link FileServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (changed to classpath resource loading)
 */
@Deprecated
public class FileServiceImplTest {

    private static final String INVALID_UUID = "uuid";

    private final UUID dmUuid = UUID.fromString("e293a96a-ddf2-41c5-b94e-c95a3a5cecc2");

    private final String dmUri = dmUuid.toString();

    private final Integer noOfBytes = 4;

    private FileServiceImpl fileService;

    private DataReference dataRef;

    private InputStream inputStream = EasyMock.createNiceMock(InputStream.class);

    private User user = EasyMock.createNiceMock(User.class);

    private MetaDataBackendService metaDataBackendService;

    /**
     * Set up.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Before
    public void setUp() throws IOException {
        fileService = new FileServiceImpl();
        fileService.bindFileDataService(new DummyFileDataService());

        Set<BinaryReference> birefs = new HashSet<BinaryReference>();
        birefs.add(new BinaryReference(UUID.randomUUID().toString(), CompressionFormat.GZIP, "1"));

        dataRef = new DataReference(dmUuid.toString(), NodeIdentifierFactory.fromNodeId("lump:6"), birefs);
        inputStream = EasyMock.createNiceMock(InputStream.class);
        EasyMock.expect(inputStream.read()).andReturn(noOfBytes).anyTimes();
        EasyMock.expect(inputStream.read(EasyMock.aryEq(new byte[noOfBytes]),
            EasyMock.eq(0), EasyMock.eq(noOfBytes.intValue()))).andReturn(noOfBytes).anyTimes();
        EasyMock.replay(inputStream);

        metaDataBackendService = EasyMock.createNiceMock(MetaDataBackendService.class);
        EasyMock.expect(metaDataBackendService.getDataReference(dmUri)).andReturn(dataRef).anyTimes();
        EasyMock.replay(metaDataBackendService);
        fileService.bindMetadataBackendService(metaDataBackendService);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testOpenForSuccess() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        fileService.close(uuid);
        assertNotNull(uuid);
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testOpenForFailure() throws Exception {
        try {
            fileService.open(user, FileType.RCE_DM, UUID.randomUUID().toString() + "/6");
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testReadForSuccess() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        int read = fileService.read(uuid);
        assertTrue(read > 0);
        fileService.close(uuid);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testReadForFailure() throws Exception {
        try {
            fileService.read(INVALID_UUID);
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testRead2ForSuccess() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        byte[] byteArray = fileService.read(uuid, noOfBytes);
        assertNotNull(byteArray);
        fileService.close(uuid);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testRead2ForSanity() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        byte[] byteArray = fileService.read(uuid, noOfBytes);
        assertEquals(noOfBytes.intValue(), byteArray.length);
        fileService.close(uuid);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testRead2ForFailure() throws Exception {
        try {
            fileService.read(INVALID_UUID, new Integer(4));
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testCloseForSuccess() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        fileService.close(uuid);
        assertTrue(true);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testCloseForSanity() throws Exception {
        String uuid = null;

        uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        fileService.close(uuid);
        assertTrue(true);

        try {
            fileService.read(uuid, new Integer(4));
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }

        fileService.close(INVALID_UUID);
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testSkipForSuccess() throws Exception {
        String uuid = fileService.open(user, FileType.RCE_DM, dmUri);
        fileService.skip(uuid, new Long(4));
        byte[] byteArray = fileService.read(uuid, new Integer(6));
        assertNotNull(byteArray);
        fileService.close(uuid);

    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testSkipForFailure() throws Exception {
        try {
            fileService.skip(INVALID_UUID, new Long(2));
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }
    }

    /**
     * Test {@link FileDataService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyFileDataService extends FileDataServiceDefaultStub {

        @Override
        public InputStream getStreamFromDataReference(User u, DataReference dr, Boolean calledFromRemote)
            throws AuthorizationException {
            if (dr != null && dr.equals(dataRef)) {
                return inputStream;
            }
            return null;
        }

    }

}
