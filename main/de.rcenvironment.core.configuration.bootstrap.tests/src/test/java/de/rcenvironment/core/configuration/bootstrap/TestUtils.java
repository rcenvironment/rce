/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.utils.common.TempFileService;

/**
 * Utility methods for testing.
 * 
 * @author Tobias Brieden
 */
public final class TestUtils {

    private TestUtils() {}

    /**
     * Creates a temp. folder and sets the folder's absolute path as the value of the system property.
     * 
     * @param systemProperty The system property.
     * @param tempFileService The reference to {@link TempFileService}.
     * @return The previous value of the system property.
     * @throws IOException If the creating of the temp. folder failed.
     */
    public static String setSystemPropertyToTempFolder(String systemProperty, TempFileService tempFileService) throws IOException {
        File tempDir = tempFileService.createManagedTempDir();
        String originalValue = System.setProperty(systemProperty, tempDir.getAbsolutePath());
        return originalValue;
    }

    /**
     * Sets the value of a system property.
     * 
     * @param systemProperty The system property.
     * @param originalValue The desired value. If <code>null</code> is given, the system property is cleared.
     */
    public static void resetSystemPropertyToOriginal(String systemProperty, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(systemProperty);
        } else {
            System.setProperty(systemProperty, originalValue);
        }
    }
}
