/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for {@link ComparatorUtils}.
 * 
 * @author Robert Mischke
 */
public class ComparatorUtilsTest {

    private static final int MINUS_ONE = -1;

    /**
     * Test basic {@link ComparatorUtils#compareInt(int, int)} behaviour.
     */
    @Test
    public void testCompareIntBasics() {
        assertTrue(ComparatorUtils.compareInt(1, 0) > 0);
        assertTrue(ComparatorUtils.compareInt(MINUS_ONE, 0) < 0);
        assertTrue(ComparatorUtils.compareInt(MINUS_ONE, MINUS_ONE) == 0);
        assertTrue(ComparatorUtils.compareInt(1, 1) == 0);
    }

    /**
     * Test {@link ComparatorUtils#compareInt(int, int)} with boundary Integer values.
     */
    @Test
    public void testCompareIntBoundaries() {
        assertTrue(ComparatorUtils.compareInt(Integer.MAX_VALUE, 0) > 0);
        assertTrue(ComparatorUtils.compareInt(Integer.MAX_VALUE, Integer.MIN_VALUE) > 0);
        assertTrue(ComparatorUtils.compareInt(Integer.MAX_VALUE, Integer.MAX_VALUE) == 0);
        assertTrue(ComparatorUtils.compareInt(Integer.MIN_VALUE, Integer.MIN_VALUE) == 0);
        assertTrue(ComparatorUtils.compareInt(Integer.MIN_VALUE, 0) < 0);
        assertTrue(ComparatorUtils.compareInt(Integer.MIN_VALUE, Integer.MAX_VALUE) < 0);
    }

    /**
     * Test basic {@link ComparatorUtils#compareLong(long, long)} behaviour.
     */
    @Test
    public void testCompareLongBasics() {
        assertTrue(ComparatorUtils.compareLong(1, 0) > 0);
        assertTrue(ComparatorUtils.compareLong(MINUS_ONE, 0) < 0);
        assertTrue(ComparatorUtils.compareLong(MINUS_ONE, MINUS_ONE) == 0);
        assertTrue(ComparatorUtils.compareLong(1, 1) == 0);
    }

    /**
     * Test {@link ComparatorUtils#compareLong(long, long)} with boundary Long values.
     */
    @Test
    public void testCompareLongBoundaries() {
        assertTrue(ComparatorUtils.compareLong(Long.MAX_VALUE, 0) > 0);
        assertTrue(ComparatorUtils.compareLong(Long.MAX_VALUE, Long.MIN_VALUE) > 0);
        assertTrue(ComparatorUtils.compareLong(Long.MAX_VALUE, Long.MAX_VALUE) == 0);
        assertTrue(ComparatorUtils.compareLong(Long.MIN_VALUE, Long.MIN_VALUE) == 0);
        assertTrue(ComparatorUtils.compareLong(Long.MIN_VALUE, 0) < 0);
        assertTrue(ComparatorUtils.compareLong(Long.MIN_VALUE, Long.MAX_VALUE) < 0);
    }

}
