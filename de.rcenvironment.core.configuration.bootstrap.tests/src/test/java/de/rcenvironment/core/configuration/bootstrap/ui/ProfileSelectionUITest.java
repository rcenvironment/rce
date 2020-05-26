/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.Key.Kind;

import de.rcenvironment.core.configuration.bootstrap.TestUtils;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link ProfileSelectionUI}.
 *
 * @author Tobias Brieden
 */
public class ProfileSelectionUITest {

    private TempFileService tempFileService;

    private LanternaTestUtils lanternaTest;

    /**
     * Creates the test instance.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();

        lanternaTest = new LanternaTestUtils();
        Runnable uiRunnable = new Runnable() {

            @Override
            public void run() {
                try {
                    new ProfileSelectionUI(lanternaTest.getTestTerminal()).run();
                } catch (ProfileException e) {
                    fail(e.getMessage());
                }
            }
        };
        lanternaTest.setUiRunnable(uiRunnable);
    }

    // TODO why does this test take 1 second?
    /**
     * Tests if an appropriate error message is displayed if list of recently used profiles cannot be loaded.
     * 
     * @throws IOException unexpected
     * @throws ProfileException unexpected
     */
    @Test
    public void testErrorLabelIsDisplayedIfMostRecentlyUsedProfilesCannotBeLoaded() throws IOException, ProfileException {

        // setup
        String originalUserHome = TestUtils.setSystemPropertyToTempFolder(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, tempFileService);
        // create a directory named recentlyUsed which should be a file in normal operations
        File profileParentDir = ProfileUtils.getProfilesParentDirectory();
        File recentlyUsedFile = profileParentDir.toPath().resolve("common").resolve("profiles").resolve("recentlyUsed").toFile();
        recentlyUsedFile.mkdirs();
        assertTrue(recentlyUsedFile.isDirectory());

        // create the assertion and navigate to the location where the error label should be displayed
        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        assertions.add(TerminalStub.containsString("Unable to load most recently used profiles."));
        lanternaTest.addTestStage(new Key(Kind.Enter), assertions, 1);

        // exit the UI
        lanternaTest.addTestStage(new Key(Kind.Escape), 1);
        lanternaTest.addTestStage(new Key(Kind.ArrowDown), 1);
        lanternaTest.addTestStage(new Key(Kind.ArrowDown), 1);
        lanternaTest.addTestStage(new Key(Kind.Enter), 1);

        lanternaTest.executeTestsWithTimeout(5, TimeUnit.SECONDS);

        // reset
        TestUtils.resetSystemPropertyToOriginal(ProfileUtils.SYSTEM_PROPERTY_USER_HOME, originalUserHome);
    }

    /**
     * Tests if an error message is displayed correctly if the profiles parent directory is invalid; it needs to be a directory; in this
     * test we try to use a file instead.
     * 
     * @throws IOException unexpected
     */
    @Test
    public void testErrorIsDisplayedIfProfilesParentDirectoryIsInvalid() throws IOException {

        // setup
        File tempFile = tempFileService.createTempFileFromPattern("*");
        System.setProperty(ProfileUtils.SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE, tempFile.getAbsolutePath());

        // create the assertion and navigate to the location where the error label should be displayed
        Queue<Matcher<char[][]>> assertions = new LinkedList<Matcher<char[][]>>();
        assertions.add(TerminalStub.containsString("The profiles parent directory cannot be created or it is not"));
        assertions.add(TerminalStub.containsString("a directory."));
        lanternaTest.addTestStage(new Key(Kind.Enter), assertions, 1);

        lanternaTest.executeTests();

        // reset
        System.clearProperty(ProfileUtils.SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE);
    }
}
