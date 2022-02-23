/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Test cases for {@link CrossPlatformFilenameUtils}.
 *
 * @author Jan Flink
 * @author Tobias Rodehutskors
 */
public class CrossPlatformFilenameUtilsTest {

    private static final String PATH_PATTERN = "bla/blu%sb/bla%s1.txt";

    private static final String FILENAME_PATTERN = "dum%smy.txt";

    private static final int LAST_ASCII_CTRL_INDEX = 31;

    private static final int FORBIDDEN_LENGTH = 260;

    private static final char ESCAPED_BACKSLASH = '\\';

    /** Test. */
    @Test
    public void testIsFilenameValid() {
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("."));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(".."));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("..."));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("...."));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("a.."));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("a."));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("..a"));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid(".a"));

        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("b?lub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid("b*lub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid(" blub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("     blub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("     blu b.txt"));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("     blu b.txt "));

        for (char c : CrossPlatformFilenameUtils.FORBIDDEN_CHARACTERS) {
            if (c != ESCAPED_BACKSLASH) {

                assertFalse(CrossPlatformFilenameUtils.isFilenameValid(StringUtils.format(FILENAME_PATTERN, String.valueOf(c))));
            }
        }

        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(
            StringUtils.format(FILENAME_PATTERN, new String(CrossPlatformFilenameUtils.FORBIDDEN_CHARACTERS))));
        assertTrue(CrossPlatformFilenameUtils.isFilenameValid("blub.txt"));

        // there is a bunch of filenames which are forbidden, even though they are composed of valid characters...
        for (String forbiddenFilename : CrossPlatformFilenameUtils.FORBIDDEN_WINDOWS_FILENAMES) {
            assertFalse(CrossPlatformFilenameUtils.isFilenameValid(forbiddenFilename));
        }

        // ...these names are also not allowed in combination with a file extension...
        for (String forbiddenFilename : CrossPlatformFilenameUtils.FORBIDDEN_WINDOWS_FILENAMES) {
            assertFalse(CrossPlatformFilenameUtils.isFilenameValid(forbiddenFilename + ".txt"));
        }

        // ...but they can still be part of a filename
        for (String forbiddenFilename : CrossPlatformFilenameUtils.FORBIDDEN_WINDOWS_FILENAMES) {
            assertTrue(CrossPlatformFilenameUtils.isFilenameValid(forbiddenFilename + "test"));
            assertTrue(CrossPlatformFilenameUtils.isFilenameValid(forbiddenFilename + "test.txt"));
            assertTrue(CrossPlatformFilenameUtils.isFilenameValid("test" + forbiddenFilename + ".txt"));
        }

        // there is also a length limitation for filenames on Windows
        String longfilename = new String(new char[FORBIDDEN_LENGTH]).replace("\0", "a");
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(longfilename));

        // the first characters of the ASCII table are control characters and not allowed either
        for (int i = 0; i <= LAST_ASCII_CTRL_INDEX; i++) {
            assertFalse(CrossPlatformFilenameUtils.isFilenameValid(StringUtils.format(FILENAME_PATTERN, (char) i)));
        }

        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(StringUtils.format(FILENAME_PATTERN, "\b")));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(StringUtils.format(FILENAME_PATTERN, "\0")));
        assertFalse(CrossPlatformFilenameUtils.isFilenameValid(StringUtils.format(FILENAME_PATTERN, "\t")));

    }

    /** Test. */
    @Test
    public void testIsPathValid() {

        assertFalse(CrossPlatformFilenameUtils.isPathValid("b?l/ub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid("b*l\\ub.txt"));
        assertFalse(CrossPlatformFilenameUtils.isPathValid("c:\\b*l\\ub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid(" bal/blub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("     bla\\blub.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("b la\\blub.txt"));

        for (char c : CrossPlatformFilenameUtils.FORBIDDEN_CHARACTERS) {
            if (c != ESCAPED_BACKSLASH && c != '/') {
                File x = new File(StringUtils.format(PATH_PATTERN, c, c));
                assertFalse(CrossPlatformFilenameUtils.isPathValid(x.getAbsolutePath()));
            }
        }

        assertFalse(CrossPlatformFilenameUtils.isPathValid("/i/am/CON/path.txt"));

        assertFalse(CrossPlatformFilenameUtils.isPathValid(
            StringUtils.format(PATH_PATTERN, org.apache.commons.lang3.StringUtils.join(CrossPlatformFilenameUtils.FORBIDDEN_CHARACTERS),
                "*")));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i\\am\\a\\path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("/i/am/a/path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i/am/a/path.txt"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("/i/am/a/path"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("i/am/a/path/"));
        assertTrue(CrossPlatformFilenameUtils.isPathValid("c:\\i\\am\\a\\path.txt"));

    }
    
    /** Test. */
    @Test
    public void testIsNFSFile() {
        assertTrue(CrossPlatformFilenameUtils.isNFSFile(".nfs1234"));
        assertTrue(CrossPlatformFilenameUtils.isNFSFile(".nfs000000000095a01200000e8"));
        assertFalse(CrossPlatformFilenameUtils.isNFSFile("test.nfs1234"));
        assertFalse(CrossPlatformFilenameUtils.isNFSFile("test.nfs000000000095a01200000e8"));
    }
}
