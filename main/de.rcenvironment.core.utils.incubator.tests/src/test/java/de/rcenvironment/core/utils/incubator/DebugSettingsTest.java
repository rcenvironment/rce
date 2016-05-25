/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for {@link DebugSettings} (currently, the verbose logging configuration).
 * 
 * @author Robert Mischke
 */
public class DebugSettingsTest {

    private static final String ASTERISK = "*";

    private static final String ASTERISK_AND_DOT = "*.";

    private static final Class<DebugSettingsTest> TEST_CLASS = DebugSettingsTest.class;

    private static final String TEST_CLASS_NAME = TEST_CLASS.getSimpleName();

    /**
     * Initialization before the first test is run.
     */
    @BeforeClass
    public static void setUpOnce() {
        // this is to bring the singleton instance into a defined state before testing with non-singleton instances
        System.clearProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY);
        // this also tests that no problematic constant is set that may interfere with the test run
        assertFalse(DebugSettings.getVerboseLoggingEnabled(TEST_CLASS));
        assertFalse(DebugSettings.getVerboseLoggingEnabled(String.class));
    }

    /**
     * Tests the default case.
     */
    @Test
    public void testNoSystemProperty() {
        System.clearProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY);
        DebugSettings testInstance = new DebugSettings();
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
    }

    /**
     * Tests the pattern "*".
     */
    @Test
    public void testCorrectSystemProperty1() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, ASTERISK);
        DebugSettings testInstance = new DebugSettings();
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(String.class));
    }

    /**
     * Tests a specific class name.
     */
    @Test
    public void testCorrectSystemProperty2() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, ASTERISK_AND_DOT + TEST_CLASS_NAME);
        DebugSettings testInstance = new DebugSettings();
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(String.class));
    }

    /**
     * Tests a specific class name's part, WITHOUT a wildcard at the end, so it should NOT match.
     */
    @Test
    public void testCorrectSystemProperty3() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, ASTERISK_AND_DOT + TEST_CLASS_NAME.substring(0, 5));
        DebugSettings testInstance = new DebugSettings();
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(String.class));
    }

    /**
     * Tests a specific class name's part, WITH a wildcard at the end, so it should match.
     */
    @Test
    public void testCorrectSystemProperty4() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, ASTERISK_AND_DOT + TEST_CLASS_NAME.substring(0, 5)
            + ASTERISK);
        DebugSettings testInstance = new DebugSettings();
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(String.class));
    }

    /**
     * Tests a pattern with multiple name patterns.
     */
    @Test
    public void testCorrectMultiClassSystemProperty() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, ASTERISK_AND_DOT + TEST_CLASS_NAME
            + ",java.lang.String,*.Integer,*.Array");
        DebugSettings testInstance = new DebugSettings();
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(String.class));
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(Integer.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(BigInteger.class));
        assertTrue(testInstance.getVerboseLoggingEnabledInternal(Array.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(ArrayList.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(Number.class));
    }

    /**
     * Tests a broken pattern.
     */
    @Test
    public void testBrokenPattern() {
        System.setProperty(DebugSettings.VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY, "(");
        DebugSettings testInstance = new DebugSettings();
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(TEST_CLASS));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(String.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(Integer.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(BigInteger.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(Array.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(ArrayList.class));
        assertFalse(testInstance.getVerboseLoggingEnabledInternal(Number.class));
    }
}
