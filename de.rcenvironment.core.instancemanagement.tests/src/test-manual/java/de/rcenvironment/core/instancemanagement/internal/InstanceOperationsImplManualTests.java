/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.instancemanagement.InstanceStatus;
import de.rcenvironment.core.instancemanagement.InstanceStatus.InstanceState;
import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Integration tests for {@link InstanceOperationsImpl} that are not intended to be run within the automated test battery, usually because
 * it requires certain external resources.
 * 
 * @author Robert Mischke
 * @author Lukas Rosenbach
 */
public class InstanceOperationsImplManualTests {

    private TestParametersProvider testParameters;

    /**
     * Common setup.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Before
    public void setUp() throws Exception {
        testParameters = new ParameterizedTestUtils().readDefaultPropertiesFile(getClass());
    }

    /**
     * Common teardown.
     * 
     * @throws Exception on uncaught exceptions
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Tests the basic start/stop cycle with a provided external installation.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void startStopRoundTrip() throws IOException {

        // must exist
        final File installationDir = testParameters.getExistingDir("startStopRoundTrip.installationDir");
        // may exist
        final File profileDir = testParameters.getDefinedFileOrDir("startStopRoundTrip.profileDir");
        List<File> profileDirList = new ArrayList<>();
        profileDirList.add(profileDir);
        // optional repetitions, e.g. for stability testing
        final int repetitions = testParameters.getOptionalInteger("startStopRoundTrip.repetitions", 1);

        assertTrue(profileDir.getPath(), profileDir.isAbsolute()); // doesn't have to exist, but should be absolute for reliable starting
        assertTrue(installationDir.getPath(), installationDir.isAbsolute());
        assertTrue(installationDir.getPath(), installationDir.isDirectory());

        InstanceOperationsImpl instanceOperations = new InstanceOperationsImpl();

        assertFalse(InstanceOperationsUtils.isProfileLocked(profileDir));
        
        ConcurrentMap<String, InstanceStatus> profileIdToInstanceStatusMap = new ConcurrentHashMap<>();
        for (File profile : profileDirList) {
            profileIdToInstanceStatusMap.put(profile.getName(), new InstanceStatus(installationDir.getName(), InstanceState.NOTRUNNING));
        }
        
        for (int i = 0; i < repetitions; i++) {

            instanceOperations.startInstanceUsingInstallation(profileDirList, installationDir, profileIdToInstanceStatusMap,
                    0, null, false);
            assertTrue("profile not locked after start", InstanceOperationsUtils.isProfileLocked(profileDir));

            instanceOperations.shutdownInstance(profileDirList, 0, null);
            assertFalse("profile still locked after shutdown", InstanceOperationsUtils.isProfileLocked(profileDir));
        }
    }
}
