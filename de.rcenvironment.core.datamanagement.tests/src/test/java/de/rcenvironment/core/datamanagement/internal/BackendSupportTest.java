/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.datamanagement.backend.DataBackend;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;

/**
 * Tests cases for {@link BackendSupport}.
 * 
 * @author Juergen Klein
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class BackendSupportTest {

    private static final String CLOSE_BRACKET = ")";

    private static final String OPEN_BRACKET = "(";

    private static final String EQUALS_SIGN = "=";

    private static String xmlBackendProvider = "snoopy";

    private static String fileBackendProvider = "linus";

    private static String dataScheme = "ftp";

    private static String catalogBackendProvider = "de.rcenvironment.core.datamanagement.backend.metadata.derby";

    private MetaDataBackendService catalogBackend = EasyMock.createNiceMock(MetaDataBackendService.class);

    private DataBackend dataBackend;

    private BackendSupport backendSupport;

    /** Set up. */
    @Before
    public void setUp() {
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

    /** Test. */
    @Test
    public void testGetCatalogBackend() {
        assertEquals(catalogBackend, BackendSupport.getMetaDataBackend());
    }

    /** Test. */
    @Test
    public void testGetDataBackend() {
        URI dataUri = null;
        try {
            dataUri = new URI("ftp://eieiei");
        } catch (URISyntaxException e) {
            fail();
        }
        assertEquals(dataBackend, BackendSupport.getDataBackend(dataUri));

        try {
            dataUri = new URI("efs://eieiei");
        } catch (URISyntaxException e) {
            fail();
        }
        try {
            BackendSupport.getDataBackend(dataUri);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }

        assertEquals(dataBackend, BackendSupport.getDataBackend());
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

}
