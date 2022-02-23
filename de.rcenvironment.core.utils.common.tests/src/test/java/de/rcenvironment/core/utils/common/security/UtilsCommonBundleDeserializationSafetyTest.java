/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

/**
 * Checks for potential security issues related to deserialization of data received from external sources, running in the classpath of the
 * "utils.common" bundle.
 * 
 * @author Robert Mischke
 */
public class UtilsCommonBundleDeserializationSafetyTest extends AbstractDeserializationClasspathCheck {

    /**
     * Checks the current classpath for classes known or suspected to be unsafe for deserialization of external data.
     * 
     * @throws IOException
     */
    @Test
    public void testForKnownUnsafeClassesInClasspath() {

        boolean unsafeClassFound = checkForKnownUnsafeClassesInClasspath();

        assertFalse("Found at least one known unsafe or suspicious class in the available classpath; check log output for details",
            unsafeClassFound);
    }

}
