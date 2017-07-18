/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration.bootstrap.profile;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link BaseProfile}.
 *
 * @author Tobias Brieden
 */
public class BaseProfileTest {

    private static final int ARBITRARY_VERSION_NUMBER = 23;

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

        expectedException.expect(ProfileException.class);
        new BaseProfile(profileDir, ARBITRARY_VERSION_NUMBER, true);
    }
}
