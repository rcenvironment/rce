/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.launcher.internal;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link RCELauncherCustomization}.
 *
 * @author Tobias Brieden
 * @author Robert Mischke (adaptations)
 */
public class RCELauncherCustomizationTest {

    private static final String PROBLEMATIC_SUBPATH = " ++_hallo&";

    private static final String RELATIVE_PATH = "configuration";

    private TempFileService tempFileService;

    /**
     * Creates the test instance. Sets the user home to a temp folder.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
    }

    /**
     * Tests if the SetConfigurationLocationAbsolute method works if the osgi.install.area path contains unescaped spaces.
     * 
     * @throws IOException unexpected
     */
    @Test
    public void testSetConfigurationLocationAbsoluteWithUnescapedSpaces() throws IOException {

        // setup
        System.setProperty(RCELauncherCustomization.SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION, RELATIVE_PATH);
        File tmpDir = tempFileService.createManagedTempDir();
        File problematicDir = tmpDir.toPath().resolve(PROBLEMATIC_SUBPATH).toFile();
        assertTrue(problematicDir.mkdir());
        assertTrue(problematicDir.isDirectory());
        assertTrue(problematicDir.exists());
        // We use toURL here because it does not escape properly!
        System.setProperty(RCELauncherCustomization.SYSTEM_PROPERTY_KEY_OSGI_INSTALL_AREA, problematicDir.toURL().toString());

        // initialize (precondition for next call)
        RCELauncherCustomization.initialize(new String[0]);

        // execute configuration processing hook; should resolve to an absolute configuration location
        RCELauncherCustomization.hookAfterInitialConfigurationProcessing();

        // assertions
        assertThat(System.getProperty(RCELauncherCustomization.SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION), not(RELATIVE_PATH));
        assertThat(System.getProperty(RCELauncherCustomization.SYSTEM_PROPERTY_KEY_BUNDLE_CONFIGURATION_LOCATION),
            endsWith(PROBLEMATIC_SUBPATH + File.separator + RELATIVE_PATH));

    }
}
