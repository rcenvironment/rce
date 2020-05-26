/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.TestUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link CommonProfileUtils}.
 *
 * @author Tobias Brieden
 */
public class CommonProfileUtilsTest {

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TempFileService tempFileService;

    private String originalUserHome;

    private String originalOsgiInstallArea;

    /**
     * Creates the test instance. Sets the user home to a temp folder.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {

        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();

        originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        originalOsgiInstallArea =
            TestUtils.setSystemPropertyToTempFolder(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, tempFileService);
    }

    /**
     * Reset the user home.
     */
    @After
    public void tearDown() {
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
        TestUtils.resetSystemPropertyToOriginal(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, originalOsgiInstallArea);
    }

    /**
     * Tests if the markAsDefaultProfile method can be called multiple times without an error.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testCallMarkAsDefaultProfileTwice() throws ProfileException, CommonProfileException, IOException {
        // create a new profile and mark is as recently used
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        File newProfileDirectory = new File(profileParentDir, "randomProfileName");
        Profile newProfile = new Profile.Builder(newProfileDirectory).create(true).migrate(false).buildUserProfile();
        CommonProfileUtils.markAsDefaultProfile(newProfile);
        CommonProfileUtils.markAsDefaultProfile(newProfile);
    }

    /**
     * Tests if the lock on the common profile is released even if an exception is thrown while assessing the common profile.
     * 
     * @throws IOException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testIfLockIsReleasedEvenIfAnExceptionIsThrown() throws IOException, CommonProfileException {

        // execution
        try {
            CommonProfileUtils.lockExecuteRelease(new CommonProfileUtils.Command<Void>() {

                @Override
                public Void execute() throws CommonProfileException {
                    throw new CommonProfileException("test");
                }

            });

            fail("expected exception");
        } catch (CommonProfileException e) {
            // execute another method of the CommonProfileUtils to check if the lock was released even though an exception was thrown and
            // this method can get the lock.

            CommonProfileUtils.clearDefaultProfile();
        }
    }

    /**
     * Create a first profile and mark it as recently used. Check if the list of recently used profiles contains this profile afterwards.
     * 
     * @throws CommonProfileException unexpected
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testMarkProfileAsRecentlyUsed() throws CommonProfileException, IOException, ProfileException {

        // assertions
        List<Profile> recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();
        assertTrue(recentlyUsedProfiles.isEmpty());

        // create a new profile and mark is as recently used
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        File newProfileDirectory = new File(profileParentDir, "test1");
        Profile newProfile = new Profile.Builder(newProfileDirectory).create(true).migrate(true).buildUserProfile();
        newProfile.markAsRecentlyUsed();

        // assertions
        recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();
        assertEquals(1, recentlyUsedProfiles.size());
        assertThat(recentlyUsedProfiles, hasItem(newProfile));
    }

    /**
     * Create multiple new profiles mark them as recently used. Check if the order in the list of recently used profiles is correct
     * afterwards.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testOrderOfRecentlyUsedProfiles() throws IOException, ProfileException, CommonProfileException {
        // create multiple new profiles and mark is as recently used
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        Profile newProfile1 = new Profile.Builder(new File(profileParentDir, "test1")).create(true).buildUserProfile();
        Profile newProfile2 = new Profile.Builder(new File(profileParentDir, "test2")).create(true).buildUserProfile();
        Profile newProfile3 = new Profile.Builder(new File(profileParentDir, "test3")).create(true).buildUserProfile();
        newProfile1.markAsRecentlyUsed();
        newProfile3.markAsRecentlyUsed();
        newProfile2.markAsRecentlyUsed();

        // execution
        List<Profile> recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();

        // assertions
        assertEquals(3, recentlyUsedProfiles.size());
        assertEquals(newProfile2, recentlyUsedProfiles.get(0));
        assertEquals(newProfile3, recentlyUsedProfiles.get(1));
        assertEquals(newProfile1, recentlyUsedProfiles.get(2));
    }

    /**
     * If a profile is marked as recently used but then deleted (e.g. by the user on the file system) the profile shouln't be listed as
     * recently used anymore.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testGetRecentlyUsedProfilesWithDeletedProfile() throws IOException, ProfileException, CommonProfileException {

        // create a new profile, mark it as recently used and then delete the profile
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        Profile newProfile = new Profile.Builder(new File(profileParentDir, "test")).create(true).migrate(true).buildUserProfile();
        newProfile.markAsRecentlyUsed();
        FileUtils.deleteDirectory(newProfile.getProfileDirectory());

        // execution
        List<Profile> recentlyUsedProfiles = CommonProfileUtils.getRecentlyUsedProfiles();

        // assertions
        assertEquals(0, recentlyUsedProfiles.size());
    }

    /**
     * Tests if {@link CommonProfileUtils#getRecentlyUsedProfiles()} throws a {@link CommonProfileException} if the file storing the
     * information cannot be accessed.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException expected
     */
    @Test
    public void testGetRecentlyUsedThrowsExceptionIfAccessProblem() throws IOException, ProfileException, CommonProfileException {

        // create a directory named recentlyUsed which should be a file in normal operations
        final File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        final File commonProfileDir = profileParentDir.toPath().resolve("common").toFile();
        CommonProfile commonProfile =
            new CommonProfile.Builder(commonProfileDir).create(true).migrate(false).buildCommonProfile();
        File recentlyUsedFile = commonProfile.getProfileDirectory().toPath().resolve("profiles").resolve("recentlyUsed").toFile();
        assertTrue(recentlyUsedFile.mkdirs());
        assertTrue(recentlyUsedFile.isDirectory());

        // execution
        expectedException.expect(CommonProfileException.class);
        expectedException.expectMessage("Unable to read the list of recently used profiles");
        CommonProfileUtils.getRecentlyUsedProfiles();
    }

    /**
     * Tests if the default profile clear method can be called even if no file is available.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testClearDefaultProfileWithoutPriorStorage() throws IOException, ProfileException, CommonProfileException {

        // execution
        CommonProfileUtils.clearDefaultProfile();
    }

    /**
     * Tests if the default profile selection can be cleared.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     * @throws CommonProfileException unexpected
     */
    @Test
    public void testClearDefaultProfile() throws IOException, ProfileException, CommonProfileException {

        File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();
        Profile profile =
            new Profile.Builder(new File(profileParentDirectory, "someProfile")).create(true).migrate(true).buildUserProfile();
        profile.markAsDefaultProfile();

        // execution
        CommonProfileUtils.clearDefaultProfile();
        File defaultProfile = ProfileUtils.getDefaultProfilePath();

        // assertions
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        assertEquals(profileParentDir.getCanonicalPath(), defaultProfile.getParentFile().getCanonicalPath());
        assertEquals("default", defaultProfile.getName());
        assertTrue(defaultProfile.isAbsolute());
    }
}
