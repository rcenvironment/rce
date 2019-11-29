/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import junit.framework.TestCase;

/**
 * Unit test for the Assertions class.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 */
public class AssertionsTest extends TestCase {

    /**
     * The exception error message text.
     */
    private static final String EXCEPTION_NOT_THROWN_TEXT = "Exception not thrown.";

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isTrue(boolean, java.lang.String)}.
     */
    public void testIsTrueForSuccess() {
        Boolean a = true;
        Assertions.isTrue(a, "The parameter a should be true but was false.");
        Assertions.isTrue(a, "");
        Assertions.isTrue(a, null);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isFalse(boolean, java.lang.String)}.
     */
    public void testIsFalseForSuccess() {
        Boolean a = false;
        Assertions.isFalse(a, "The parameter a should be false but was true.");
        Assertions.isFalse(a, "");
        Assertions.isFalse(a, null);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isEqual(int, int, java.lang.String)}.
     */
    public void testIsEqualForSuccess() {
        int a = 4;
        int b = 4;
        Assertions.isEqual(a, b, "The integer b should be equal to " + a + " but was : " + b);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isBiggerThan(int, int, java.lang.String)}.
     */
    public void testIsBiggerThanForSuccess() {
        int a = 5;
        int b = 3;
        Assertions.isBiggerThan(a, b, "The parameter a should be bigger then : " + b);
        
        long c = 5;
        Assertions.isBiggerThan(c, b, "The parameter a should be bigger then : " + b);
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isDefined(java.lang.Object, java.lang.String)}.
     */
    public void testIsDefinedForSuccess() {
        String a = new String("testus numerus");
        Assertions.isDefined(a, "The parameter a should not be null or empty. Please proved a usefull String.");
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isNull(java.lang.Object, java.lang.String)}.
     */
    public void testIsNullForSuccess() {
        String a = null;
        Assertions.isNull(null, "The parameter a must be null, but was : " + a);
    }

    /*
     * Test for failure.
     */

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isTrue(boolean, java.lang.String)}.
     */
    public void testIsTrueForFailure() {
        try {
            Assertions.isTrue(false, "The parameter a should be true but was false.");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isTrue(false, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isTrue(false, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isFalse(boolean, java.lang.String)}.
     */
    public void testIsFalseForFailure() {
        try {
            Assertions.isFalse(true, "The parameter a should be false but was true.");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isFalse(true, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isFalse(true, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isEqual(int, int, java.lang.String)}.
     */
    public void testIsEqualForFailure() {
        try {
            Assertions.isEqual(4, 6, "The integer b should be " + 4 + " but was : " + 6);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isEqual(2, 4, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isEqual(2, 3, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isBiggerThan(int, int, java.lang.String)}.
     */
    public void testIsBiggerThanForFailure() {
        try {
            Assertions.isBiggerThan(4, 6, "The parameter a=4 should be bigger then : " + 6);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isBiggerThan(2, 3, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            Assertions.isBiggerThan(1, 2, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isDefined(java.lang.Object, java.lang.String)}.
     */
    public void testIsDefinedForFailure() {
        // test for empty string
        try {
            Assertions.isDefined("", "The string a should neither be null or empty");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isDefined("", "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isDefined("", null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
        // test for null pointer
        try {
            Assertions.isDefined(null, "The string a should neither be null or empty");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isDefined(null, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isDefined(null, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Test method for
     * {@link de.rcenvironment.core.utils.incubator.sdk.util.Assertions#isNull(java.lang.Object, java.lang.String)}.
     */
    public void testIsNullForFailure() {
        final String testStr = "test";
        
        try {
            Assertions.isNull(testStr, "The parameter a should be null but was : " + testStr);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isNull(testStr, "");
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            Assertions.isNull(testStr, null);
            fail(EXCEPTION_NOT_THROWN_TEXT);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /*
     * Test for sanity.
     */

    // Nothing to do here. Either it throws errors or not :-)
}
