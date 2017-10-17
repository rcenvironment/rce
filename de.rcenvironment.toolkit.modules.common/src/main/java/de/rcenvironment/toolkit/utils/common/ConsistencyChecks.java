/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

/**
 * A utility class for performing explicit assertions (a.k.a. "sanity checks" or "internal consistency checks"). If such a check fails, a
 * subclass of {@link Error} is thrown, which is not expected to be caught and handled anywhere, as it represents an abnormal internal
 * state.
 * <p>
 * It is important to note that this class is <em>only</em> intended for checks that are <em>never</em> expected to fail in normal
 * operation. For example, it <em>is</em> ok to use this class to verify that
 * <ul>
 * <li>the output of a structured format string (for example, a standardized id) actually has the expected total length;
 * <li>parameters of a certain <em>internal</em> method are not null on invocation (testing the expectation that the caller has already
 * sanitized it);
 * <li>a received {@link Enum} parameter actually has one of the known values (by calling {@link #reportFailure(String)} if none of them
 * matched).
 * </ul>
 * <p>
 * However, it is <em>not</em> ok to use this class to verify expectations that may be invalid at runtime, e.g.
 * <ul>
 * <li>that an expected field or property exists is present in configuration data;
 * <li>that a received network message has a certain length or format (unless other code has checked this already);
 * <li>certain I/O conditions that may be wrong if other processes are concurrently deleting files, or when running out of disk space.
 * </ul>
 * 
 * In other words, only use these checks to guard against undetected programming errors in a controlled way.
 * 
 * @author Robert Mischke
 * 
 */
public final class ConsistencyChecks {

    private ConsistencyChecks() {}

    /**
     * @param value the condition expected to be true
     */
    public static void assertTrue(boolean value) {
        if (!value) {
            reportFailure("Expected expression to be true");
        }
    }

    /**
     * @param value the condition expected to be true
     * @param message a custom message to print on failure
     */
    public static void assertTrue(boolean value, String message) {
        if (!value) {
            reportFailure(message);
        }
    }

    /**
     * @param value the condition expected to be false
     */
    public static void assertFalse(boolean value) {
        if (value) {
            reportFailure("Expected expression to be false");
        }
    }

    /**
     * @param value the condition expected to be false
     * @param message a custom message to print on failure
     */
    public static void assertFalse(boolean value, String message) {
        if (value) {
            reportFailure(message);
        }
    }

    /**
     * @param value the object reference to be non-null
     */
    public static void assertNotNull(Object value) {
        if (value == null) {
            reportFailure("Unexpected null reference");
        }
    }

    /**
     * @param value the object reference to be non-null
     * @param message a custom message to print on failure
     */
    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            reportFailure(message);
        }
    }

    /**
     * Explicitly triggers a validation failure,as if one of the other methods was called with an unexpected parameter. This is useful for
     * more complex checks, or for avoiding the unnecessary construction of complex error message parameters before the actual check.
     * 
     * @param detailMessage the message to include in the error message
     */
    public static void reportFailure(String detailMessage) {
        throw new AssertionError("Internal consistency error: " + detailMessage);
    }
}
