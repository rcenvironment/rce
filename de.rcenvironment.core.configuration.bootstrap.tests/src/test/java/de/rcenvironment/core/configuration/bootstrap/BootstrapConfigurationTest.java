/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfileUtils;
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link BootstrapConfiguration}.
 *
 * @author Tobias Brieden
 */
public class BootstrapConfigurationTest {

    private static final String ASSERTION_FAILED_EXPECTING_PROFILE_EXCEPTION_MESSAGE = "expecting ProfileException";

    private static final String SHORT_PROFILE_FLAG = "-p";

    private static final String PROPERTY_RCE_PROFILES_PARENT_DIR = "rce.profiles.parentDir";

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TempFileService tempFileService;

    private PrintStream outOld;

    private PrintStream errOld;

    private ByteArrayOutputStream outBaos;

    private ByteArrayOutputStream errBaos;

    private String stdout;

    private String originalUserHome;

    // private String stderr;

    /**
     * Creates the test instance. Sets the user home to a temp folder.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {
        RuntimeDetection.allowSimulatedServiceActivation();
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();

        originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
    }

    /**
     * Reset the user home.
     */
    @After
    public void tearDown() {
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

    private void startCaptureSystemStreams() {
        outOld = System.out;
        errOld = System.err;

        outBaos = new ByteArrayOutputStream();
        errBaos = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outBaos));
        System.setErr(new PrintStream(errBaos));
    }

    private void endCaptureSystemStreams() {
        System.out.flush();
        System.err.flush();
        System.setOut(outOld);
        System.setErr(errOld);

        stdout = outBaos.toString();
        // stderr = errBaos.toString();
    }

    /**
     * Tests if the default profile is correctly selected if no parameters are given.
     * 
     * @throws ProfileException unexpected
     * @throws ParameterException unexpected
     * @throws IOException unexpected
     * @throws CommonProfileException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testNoParametersGiven()
        throws ProfileException, ParameterException, IOException, CommonProfileException, SystemExitException {
        // setup
        startCaptureSystemStreams();
        EclipseLaunchParameterTestUtils.simulateLaunchParameters();

        // execution
        BootstrapConfiguration.initialize();

        // reset
        endCaptureSystemStreams();

        // assertions
        Path profilePath = BootstrapConfiguration.getInstance().getProfileDirectory().toPath();
        assertTrue(profilePath.endsWith(Paths.get(".rce", "default")));
        assertThat(stdout, containsString("(use -p/--profile <id or path> to override)"));

        // check if the profile is marked as recently used
        boolean markedAsRecentlyUsed = false;
        for (Profile profile : CommonProfileUtils.getRecentlyUsedProfiles()) {
            if (profile.equals(BootstrapConfiguration.getInstance().getOriginalProfile())) {
                markedAsRecentlyUsed = true;
            }
        }
        if (!markedAsRecentlyUsed) {
            fail("The profile should be marked as recently used.");
        }
    }

    /**
     * Tests the creation of a non existing profile by supplying an absolute path.
     * 
     * @throws ProfileException unexpected
     * @throws ParameterException unexpected
     * @throws IOException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testProfilePathIsNotExisting() throws ProfileException, ParameterException, IOException, SystemExitException {
        startCaptureSystemStreams();

        File tempDir = tempFileService.createManagedTempDir();
        File profileDir = tempDir.toPath().resolve("profileDir").toFile();
        assertFalse(profileDir.exists());

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, profileDir.getAbsolutePath());

        BootstrapConfiguration.initialize();

        Path profilePath = BootstrapConfiguration.getInstance().getProfileDirectory().toPath();
        assertTrue(profilePath.endsWith("profileDir"));
        assertTrue(profileDir.exists());

        endCaptureSystemStreams();
        assertThat(stdout, not(containsString("(use -p/--profile <id or path> to override)")));
        assertThat(stdout, containsString("(as specified by the -p/--profile option)"));
    }

    /**
     * Tests if a {@link ProfileException} is correctly thrown if the given profile path points to a file.
     * 
     * @throws IOException unexpected
     * @throws ProfileException expected
     * @throws ParameterException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testProfilePathPointingToExistingFile() throws IOException, ProfileException, ParameterException, SystemExitException {
        File tempFile = tempFileService.createTempFileFromPattern("*");
        assertTrue(tempFile.exists());

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, tempFile.getAbsolutePath());

        expectedException.expect(ProfileException.class);
        expectedException.expectMessage(containsString("points to a file"));
        BootstrapConfiguration.initialize();
    }

    /**
     * Tests if a {@link ProfileException} is correctly thrown if there are conflicting profile arguments.
     * 
     * @throws ProfileException expected
     * @throws IOException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testConflicitingMixedProfilePathArguments() throws ProfileException, IOException, SystemExitException {

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, "test1", "--profile", "test2");

        try {
            BootstrapConfiguration.initialize();
            Assert.fail("expecting ParameterException");
        } catch (ParameterException e) {
            assertThat(e.getMessage(), containsString("cannot specify the same parameter several times"));
        }
    }

    /**
     * Tests if a {@link ParameterException} is correctly thrown if there are conflicting profile arguments.
     * 
     * @throws ProfileException unexpected
     * @throws IOException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testConflicitingShortProfilePathArguments() throws IOException, ProfileException, SystemExitException {

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, "test1", SHORT_PROFILE_FLAG, "test2");

        try {
            BootstrapConfiguration.initialize();
            Assert.fail("expecting ParameterException");
        } catch (ParameterException e) {
            assertThat(e.getMessage(), containsString("cannot specify the same parameter several times"));
        }
    }

    /**
     * Tests if a {@link ProfileException} is correctly thrown if the common profile should be used.
     * 
     * @throws IOException unexpected
     * @throws ParameterException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testExceptionOnCommonProfile() throws IOException, ParameterException, SystemExitException {

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, "common");

        try {
            BootstrapConfiguration.initialize();
            Assert.fail(ASSERTION_FAILED_EXPECTING_PROFILE_EXCEPTION_MESSAGE);
        } catch (ProfileException e) {
            assertThat(e.getMessage(), containsString("reserved for cross-profile settings"));
        }
    }

    /**
     * Tests if a {@link ProfileException} is correctly thrown if the common profile should be used.
     * 
     * @throws IOException unexpected
     * @throws ParameterException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testExceptionOnCommonAbsoluteProfile() throws IOException, ParameterException, SystemExitException {

        String originalProfileParentDir = TestUtils.setSystemPropertyToTempFolder(PROPERTY_RCE_PROFILES_PARENT_DIR, tempFileService);
        File file = new File(System.getProperty(PROPERTY_RCE_PROFILES_PARENT_DIR), "common");

        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, file.getAbsolutePath());

        try {
            BootstrapConfiguration.initialize();
            Assert.fail(ASSERTION_FAILED_EXPECTING_PROFILE_EXCEPTION_MESSAGE);
        } catch (ProfileException e) {
            assertThat(e.getMessage(), containsString("reserved for cross-profile settings"));
        }

        TestUtils.resetSystemPropertyToOriginal(PROPERTY_RCE_PROFILES_PARENT_DIR, originalProfileParentDir);
    }

    /**
     * Test if BootstrapConfiguration.initialize() throws a ProfileException, if profiles parent directory property is overwritten but not
     * by an absolute path.
     * 
     * @throws ParameterException expected
     * @throws IOException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testNonAbsoluteParentProfileDir() throws ParameterException, IOException, SystemExitException {

        // setup
        String originalProfileParentDir = System.getProperty(PROPERTY_RCE_PROFILES_PARENT_DIR);
        System.setProperty(PROPERTY_RCE_PROFILES_PARENT_DIR, "this is not an absolute path");
        EclipseLaunchParameterTestUtils.simulateLaunchParameters();

        try {
            BootstrapConfiguration.initialize();
            Assert.fail(ASSERTION_FAILED_EXPECTING_PROFILE_EXCEPTION_MESSAGE);
        } catch (ProfileException e) {
            assertThat(e.getMessage(), containsString("needs to be absolute"));
        }

        // reset
        TestUtils.resetSystemPropertyToOriginal(PROPERTY_RCE_PROFILES_PARENT_DIR, originalProfileParentDir);
    }

    /**
     * Tests if the system exists with error code 1 if the selected profile has an invalid version and the fallback profile is disabled.
     * 
     * @throws IOException unexpected
     * @throws ParameterException unexpected
     * @throws ProfileException unexpected
     */
    @Ignore
    @Test
    public void testInvalidProfileVersionAndFallbackDisabled() throws ParameterException, IOException, ProfileException {
        System.setProperty(BootstrapConfiguration.DRCE_LAUNCH_EXIT_ON_LOCKED_PROFILE, "");

        // create a profile directory with a future version
        File tempProfileDir = tempFileService.createManagedTempDir();
        final File internalDir = tempProfileDir.toPath().resolve("internal").toFile();
        internalDir.mkdir();
        final File versionFile = internalDir.toPath().resolve("profile.version").toFile();
        versionFile.createNewFile();
        
        try (final FileWriter fw = new FileWriter(versionFile)) {
            fw.write(String.valueOf(Profile.PROFILE_VERSION_NUMBER + 1));
        }
        
        EclipseLaunchParameterTestUtils.simulateLaunchParameters(SHORT_PROFILE_FLAG, tempProfileDir.getAbsolutePath());

        try {
            BootstrapConfiguration.initialize();
            Assert.fail(ASSERTION_FAILED_EXPECTING_PROFILE_EXCEPTION_MESSAGE);
        } catch (SystemExitException e) {
            assertEquals(1, e.getExitCode());
        }

        System.clearProperty(BootstrapConfiguration.DRCE_LAUNCH_EXIT_ON_LOCKED_PROFILE);
    }

    /**
     * Tests if a shutdown profile is used if the --shutdown option is present.
     * 
     * @throws ProfileException unexpected
     * @throws ParameterException unexpected
     * @throws CommonProfileException unexpected
     * @throws IOException unexpected
     * @throws SystemExitException unexpected
     */
    @Test
    public void testShutdownDirectoryUsed()
        throws ProfileException, ParameterException, CommonProfileException, IOException, SystemExitException {

        // setup
        EclipseLaunchParameterTestUtils.simulateLaunchParameters("--shutdown");
        startCaptureSystemStreams();

        // execution
        BootstrapConfiguration.initialize();

        // reset
        endCaptureSystemStreams();

        // assertions
        assertEquals("shutdown", BootstrapConfiguration.getInstance().getProfileDirectory().getName());
        assertThat(stdout, containsString("Using shutdown profile."));

        // check that neither the shutdown profile nor the original profile is marked as recently used
        for (Profile profile : CommonProfileUtils.getRecentlyUsedProfiles()) {

            if (profile.equals(BootstrapConfiguration.getInstance().getOriginalProfile())) {
                fail("The unused original profile should not be marked as recently used.");
            }

            if (profile.getName().contains("shutdown")) {
                fail("The shutdown profile should not be marked as recently used.");
            }
        }
    }
}
