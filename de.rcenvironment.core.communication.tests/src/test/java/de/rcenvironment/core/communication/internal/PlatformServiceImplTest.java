/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.impl.NodeIdentifierServiceImpl;
import de.rcenvironment.core.communication.configuration.internal.NodeConfigurationServiceImpl;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.MockConfigurationService;
import de.rcenvironment.core.configuration.testutils.PersistentSettingsServiceDefaultStub;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * Test cases for {@link PlatformServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class PlatformServiceImplTest {

    private static final int EXPECTED_NODE_ID_LENGTH = CommonIdBase.INSTANCE_PART_LENGTH + 1 + CommonIdBase.SESSION_PART_LENGTH;

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
        nodeConfigurationService.bindNodeIdentifierService(new NodeIdentifierServiceImpl(IdGeneratorType.FAST));
        nodeConfigurationService.activate(contextMock);

        service = new PlatformServiceImpl();
        service.bindNodeConfigurationService(nodeConfigurationService);
        service.activate();
    }

    /**
     * Tests the returned {@link InstanceNodeSessionId}.
     */
    @Test
    public void testNodeId() {
        InstanceNodeSessionId nodeId = service.getLocalInstanceNodeSessionId();
        // basic test: check that the persistent id is defined and of the expected length
        assertEquals(CommonIdBase.INSTANCE_SESSION_ID_STRING_LENGTH, nodeId.getInstanceNodeSessionIdString().length());
    }

    /**
     * Test {@link ConfigurationService} implementation.
     * 
     * @author Doreen Seider
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
}
