/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.TestConfigurationProvider;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;

/**
 * Manual integration tests for {@link InstanceManagementServiceImpl}.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class InstanceManagementServiceImplManualTests {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Common setup.
     * 
     * @throws Exception on uncaught errors
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Test for {@link InstanceManagementServiceImpl#fetchVersionInformationFromDownloadSourceFolder(String)}.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    public void fetchVersionInformationFromDownloadSourceFolder() throws IOException {
        InstanceManagementServiceImpl imService = new InstanceManagementServiceImpl();
        ConfigurationSegment configuration =
            ConfigurationSegmentUtils
                .readTestConfigurationFromString("{"
                    + "\"downloadSourceFolderUrlPattern\":\"http://software.dlr.de/updates/rce/6.x/products/standard/*/zip/\","
                    + "\"downloadFilenamePattern\":\"dummy-*.zip\"}");
        TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
        configurationProvider.setConfigurationSegment("instanceManagement", configuration);
        imService.bindConfigurationService(configurationProvider);
        imService.activate();

        String version = imService.fetchVersionInformationFromDownloadSourceFolder("releases/latest");
        log.debug("Identified remote version: " + version);
        assertTrue(version.startsWith("6."));
        // test: there should be no leftover whitespace
        assertEquals("found outer whitespace in returned version", version.trim(), version);
    }

    /**
     * {@link Test} for {@link InstanceManagementServiceImpl#setupInstallationFromUrlQualifier()}.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    @Ignore("needs configuration setup; disabled as quick fix")
    public void setupInstallationFromUrlQualifier() throws IOException {
        InstanceManagementServiceImpl imService = new InstanceManagementServiceImpl();
        // FIXME configure before running
        File dataRootDir = new File("_YOUR_ROOT_PATH_HERE_e:\\nb\\rce-im");
        File installationsRootDir = new File("_YOUR_ROOT_PATH_HERE_e:\\nb\\rce-im\\inst");
        ConfigurationSegment configuration =
            ConfigurationSegmentUtils
                .readTestConfigurationFromString("{"
                    + "\"dataRootDirectory\":\"" + dataRootDir.getAbsolutePath().replace("\\", "\\\\\\\\") + "\","
                    + "\"installationsRootDirectory\":\"" + installationsRootDir.getAbsolutePath().replace("\\", "\\\\\\\\") + "\","
                    + "\"downloadSourceFolderUrlPattern\":\"http://software.dlr.de/updates/rce/6.x/products/standard/*/zip/\","
                    + "\"downloadFilenamePattern\":\"rce-*-standard-win32.x86_64.zip\"}");
        TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
        configurationProvider.setConfigurationSegment("instanceManagement", configuration);
        imService.bindConfigurationService(configurationProvider);
        imService.activate();

        imService.setupInstallationFromUrlQualifier("latest-snapshot", "snapshots/trunk",
            InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT, new LoggingTextOutReceiver(""), 0);
    }
}
