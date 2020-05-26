/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.endsWith;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.TestUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link ProfileUtils}.
 *
 * @author Tobias Brieden
 */
public class ProfileUtilsTest {

    private TempFileService tempFileService;

    /**
     * Creates the test instance.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
    }

    /**
     * If the listProfiles method is called with a specified directory, the method should not create profiles if there are empty folders
     * within the given directory.
     * 
     * @throws IOException unexpected
     */
    @Test
    public void testListProfilesDoesntCreateProfilesFromEmptyFolders() throws IOException {

        // setup
        File tempDir = tempFileService.createManagedTempDir();
        File emtpyFolder = new File(tempDir, "emptyFolder");
        emtpyFolder.mkdirs();

        // assertions
        assertTrue(emtpyFolder.exists());
        assertTrue(emtpyFolder.isDirectory());
        assertEquals(0, emtpyFolder.listFiles().length);

        // execution
        ProfileUtils.listProfiles(tempDir);

        // assertions
        assertTrue(emtpyFolder.exists());
        assertTrue(emtpyFolder.isDirectory());
        assertEquals(0, emtpyFolder.listFiles().length);
    }

    // -----------------------------------------------------------------------------------
    // getDefaultProfile
    // -----------------------------------------------------------------------------------

    /**
     * Checks if the {@link ProfileUtils#getDefaultProfilePath()} method returns absolute paths.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testGetDefaultProfileReturnsAbsoluteSystemPropertyDefault() throws IOException, ProfileException {

        // setup
        File tempDir = tempFileService.createManagedTempDir();
        System.setProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH, tempDir.getAbsolutePath());

        // execution
        File returnedDefaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        assertEquals(tempDir, returnedDefaultProfile);
        assertTrue(returnedDefaultProfile.isAbsolute());

        // reset
        System.clearProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH);
    }

    /**
     * Checks if the {@link ProfileUtils#getDefaultProfilePath()} method uses the rce.profile.default correctly.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testGetDefaultProfileReturnsRelativeSystemPropertyDefault() throws ProfileException, IOException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        System.setProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH, "relativeDefault");

        // execution
        File returnedDefaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        assertEquals(profileParentDir.getCanonicalPath(), returnedDefaultProfile.getParentFile().getCanonicalPath());
        assertEquals("relativeDefault", returnedDefaultProfile.getName());
        assertTrue(returnedDefaultProfile.isAbsolute());

        // reset
        System.clearProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH);
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

    /**
     * Checks if the {@link ProfileUtils#getDefaultProfilePath()} method returns the stored default profile.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testGetDefaultProfileReturnsSavedDefault() throws IOException, ProfileException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        String originalOsgiInstallArea =
            TestUtils.setSystemPropertyToTempFolder(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, tempFileService);

        File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();
        Profile profile =
            new Profile.Builder(new File(profileParentDirectory, "someProfile")).create(true).migrate(true).buildUserProfile();
        profile.markAsDefaultProfile();

        // execution
        File defaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        assertEquals(profile.getProfileDirectory().getCanonicalPath(), defaultProfile.getCanonicalPath());

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
        TestUtils.resetSystemPropertyToOriginal(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, originalOsgiInstallArea);
    }

    /**
     * Checks that the {@link ProfileUtils#getDefaultProfilePath()} method does not return the stored default profile if it does not exist
     * (because it was deleted).
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testGetDefaultProfileDoesntReturnsSavedDefault() throws IOException, ProfileException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        String originalOsgiInstallArea =
            TestUtils.setSystemPropertyToTempFolder(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, tempFileService);

        File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();
        Profile profile =
            new Profile.Builder(new File(profileParentDirectory, "someProfile")).create(true).migrate(true).buildUserProfile();
        profile.markAsDefaultProfile();

        FileUtils.deleteDirectory(profile.getProfileDirectory());

        // assertions
        assertFalse(profile.getProfileDirectory().exists());

        // execution
        File defaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        assertThat(defaultProfile.getCanonicalPath(), not(profile.getProfileDirectory().getCanonicalPath()));
        assertThat(defaultProfile.getCanonicalPath(), endsWith("default"));

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
        TestUtils.resetSystemPropertyToOriginal(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, originalOsgiInstallArea);
    }

    /**
     * Checks if the {@link ProfileUtils#getDefaultProfilePath()} method returns the implicit default if nothing else is specified.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testGetDefaultProfileReturnsImplicitDefault() throws ProfileException, IOException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);

        // execution
        File returnedDefaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        assertEquals(profileParentDir.getCanonicalPath(), returnedDefaultProfile.getParentFile().getCanonicalPath());
        assertEquals("default", returnedDefaultProfile.getName());
        assertTrue(returnedDefaultProfile.isAbsolute());

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

    // -----------------------------------------------------------------------------------
}
