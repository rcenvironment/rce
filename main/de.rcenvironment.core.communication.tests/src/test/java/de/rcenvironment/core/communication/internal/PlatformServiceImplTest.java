/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.configuration.internal.NodeConfigurationServiceImpl;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.configuration.testutils.PersistentSettingsServiceDefaultStub;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test cases for {@link PlatformServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class PlatformServiceImplTest {

    private static final int EXPECTED_PLATFORM_ID_LENGTH = 32;

    private PlatformServiceImpl service;

    private BundleContext contextMock;

    /**
     * Set up.
     * 
     * @throws Exception if an error occur.
     **/
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();

        contextMock = EasyMock.createNiceMock(BundleContext.class);
        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn("bundle").anyTimes();
        EasyMock.replay(bundleMock);
        EasyMock.expect(contextMock.getBundle()).andReturn(bundleMock).anyTimes();
        EasyMock.replay(contextMock);

        NodeConfigurationServiceImpl nodeConfigurationService = new NodeConfigurationServiceImpl();
        nodeConfigurationService.bindConfigurationService(new DummyConfigurationService());
        nodeConfigurationService.bindPersistentSettingsService(new PersistentSettingsServiceDefaultStub());
        nodeConfigurationService.activate(contextMock);

        service = new PlatformServiceImpl();
        service.bindNodeConfigurationService(nodeConfigurationService);
        service.activate();
    }

    /**
     * Tests the returned {@link NodeIdentifier}.
     */
    @Test
    public void testNodeId() {
        NodeIdentifier nodeId = service.getLocalNodeId();
        // basic test: check that the persistent id is defined and of the expected length
        assertEquals(EXPECTED_PLATFORM_ID_LENGTH, nodeId.getIdString().length());
    }

    /**
     * Test {@link ConfigurationService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyConfigurationService extends MockConfigurationService.ThrowExceptionByDefault {

        @Override
        public ConfigurationSegment getConfigurationSegment(String relativePath) {
            assertEquals("network", relativePath);
            try {
                return ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(
                    "/config-tests/example1.json"));
            } catch (IOException e) {
                throw new AssertionError("Error loading configuration", e);
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

    }
}
