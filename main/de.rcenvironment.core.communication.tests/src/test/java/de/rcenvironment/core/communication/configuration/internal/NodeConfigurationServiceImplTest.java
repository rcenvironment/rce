/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration.internal;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NodeIdentityInformation;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.configuration.testutils.PersistentSettingsServiceDefaultStub;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Unit test for {@link NodeConfigurationServiceImpl}.
 * 
 * @author Doreen Seider (original PlatformServiceImpl tests)
 * @author Robert Mischke
 */
public class NodeConfigurationServiceImplTest {

    private static final int EXPECTED_PLATFORM_ID_LENGTH = 32;

    private NodeConfigurationServiceImpl service;

    /**
     * Test {@link ConfigurationService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke
     * @author Brigitte Boden (added SSH config segment)
     */
    private class DummyConfigurationService extends MockConfigurationService.ThrowExceptionByDefault {

        @Override
        public ConfigurationSegment getConfigurationSegment(String relativePath) {
            if ("network".equals(relativePath)) {
                try {
                    return ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(
                        "/config-tests/example1.json"));
                } catch (IOException e) {
                    throw new AssertionError("Error loading configuration", e);
                }
            } else if ("sshRemoteAccess".equals(relativePath)) {
                try {
                    return ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(
                        "/config-tests/exampleSsh.json"));
                } catch (IOException e) {
                    throw new AssertionError("Error loading configuration", e);
                }
            } else {
                fail("relativePath must be \"network\" or \"ssh\"");
                return null;
            }
        }

        @Override
        public boolean getIsWorkflowHost() {
            return false;
        }

        @Override
        public boolean getIsRelay() {
            return false;
        }

        @Override
        public String getInstanceName() {
            return "";
        }

        @Override
        public double[] getLocationCoordinates() {
            return new double[] { 0.0, 0.0 };
        }

        @Override
        public String getLocationName() {
            return "";
        }

        @Override
        public String getInstanceContact() {
            return "";
        }

        @Override
        public String getInstanceAdditionalInformation() {
            return "";
        }
    }

    /**
     * Test setup.
     * 
     * @throws Exception on unexpected exceptions
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();

        BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);
        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn("bundle").anyTimes();
        EasyMock.replay(bundleMock);
        EasyMock.expect(contextMock.getBundle()).andReturn(bundleMock).anyTimes();
        EasyMock.replay(contextMock);

        service = new NodeConfigurationServiceImpl();
        service.bindConfigurationService(new DummyConfigurationService());
        service.bindPersistentSettingsService(new PersistentSettingsServiceDefaultStub());
        service.activate(contextMock);
    }

    /**
     * Tests the returned {@link NodeIdentityInformation}.
     */
    @Test
    public void testGetIdentityInformation() {
        InitialNodeInformation initialNodeInformation = service.getInitialNodeInformation();
        // basic test: check that the persistent id is defined and of the expected length
        assertEquals(EXPECTED_PLATFORM_ID_LENGTH, initialNodeInformation.getNodeIdString().length());
    }

}
