/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link StringUtils}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (added more test cases)
 */
public class StringUtilsTest {

    private static final String SINGLE_ALPHABETIC_CHARACTER = "x";

    /** Test. */
    @Test
    public void testEscaping() {
        final String rawString = "holter" + StringUtils.SEPARATOR + "diePolter";
        final String escapedString = "holter" + StringUtils.ESCAPE_CHARACTER + StringUtils.SEPARATOR + "diePolter";

        assertEquals(escapedString, StringUtils.escapeSeparator(rawString));
        assertEquals(rawString, StringUtils.unescapeSeparator(escapedString));

    }

    /** Test. */
    @Test
    public void testSplitAndUnescapeOutput() {
        String stringToSplit = "ka" + StringUtils.ESCAPE_CHARACTER + StringUtils.SEPARATOR + "Bumm" + StringUtils.SEPARATOR + "puffPeng";
        String[] splittedString = StringUtils.splitAndUnescape(stringToSplit);
        assertEquals(2, splittedString.length);
        assertEquals("ka" + StringUtils.SEPARATOR + "Bumm", splittedString[0]);
        assertEquals("puffPeng", splittedString[1]);
    }

    /** Test. */
    @Test
    public void testEscapeAndConcatOutput() {
        String[] parts = new String[] { "la", "le", "l:u", "\\" };

        String result = StringUtils.escapeAndConcat(parts);
        String escChar = StringUtils.SEPARATOR;
        assertEquals("la" + escChar + "le" + escChar + "l\\:u" + escChar + "\\\\", result);

    }

    /** Test of an empty string in the input. */
    @Test
    public void testConcatAndSplitWithEmptyString() {
        checkConcatAndSplit("");
    }

    /** Test of an empty string in the input. */
    @Test
    public void testConcatAndSplitWithNullString() {
        checkConcatAndSplit((String) null);
    }

    /** Test of an empty array as input. */
    @Test
    public void testConcatAndSplitWithEmptyArray() {
        checkConcatAndSplit(new String[0]);
    }

    /** Test of an array with null strings as input. */
    @Test
    public void testConcatAndSplitWithArrayContainingNullStrings() {
        checkConcatAndSplit(new String[] { null });
        checkConcatAndSplit(new String[] { null, SINGLE_ALPHABETIC_CHARACTER });
        checkConcatAndSplit(new String[] { SINGLE_ALPHABETIC_CHARACTER, null });
        checkConcatAndSplit(new String[] { null, null });
    }

    /** Test of "::". */
    @Test
    public void testConcatAndSplitWithDoubleSeparator() {
        // single two-char part
        checkConcatAndSplit(StringUtils.SEPARATOR + StringUtils.SEPARATOR);
    }

    /** Test of "\\". */
    @Test
    public void testConcatAndSplitWithDoubleEscape() {
        // single two-char part
        checkConcatAndSplit(StringUtils.ESCAPE_CHARACTER + StringUtils.ESCAPE_CHARACTER);
    }

    /**
     * Tests all 4-character combinations of a standard character, the separator, the escape
     * character, and a null string.
     */
    @Test
    public void testCombinations() {
        String[] chars = new String[] { SINGLE_ALPHABETIC_CHARACTER, StringUtils.ESCAPE_CHARACTER, StringUtils.SEPARATOR };
        for (int i1 = 0; i1 < chars.length; i1++) {
            // 1-char combinations
            checkConcatAndSplit(chars[i1]);
            for (int i2 = 0; i2 < chars.length; i2++) {
                // 2-char combinations
                checkConcatAndSplit(chars[i1] + chars[i2]);
                for (int i3 = 0; i3 < chars.length; i3++) {
                    // 3-char combinations
                    checkConcatAndSplit(chars[i1] + chars[i2] + chars[i3]);
                    for (int i4 = 0; i4 < chars.length; i4++) {
                        // extended test with 4-char combinations
                        String testString = chars[i1] + chars[i2] + chars[i3] + chars[i4];
                        String testString2 = chars[i2] + chars[i4] + chars[i1] + chars[i3];
                        checkConcatAndSplit(testString);
                        checkConcatAndSplit(testString2);
                        checkConcatAndSplit(testString, testString);
                        checkConcatAndSplit(testString, testString2);
                        checkConcatAndSplit(testString2, testString);
                    }
                }
            }
        }
    }

    private void checkConcatAndSplit(String... parts) {
        String concatenated = StringUtils.escapeAndConcat(parts);
        String[] restored = StringUtils.splitAndUnescape(concatenated);
        Assert.assertArrayEquals("Concat-and-split result was not equal for input \"" + Arrays.toString(parts) + "\", serialized form: \""
            + concatenated + "\"", (Object[]) parts, (Object[]) restored);
    }
}
