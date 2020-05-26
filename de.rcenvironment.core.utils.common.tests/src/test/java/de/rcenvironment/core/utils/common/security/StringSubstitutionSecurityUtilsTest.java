/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.security;

import static de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils.isSafeForSubstitutionInsideDoubleQuotes;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils.SubstitutionContext;

/**
 * Unit test for {@link StringSubstitutionSecurityUtils}.
 * 
 * @author Robert Mischke
 */
public class StringSubstitutionSecurityUtilsTest {

    private static final String SIMPLE_TEST_STRING = "test";

    private static final String FANCY_TEST_STRING = " te_34 st (23) + ";

    private static final String SINGLE_QUOTE = "'";

    /**
     * Test cleanup.
     */
    @After
    public void tearDown() {
        // reset test flag
        StringSubstitutionSecurityUtils.setSuppressLogMessageOnDeniedSubstitution(false);
    }

    /**
     * Test proper rejection of null parameters.
     */
    @Test
    public void testNullParameters() {
        // null string
        for (SubstitutionContext context : SubstitutionContext.values()) {
            try {
                isSafeForSubstitutionInsideDoubleQuotes(null, context);
                fail("Exception expected for null string");
            } catch (NullPointerException e) {
                assertTrue(true);
            }
        }
        // null context
        try {
            isSafeForSubstitutionInsideDoubleQuotes(SIMPLE_TEST_STRING, null);
            fail("Exception expected for null context");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    /**
     * Test context-independent cases.
     */
    @Test
    public void testSharedCases() {
        for (SubstitutionContext context : SubstitutionContext.values()) {
            testIsSafeForSubstitution(true, "", context);
            testIsSafeForSubstitution(true, SIMPLE_TEST_STRING, context);

            // test method sanity checks
            testSubstringInStartMiddleAndEnd(true, "x", context);
            testSubstringInStartMiddleAndEnd(true, " ", context);
            testSubstringInStartMiddleAndEnd(true, "0", context);

            testSubstringInStartMiddleAndEnd(false, "\"", context);
            testSubstringInStartMiddleAndEnd(false, "\\", context);
            testSubstringInStartMiddleAndEnd(false, "/", context);
            testSubstringInStartMiddleAndEnd(false, "*", context);
            testSubstringInStartMiddleAndEnd(false, "?", context);
            testSubstringInStartMiddleAndEnd(false, "\n", context);
            testSubstringInStartMiddleAndEnd(false, "\r", context);
            testSubstringInStartMiddleAndEnd(false, "\rn", context);
            testSubstringInStartMiddleAndEnd(false, "\t", context);
            testSubstringInStartMiddleAndEnd(false, "\0", context);
            testSubstringInStartMiddleAndEnd(false, "" + '\u0000', context); // using char instead of string to rule out typos
            testSubstringInStartMiddleAndEnd(false, "" + '\u001f', context); // using char instead of string to rule out typos
        }
    }

    /**
     * Test patterns that are only forbidden in Windows batch files.
     */
    @Test
    public void testWindowsBatchSpecificCases() {
        testSubstringInStartMiddleAndEnd(false, "%", SubstitutionContext.WINDOWS_BATCH);
        // single quote is only forbidden in Jython context
        testSubstringInStartMiddleAndEnd(true, SINGLE_QUOTE, SubstitutionContext.WINDOWS_BATCH);
    }

    /**
     * Test patterns that are only forbidden in Linux bash scripts.
     */
    @Test
    public void testLinuxBashSpecificCases() {
        // backtick character
        testSubstringInStartMiddleAndEnd(false, "" + '\u0060', SubstitutionContext.LINUX_BASH); // char instead of string to rule out typos
        testSubstringInStartMiddleAndEnd(false, "$", SubstitutionContext.LINUX_BASH);
        testSubstringInStartMiddleAndEnd(false, "${", SubstitutionContext.LINUX_BASH);
        testSubstringInStartMiddleAndEnd(true, "{", SubstitutionContext.LINUX_BASH);
        testSubstringInStartMiddleAndEnd(true, "}", SubstitutionContext.LINUX_BASH);
        testSubstringInStartMiddleAndEnd(false, "$(", SubstitutionContext.LINUX_BASH);
        // single quote is only forbidden in Jython context
        testSubstringInStartMiddleAndEnd(true, SINGLE_QUOTE, SubstitutionContext.LINUX_BASH);
    }

    /**
     * Test patterns that are only forbidden in Jython scripts.
     */
    @Test
    public void testJythonSpecificCases() {
        testSubstringInStartMiddleAndEnd(false, SINGLE_QUOTE, SubstitutionContext.JYTHON);
    }

    private void testSubstringInStartMiddleAndEnd(boolean expected, String substring, SubstitutionContext testContext) {
        testIsSafeForSubstitution(expected, substring + SIMPLE_TEST_STRING, testContext);
        testIsSafeForSubstitution(expected, substring + FANCY_TEST_STRING, testContext);
        testIsSafeForSubstitution(expected, "pre" + substring + "post", testContext);
        testIsSafeForSubstitution(expected, FANCY_TEST_STRING + substring + FANCY_TEST_STRING, testContext);
        testIsSafeForSubstitution(expected, SIMPLE_TEST_STRING + substring, testContext);
        testIsSafeForSubstitution(expected, FANCY_TEST_STRING + substring, testContext);
    }

    private void testIsSafeForSubstitution(boolean expected, String testString, SubstitutionContext testContext) {
        // suppress "denied substitution" message if this is the correct and expected result
        StringSubstitutionSecurityUtils.setSuppressLogMessageOnDeniedSubstitution(!expected);
        boolean result = isSafeForSubstitutionInsideDoubleQuotes(testString, testContext);
        StringSubstitutionSecurityUtils.setSuppressLogMessageOnDeniedSubstitution(false);
        if (expected != result) {
            // only build message on failure
            Assert.fail(StringUtils.format("Test case \"%s\" failed in %s context: expected '%s', received '%s'",
                testString, testContext, expected, result));
        }
    }

}
