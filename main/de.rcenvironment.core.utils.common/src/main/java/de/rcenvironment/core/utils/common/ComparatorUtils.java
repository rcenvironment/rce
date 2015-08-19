/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

/**
 * Provides common utility methods for {@link Comparator} writing.
 * 
 * @author Robert Mischke
 */
public abstract class ComparatorUtils {

    /**
     * A constant for returning a positive integer in {@link Comparator#compare(Object, Object)}.
     * Usually needed to satisfy Checkstyle.
     */
    public static final int POSITIVE_INT = 1;

    /**
     * A constant for returning a negative integer in {@link Comparator#compare(Object, Object)}.
     * Usually needed to satisfy Checkstyle.
     */
    public static final int NEGATIVE_INT = -1;

    private ComparatorUtils() {}

    /**
     * Implements {@link Comparator#compare(Object, Object)} for integers.
     * 
     * @param val1 value 1
     * @param val2 value 2
     * @return see {@link Comparator#compare(Object, Object)}
     */
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
