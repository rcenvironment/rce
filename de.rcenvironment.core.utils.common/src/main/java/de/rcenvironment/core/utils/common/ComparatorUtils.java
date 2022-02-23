/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

/**
 * Provides common utility methods for {@link Comparator} writing.
 * 
 * @author Robert Mischke
 */
public abstract class ComparatorUtils {

    /**
     * A constant for returning a positive integer in {@link Comparator#compare(Object, Object)}. Usually needed to satisfy Checkstyle.
     */
    public static final int POSITIVE_INT = 1;

    /**
     * A constant for returning a negative integer in {@link Comparator#compare(Object, Object)}. Usually needed to satisfy Checkstyle.
     */
    public static final int NEGATIVE_INT = -1;

    private ComparatorUtils() {}

    /**
     * Tests whether two float numbers differ by at most "delta".
     * 
     * @param a value 1
     * @param b value 2
     * @param delta the maximum delta
     * @return true if abs(a-b) < delta
     */
    public static boolean floatEqualWithinDelta(float a, float b, float delta) {
        return Math.abs(a - b) < delta;
    }

    /**
     * Tests whether two double numbers differ by at most "delta".
     * 
     * @param a value 1
     * @param b value 2
     * @param delta the maximum delta
     * @return true if abs(a-b) < delta
     */
    public static boolean doubleEqualWithinDelta(double a, double b, double delta) {
        return Math.abs(a - b) < delta;
    }

    /**
     * Implements {@link Comparator#compare(Object, Object)} for integers.
     * 
     * @param val1 value 1
     * @param val2 value 2
     * @return see {@link Comparator#compare(Object, Object)}
     */
    @Deprecated // as we use Java 7+, we can replace this with Integer.compare()
    public static int compareInt(int val1, int val2) {
        // Note: this is rather verbose as Checkstyle forbids the conditional operator ("a?b:c")
        if (val1 > val2) {
            return POSITIVE_INT;
        }
        if (val1 < val2) {
            return NEGATIVE_INT;
        }
        return 0;
    }

    /**
     * Implements {@link Comparator#compare(Object, Object)} for integers.
     * 
     * @param val1 value 1
     * @param val2 value 2
     * @return see {@link Comparator#compare(Object, Object)}
     */
    @Deprecated // as we use Java 7+, we can replace this with Long.compare()
    public static int compareLong(long val1, long val2) {
        // Note: this is rather verbose as Checkstyle forbids the conditional operator ("a?b:c")
        if (val1 > val2) {
            return POSITIVE_INT;
        }
        if (val1 < val2) {
            return NEGATIVE_INT;
        }
        return 0;
    }
}
