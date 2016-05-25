/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * Test cases for {@link CrossPlatformFilenameUtils}.
 *
 * @author Jan Flink
 */
public class CrossPlatformFilenameUtilsTest {

    private static final String PATH_PATTERN = "bla/blu%sb/bla%s1.txt";

    private static final String FILENAME_PATTERN = "dum%smy.txt";

    /** Test. */
    @Test
    public void testIsFilenameValid() {

        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("b?lub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("b*lub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(" blub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("     blub.txt"));

        for (String c : CrossPlatformFilenameUtils.COMMON_FORBIDDEN_FILENAME_PATTERNS) {
            assertFalse(CrossPlatformFilenameUtils.isFilenameValid(String.format(FILENAME_PATTERN, c)));
        }

        for (String c : CrossPlatformFilenameUtils.WINDOWS_FORBIDDEN_FILENAME_PATTERNS) {
            if (!c.equals("\\")) {
                assertFalse(CrossPlatformFilenameUtils.isFilenameValid(String.format(FILENAME_PATTERN, c)));
            }
        }

        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(
            String.format(FILENAME_PATTERN, StringUtils.join(CrossPlatformFilenameUtils.WINDOWS_FORBIDDEN_FILENAME_PATTERNS))));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("blub.txt"));

    }

    /** Test. */
    @Test
    public void testIsPathValid() {

        assertFalse(CrossPlatformFilenameUtils.isPathValid("b?l/ub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid("b*l\\ub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid("c:\\b*l\\ub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid(" bal/blub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid("     bla\\blub.txt"));

        for (String c : CrossPlatformFilenameUtils.WINDOWS_FORBIDDEN_FILENAME_PATTERNS) {
            if (!c.equals("\\")) {
                File x = new File(String.format(PATH_PATTERN, c, c));
                assertFalse(CrossPlatformFilenameUtils.isPathValid(x.getAbsolutePath()));
            }
        }

        assertFalse(CrossPlatformFilenameUtils.isPathValid(
            String.format(PATH_PATTERN, StringUtils.join(CrossPlatformFilenameUtils.WINDOWS_FORBIDDEN_FILENAME_PATTERNS), "*")));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i\\am\\a\\path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("/i/am/a/path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i/am/a/path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("/i/am/a/path"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i/am/a/path/"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("c:\\i\\am\\a\\path.txt"));
    }
}
