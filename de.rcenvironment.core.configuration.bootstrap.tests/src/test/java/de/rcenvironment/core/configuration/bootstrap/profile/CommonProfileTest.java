/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link CommonProfile}.
 *
 * @author Tobias Brieden
 */
public class CommonProfileTest {

    private static final String COMMON_PROFILE_NAME = "common";

    private static final int CURRENT_VERSION_NUMBER = 2;

    private static final int PAST_VERSION_NUMBER = 1;

    private static final int FUTURE_VERSION_NUMBER = 4;

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
     * Tests that a profile is successfully created if the profile folder does not yet exist and the user requests both creation of
     * non-existing profiles and migration of older profiles.
     * 
     * @throws IOException Thrown when the setup of the test environment fails. Unexpected.
     * @throws ProfileException Thrown when the creation of the profile instance fails. Unexpected.
     */
    @Test
    public void testProfileCreationSuccessWithMigration() throws IOException, ProfileException {
        final File tempDir = tempFileService.createManagedTempDir();
        final ProfileRepresentation profileRepresentation = ProfileRepresentation.create(tempDir, COMMON_PROFILE_NAME);

        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(true).migrate(true).buildCommonProfile();

        assertTrue(profileRepresentation.getProfileDir().isDirectory());
        assertTrue(profileRepresentation.getInternalDir().isDirectory());
        assertTrue(profileRepresentation.getVersionFile().isFile());
        final byte[] expectedContent = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        final byte[] actualContent = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals(expectedContent, actualContent);
    }

    /**
     * Tests that a profile is successfully created if the profile folder does not yet exist and the user requests creation of non-existing
     * profiles, but does not request migration of older profiles.
     * 
     * @throws IOException Thrown when the setup of the test environment fails. Unexpected.
     * @throws ProfileException Thrown when the creation of the profile instance fails. Unexpected.
     */
    @Test
    public void testProfileCreationSuccessWithoutMigration() throws IOException, ProfileException {
        final File tempDir = tempFileService.createManagedTempDir();
        final ProfileRepresentation profileRepresentation = ProfileRepresentation.create(tempDir, COMMON_PROFILE_NAME);

        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(true).migrate(false).buildCommonProfile();

        assertTrue(profileRepresentation.getProfileDir().isDirectory());
        assertTrue(profileRepresentation.getInternalDir().isDirectory());
        assertTrue(profileRepresentation.getVersionFile().isFile());
        final byte[] expectedContent = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        final byte[] actualContent = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals(expectedContent, actualContent);
    }

    /**
     * Tests that a profile is not successfully created if the given profile directory does not exist, but the user also does not request
     * its creation.
     * 
     * @throws IOException Thrown when the setup of the test environment fails. Unexpected.
     * @throws ProfileException Thrown when the creation of the profile instance fails. Expected.
     */
    @Test
    public void testProfileCreationSuccessWithoutCreation() throws IOException, ProfileException {
        final File tempDir = tempFileService.createManagedTempDir();
        final ProfileRepresentation profileRepresentation = ProfileRepresentation.create(tempDir, COMMON_PROFILE_NAME);

        expectedException.expect(ProfileException.class);
        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(false).buildCommonProfile();
    }

    /**
     * Tests that a profile is not successfully created if the given profile directory does not exist, but the user also does not request
     * its creation.
     * 
     * @throws IOException Thrown when the setup of the test environment fails. Unexpected.
     * @throws ProfileException Thrown when the creation of the profile instance fails. Expected.
     */
    @Test
    public void testProfileCreationStartupLogs() throws IOException, ProfileException {
        final File tempDir = tempFileService.createManagedTempDir();
        final ProfileRepresentation profileRepresentation = ProfileRepresentation.create(tempDir, COMMON_PROFILE_NAME);
        profileRepresentation.tryCreateProfileDir();
        profileRepresentation.getProfileDir().toPath().resolve("startup_logs").toFile().mkdir();

        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(true).migrate(false).buildCommonProfile();

        assertTrue(profileRepresentation.getProfileDir().isDirectory());
        assertTrue(profileRepresentation.getInternalDir().isDirectory());
        assertTrue(profileRepresentation.getVersionFile().isFile());
        final byte[] expectedContent = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        final byte[] actualContent = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals(expectedContent, actualContent);
    }

    /**
     * Tests if a profile directory is rejected if it denotes a file.
     * 
     * @throws IOException unexpected
     * @throws ProfileException expected
     */
    @Test
    public void testInvalidProfileDirectory() throws IOException, ProfileException {
        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve("file").toFile();
        assertTrue(profileDir.createNewFile());

        ProfileException exception = null;
        try {
            new CommonProfile.Builder(profileDir).create(false).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            exception = e;
        }
        assertNotNull(exception);

        exception = null;
        try {
            new CommonProfile.Builder(profileDir).create(true).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    /**
     * Tests that a legacy profile is automatically upgraded to a non-legacy profile of the current version when setting the create-flag to
     * true upon creation.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment
     * @throws ProfileException Expected to be thrown during profile creation
     */
    @Test
    public void testLegacyProfileUpgradeSuccess() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME);
        profileRepresentation.tryCreateProfileDir().tryCreateInternalDir();

        final File profileDir = profileRepresentation.getProfileDir();
        new CommonProfile.Builder(profileDir).create(false).migrate(true).buildCommonProfile();

        File versionFile = profileRepresentation.getVersionFile();
        assertTrue(versionFile.exists());

        final byte[] expectedContent = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        final byte[] actualContent = Files.readAllBytes(versionFile.toPath());
        assertArrayEquals(expectedContent, actualContent);
    }

    /**
     * Tests that opening a legacy profile is rejected if the user does not want to silently upgrade the profile upon creation.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment
     * @throws ProfileException Expected to be thrown during profile creation
     */
    @Test
    public void testLegacyProfileUpgradeFailure() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir();

        expectedException.expect(ProfileException.class);
        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(false).buildCommonProfile();
    }

    /**
     * Tests that attempting to open a profile with a corrupted version file fails, whether or not the user allows creating the file if it
     * already exists.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment
     */
    @Test
    public void testCorruptedProfile() throws IOException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile("asdf");

        // Test profile creation without enforced upgrade
        ProfileException thrownException = null;
        try {
            new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            thrownException = e;
        }
        assertNotNull(thrownException);

        // Test profile creation with enforced upgrade
        thrownException = null;
        try {
            new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(true).buildCommonProfile();
        } catch (ProfileException e) {
            thrownException = e;
        }
        assertNotNull(thrownException);
    }

    /**
     * Tests that attempting to create a profile that is already represented on the file system with a version number lower than the given
     * one succeeds whether or not the user allows migration of older profiles.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment. Not expected.
     * @throws ProfileException Thrown if there is an error during profile creation. Not expected.
     */
    @Test
    public void testPastProfileVersion() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation = ProfileRepresentation
            .create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
            .tryCreateProfileDir()
            .tryCreateInternalDir()
            .tryCreateVersionFile(PAST_VERSION_NUMBER);

        new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(false).buildCommonProfile();
    }

    /**
     * Tests that attempting to create a profile that is already represented on the file system with a version number lower than the given
     * one succeeds if the user allows migration of older profiles.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment
     * @throws ProfileException Expected to be thrown during profile creation
     */
    @Test
    public void testOutdatedProfileMigration() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile(PAST_VERSION_NUMBER);

        final CommonProfile profile =
            new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(true).buildCommonProfile();
        assertEquals(CURRENT_VERSION_NUMBER, profile.getVersion());

        final byte[] expectedVersionOnFileSystem = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        final byte[] actualVersionOnFileSystem = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals(expectedVersionOnFileSystem, actualVersionOnFileSystem);
    }

    /**
     * Tests that creating an object whose representation on the file system is of the expected version works without errors.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment. Not expected.
     * @throws ProfileException Thrown if creation of the profile fails. Not expected.
     */
    @Test
    public void testCurrentProfile() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile(CURRENT_VERSION_NUMBER);

        final File profileDir = profileRepresentation.getProfileDir();
        final File versionFile = profileRepresentation.getVersionFile();

        final CommonProfile unmigratedProfile = new CommonProfile.Builder(profileDir).create(false).migrate(false).buildCommonProfile();
        assertEquals(CURRENT_VERSION_NUMBER, unmigratedProfile.getVersion());
        final byte[] expectedVersionOnFileSystem = String.valueOf(CURRENT_VERSION_NUMBER).getBytes();
        byte[] actualVersionOnFileSystem = Files.readAllBytes(versionFile.toPath());
        assertArrayEquals(expectedVersionOnFileSystem, actualVersionOnFileSystem);

        final CommonProfile migratedProfile = new CommonProfile.Builder(profileDir).create(false).migrate(true).buildCommonProfile();
        assertEquals(CURRENT_VERSION_NUMBER, migratedProfile.getVersion());
        actualVersionOnFileSystem = Files.readAllBytes(versionFile.toPath());
        assertArrayEquals(expectedVersionOnFileSystem, actualVersionOnFileSystem);
    }


    /**
     * Tests that creating an object whose representation on the file system is of the expected version works without errors. In contrast to
     * testCurrentProfile, the profile version file in this test contains trailing newlines in order to simulate a manual manipulation of
     * this file. This trailing newline should, however, be silently ignored.
     * 
     * @throws IOException      Thrown if there is an error during preparation of the test environment. Not expected.
     * @throws ProfileException Thrown if creation of the profile fails. Not expected.
     */
    @Test
    public void testCurrentProfileWithNewlineVersionFile() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile("2\n");

        final File profileDir = profileRepresentation.getProfileDir();
        final File versionFile = profileRepresentation.getVersionFile();

        final CommonProfile unmigratedProfile = new CommonProfile.Builder(profileDir).create(false).migrate(false).buildCommonProfile();
        assertEquals(CURRENT_VERSION_NUMBER, unmigratedProfile.getVersion());
        final byte[] expectedVersionOnFileSystem = "2\n".getBytes();
        byte[] actualVersionOnFileSystem = Files.readAllBytes(versionFile.toPath());
        assertArrayEquals(expectedVersionOnFileSystem, actualVersionOnFileSystem);

        final CommonProfile migratedProfile = new CommonProfile.Builder(profileDir).create(false).migrate(true).buildCommonProfile();
        assertEquals(CURRENT_VERSION_NUMBER, migratedProfile.getVersion());
        actualVersionOnFileSystem = Files.readAllBytes(versionFile.toPath());
        assertArrayEquals(expectedVersionOnFileSystem, actualVersionOnFileSystem);
    }

    /**
     * Tests that creating an object whose representation on the file system is of a newer version than the most current one fails if the
     * user does not request migration of older profile versions.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment. Not expected.
     * @throws ProfileException Expected exception.
     */
    @Test
    public void testFutureProfileNoMigration() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile(FUTURE_VERSION_NUMBER);

        ProfileException exception = null;
        try {
            new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(false).buildCommonProfile();
        } catch (ProfileException e) {
            exception = e;
        }
        assertNotNull("Profile creation backed by a future version of a profile succeeded unexpectedly", exception);

        final byte[] expectedVersionOnFileSystem = String.valueOf(FUTURE_VERSION_NUMBER).getBytes();
        byte[] actualVersionOnFileSystem = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals("Profile creation backed by a future version changed the contents of the version file",
            expectedVersionOnFileSystem, actualVersionOnFileSystem);
    }

    /**
     * Tests that creating an object whose representation on the file system is of a newer version than the most current one fails if the
     * user requests migration of older profile versions.
     * 
     * @throws IOException Thrown if there is an error during preparation of the test environment. Not expected.
     * @throws ProfileException Expected exception.
     */
    @Test
    public void testFutureProfileMigration() throws IOException, ProfileException {
        final ProfileRepresentation profileRepresentation =
            ProfileRepresentation.create(tempFileService.createManagedTempDir(), COMMON_PROFILE_NAME)
                .tryCreateProfileDir()
                .tryCreateInternalDir()
                .tryCreateVersionFile(FUTURE_VERSION_NUMBER);

        ProfileException exception = null;
        try {
            new CommonProfile.Builder(profileRepresentation.getProfileDir()).create(false).migrate(true).buildCommonProfile();
        } catch (ProfileException e) {
            exception = e;
        }
        assertNotNull("Profile creation backed by a future version of a profile succeeded unexpectedly", exception);

        final byte[] expectedVersionOnFileSystem = String.valueOf(FUTURE_VERSION_NUMBER).getBytes();
        byte[] actualVersionOnFileSystem = Files.readAllBytes(profileRepresentation.getVersionFile().toPath());
        assertArrayEquals("Profile creation backed by a future version changed the contents of the version file",
            expectedVersionOnFileSystem, actualVersionOnFileSystem);
    }
}
