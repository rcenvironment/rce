/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
     * Test of {@link StringUtils#checkAgainstCommonInputRules(String)}.
     */
    @Test
    public void testCommonValidInputSet() {
        assertNull(StringUtils.checkAgainstCommonInputRules(""));

        // verify positive examples
        final String validTestChars = "azAZ09 .,-+_()";
        assertNull(StringUtils.checkAgainstCommonInputRules(validTestChars));

        final String validSingleChars = "azAZ_09";
        for (char c : validSingleChars.toCharArray()) {
            String testString = Character.toString(c);
            assertNull(testString, StringUtils.checkAgainstCommonInputRules(testString));
        }

        // check: spaces should not be allowed as first or last char
        assertEquals(StringUtils.COMMON_VALID_INPUT_FIRST_CHARACTER_ERROR, StringUtils.checkAgainstCommonInputRules(" " + validTestChars));
        assertEquals(StringUtils.COMMON_VALID_INPUT_LAST_CHARACTER_ERROR, StringUtils.checkAgainstCommonInputRules(validTestChars + " "));

        // test counter-examples
        // TODO replace non-ascii characters with unicode values for robustness
        // TODO add more exotic characters
        final String forbiddenTestChars = "\0\t\n:;~*?!<>|äöüßÄÖÜ@";
        final int expectedCharCount = 20;
        assertEquals(StringUtils.COMMON_VALID_INPUT_CHARSET_ERROR, StringUtils.checkAgainstCommonInputRules(forbiddenTestChars));
        assertEquals(expectedCharCount, forbiddenTestChars.toCharArray().length); // prevent accidents during string->char splitting
        for (char c : forbiddenTestChars.toCharArray()) {
            String testString = Character.toString(c);
            assertEquals(testString, StringUtils.COMMON_VALID_INPUT_CHARSET_ERROR, StringUtils.checkAgainstCommonInputRules(testString));
        }
    }

    /** Test fault tolerant implementation of {@link String#format(String, Object...)}. */
    @Test
    public void testStringFormat() {
        String formatString = "%s %b %d";
        String stringValue = "RCE";
        boolean booleanValue = true;
        int intValue = 5;

        String nullFormatString = null;
        String stringNullValue = null;
        Boolean booleanNullValue = null;
        Integer integerNullValue = null;

        String anyStringValue = "red green blue";

        String expectedResultString = stringValue + " " + booleanValue + " " + intValue;

        String result = StringUtils.format(formatString);
        assertEquals(formatString, result);

        result = StringUtils.format(formatString, stringValue);
        System.err.println(result);
        assertEquals(formatString + StringUtils.FORMAT_SEPARATOR + stringValue, result);

        result = StringUtils.format(formatString, stringValue, booleanValue);
        assertEquals(formatString + StringUtils.FORMAT_SEPARATOR + stringValue + StringUtils.FORMAT_SEPARATOR + booleanValue, result);

        result = StringUtils.format(formatString, stringValue, booleanValue, intValue);
        assertEquals(expectedResultString, result);

        result = StringUtils.format(formatString, stringValue, booleanValue, intValue, anyStringValue);
        assertEquals(expectedResultString, result);

        result = StringUtils.format(formatString, stringNullValue);
        assertEquals(formatString + StringUtils.FORMAT_SEPARATOR + stringNullValue, result);

        result = StringUtils.format(formatString, null, null);
        assertEquals(formatString + StringUtils.FORMAT_SEPARATOR + stringNullValue
            + StringUtils.FORMAT_SEPARATOR + stringNullValue, result);

        result = StringUtils.format(formatString, stringNullValue, booleanNullValue, integerNullValue);
        assertEquals(stringNullValue + " " + Boolean.valueOf(null) + " " + integerNullValue, result);
    }

    /**
     * Tests all 4-character combinations of a standard character, the separator, the escape character, and a null string.
     */
    @Test
    public void testCombinations() {
        String[] chars = new String[] { SINGLE_ALPHABETIC_CHARACTER, StringUtils.ESCAPE_CHARACTER, StringUtils.SEPARATOR };
        for (String c : chars) {
            // 1-char combinations
            checkConcatAndSplit(c);
            for (String d : chars) {
                // 2-char combinations
                checkConcatAndSplit(c + d);
                for (String e : chars) {
                    // 3-char combinations
                    checkConcatAndSplit(c + d + e);
                    for (String f : chars) {
                        // extended test with 4-char combinations
                        String testString = c + d + e + f;
                        String testString2 = d + f + c + e;
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
            + concatenated + "\"", parts, restored);
    }
}
