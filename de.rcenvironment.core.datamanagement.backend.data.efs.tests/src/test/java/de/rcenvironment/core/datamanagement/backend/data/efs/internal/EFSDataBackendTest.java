/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.data.efs.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.easymock.EasyMock;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;

/**
 * Test cases for {@link EFSDataBackend}.
 * 
 * @author Doreen Seider
 */
public class EFSDataBackendTest {

    private EFSDataBackend efsDataBackend;

    private EFSDataBackendConfiguration config;

    private IFileStore fileStore = EasyMock.createNiceMock(IFileStore.class);

    private URI uri;

    private InputStream is;

    private InputStream brokenIs;

    private OutputStream os;

    private UUID uuid = UUID.randomUUID();

    private String storage = "src/test/resources";

    private String bundleName = "ick mag dir";

    private int writtenBytes = 9;

    /**
     * Set up.
     * 
     * @throws Exception if an error occurs.
     **/
    @Before
    public void setUp() throws Exception {

        uri = new EFSDataBackend().suggestLocation(uuid);
        config = new EFSDataBackendConfiguration();
        config.setEfsStorage(storage);

        fileStore = EasyMock.createNiceMock(IFileStore.class);
        EasyMock.expect(fileStore.getParent()).andReturn(fileStore).anyTimes();
        is = EasyMock.createNiceMock(GZIPInputStream.class);
        final int bufferSize = 256 * 1024;
        final int endOfRead = -1;
        byte[] buffer = new byte[bufferSize];

        EasyMock.expect(is.read(EasyMock.aryEq(buffer))).andReturn(writtenBytes);
        EasyMock.expect(is.read(EasyMock.aryEq(buffer))).andReturn(endOfRead);
        EasyMock.replay(is);
        EasyMock.expect(fileStore.openInputStream(EFS.NONE, null)).andReturn(is);
        brokenIs = EasyMock.createNiceMock(InputStream.class);
        EasyMock.expect(brokenIs.read(EasyMock.aryEq(buffer))).andThrow(new IOException());
        EasyMock.replay(brokenIs);
        EasyMock.expect(fileStore.openInputStream(EFS.NONE, null)).andReturn(brokenIs);
        os = EasyMock.createNiceMock(OutputStream.class);
        EasyMock.expect(fileStore.openOutputStream(EFS.NONE, null)).andReturn(os).anyTimes();
        EasyMock.expect(fileStore.getName()).andReturn(uuid.toString()).anyTimes();
        EasyMock.expect(fileStore.childNames(EFS.NONE, null)).andReturn(new String[] {}).anyTimes();
        EasyMock.replay(fileStore);

        efsDataBackend = new EFSDataBackend();
        efsDataBackend.bindConfigurationService(new DummyConfigurationService());
        efsDataBackend.bindEncapsulatedEFSService(new DummyEncapsulatedEFSService());

        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn(bundleName).anyTimes();
        EasyMock.replay(bundle);
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.replay(bundleContext);
        efsDataBackend.activate(bundleContext);

    }

    /** Test. */
    @Test
    public void testGet() {

        try {
            efsDataBackend.get(new URI("yeah://besser"));
            fail();
        } catch (URISyntaxException e) {
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /** Test. */
    @Test
    public void testPut() {
        try {
            efsDataBackend.put(uri, (Object) brokenIs, false);
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            efsDataBackend.put(uri, new Object(), false);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            efsDataBackend.put(new URI("och://du"), (Object) is, false);
            fail();
        } catch (URISyntaxException e) {
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /** Test. */
    @Test
    public void testSuggestLocation() {
        assertNotNull(efsDataBackend.suggestLocation(UUID.randomUUID()));
    }

    /** Test. */
    @Test
    public void testDelete() {
        // create file (on base of URI) in file system to delete
        String dirPath = new File(config.getEfsStorage()).getAbsoluteFile().getAbsolutePath();
        File dir = new File(dirPath);
        dir.mkdirs();
        File file = new File(dirPath + File.separator + uuid);
        try {
            file.createNewFile();
        } catch (IOException e) {
            fail();
        }

        assertTrue(efsDataBackend.delete(uri));

        try {
            efsDataBackend.delete(new URI("jap://gehtSo"));
            fail();
        } catch (URISyntaxException e) {
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        file.delete();
        dir.delete();

        assertFalse(efsDataBackend.delete(uri));
    }

    /**
     * Test implementation of {@link ConfigurationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyConfigurationService extends MockConfigurationService.ThrowExceptionByDefault {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getConfiguration(String identifier, Class<T> clazz) {
            return (T) config;
        }

        @Override
        public File getProfileDirectory() {
            return new File(System.getProperty("java.io.tmpdir"), "unittest-temp"); // TODO improve
        }

        @Override
        public File initializeSubDirInConfigurablePath(ConfigurablePathId pathId, String relativePath) {
            return new File(config.getEfsStorage());
        }

    }

    /**
     * Test implementation of {@link EncapsulatedEFSService}.
     * 
     * @author Doreen Seider
     */
    private class DummyEncapsulatedEFSService implements EncapsulatedEFSService {

        @Override
        public IFileStore getStore(URI location) throws CoreException {
            return fileStore;
        }

    }
}
