/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * {@link SshAccountConfigurationServiceImpl} unit tests.
 * 
 * @author Robert Mischke
 */
public class SshAccountConfigurationServiceImplTest {

    private ConfigurationSegment configurationSegment;

    /**
     * Subclasses the tested service to inject custom configuration loading.
     * 
     * @author Robert Mischke
     */
    private class AdaptedSshAccountConfigurationServiceImpl extends SshAccountConfigurationServiceImpl {

        AdaptedSshAccountConfigurationServiceImpl() {
            ConfigurationService confServiceMock = EasyMock.createMock(ConfigurationService.class);
            EasyMock.expect(confServiceMock.isUsingIntendedProfileDirectory()).andReturn(Boolean.TRUE);
            // the configuration fallback in case of invalid configuration data is not part of this test so far
            EasyMock.expect(confServiceMock.isUsingDefaultConfigurationValues()).andReturn(Boolean.FALSE);
            EasyMock.replay(confServiceMock);
            bindConfigurationService(confServiceMock);
        }

        @Override
        protected ConfigurationSegment loadSshConfigurationSegment() {
            return configurationSegment;
        }
    }

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Tests initial state checks with an empty configuration file.
     */
    @Test
    public void testEmptyConfig() {
        setConfigurationSegment("empty");
        AdaptedSshAccountConfigurationServiceImpl testServive = new AdaptedSshAccountConfigurationServiceImpl();
        String error = testServive.verifyExpectedStateForConfigurationEditing();
        assertNull(error, error);
    }

    private void setConfigurationSegment(String configFileName) {
        try {
            configurationSegment =
                ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(
                    "/configuration/" + configFileName + ".json"));

        } catch (IOException e) {
            fail("Failed to load test config: " + e);
        }
    }

}
