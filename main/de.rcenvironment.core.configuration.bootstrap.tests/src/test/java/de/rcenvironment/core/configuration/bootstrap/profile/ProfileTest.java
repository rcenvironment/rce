/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.bootstrap.TestUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link Profile}.
 *
 * @author Tobias Brieden
 */
public class ProfileTest {

    private static final int ARBITRARY_VERSION_NUMBER = 23;

    private static final String ARBITRARY_PROFILE_NAME = "profileDir";

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
     * Tests if the constructor sets the version number.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testConstructorSetsVersionNumber() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve(ARBITRARY_PROFILE_NAME).toFile();

        Profile profile = new Profile(profileDir, ARBITRARY_VERSION_NUMBER, true);
        assertEquals(ARBITRARY_VERSION_NUMBER, profile.getVersion());
    }

    /**
     * Tests if the constructor correctly throws a ProfileException if the given directory does not already contain a valid profile and it
     * should not create it.
     * 
     * @throws IOException unexpected
     * @throws ProfileException expected
     */
    @Test
    public void testConstructorThrowsProfileExceptionOnEmptyProfileDir() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve(ARBITRARY_PROFILE_NAME).toFile();

        expectedException.expect(ProfileException.class);
        expectedException.expectMessage("does not contain an existing profile");
        // do not create the profile if the folder does not already contain a valid one
        new Profile(profileDir, false);
    }

    /**
     * Tests if the constructor correctly throws a ProfileException if the given directory is not empty and does not contain a valid
     * profile, but it should be created.
     * 
     * @throws IOException unexpected
     * @throws ProfileException expected
     */
    @Test
    public void testConstructorThrowsProfileExceptionOnNotEmptyProfileDir() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve(ARBITRARY_PROFILE_NAME).toFile();
        assertTrue(profileDir.mkdirs());
        assertTrue(profileDir.toPath().resolve("file").toFile().createNewFile());

        expectedException.expect(ProfileException.class);
        expectedException.expectMessage("is neither an existing profile directory nor is it empty");
        new Profile(profileDir);
    }

    /**
     * Older versions of RCE (< 7.0) created profiles without a profile.version file. If such a profile is started now, the profile.version
     * file should be created.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testConstructorCreatesProfileVersionFileOnLegacyProfiles() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve(ARBITRARY_PROFILE_NAME).toFile();
        assertTrue(profileDir.mkdirs());
        assertTrue(profileDir.toPath().resolve("file").toFile().createNewFile());

        Profile profile = new Profile(profileDir, true, true);
        assertTrue(profile.getInternalDirectory().toPath().resolve("profile.version").toFile().exists());
    }

    /**
     * If the same profile is twice attempted to be locked a {@link ProfileException} is throws.
     * 
     * @throws IOException unexpected
     * @throws ProfileException expected
     */
    @Test
    public void testAttemptToLockTwice() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve(ARBITRARY_PROFILE_NAME).toFile();

        Profile profile = new Profile(profileDir, ARBITRARY_VERSION_NUMBER, true);
        assertTrue(profile.attemptToLockProfileDirectory());

        expectedException.expect(ProfileException.class);
        expectedException.expectMessage("when trying to acquire a file lock");
        profile.attemptToLockProfileDirectory();
    }

    /**
     * Tests if the location dependent name is correctly constructed if the profile is located within the profile parent directory.
     * 
     * @throws IOException unexpected.
     * @throws ProfileException unexpected.
     */
    @Test
    public void testLocationDependentNameWithinProfileParentDir() throws IOException, ProfileException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        File profileParentDirectory = ProfileUtils.getProfilesParentDirectory();
        Profile profile = new Profile(new File(profileParentDirectory, "someProfile"));

        // assertion
        assertEquals("someProfile", profile.getLocationDependentName());

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

    /**
     * Tests if the location dependent name is correctly constructed if the profile is located outside the profile parent directory.
     * 
     * @throws IOException unexpected.
     * @throws ProfileException unexpected.
     */
    @Test
    public void testLocationDependentNameOutsideProfileParentDir() throws IOException, ProfileException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        File randomDir = tempFileService.createManagedTempDir("someTempFolder");
        Profile profile = new Profile(new File(randomDir, "anotherProfile"));

        // assertion
        assertEquals(profile.getProfileDirectory().getAbsolutePath(), profile.getLocationDependentName());

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

}
