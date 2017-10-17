/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.incubator;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link RuntimeDetection}.
 *
 * @author Tobias Brieden
 */
public class RuntimeDetectionTest {

    /**
     * Tests if the execution of a test is correctly detected.
     *
     */
    @Test
    public void test() {
        assertTrue(RuntimeDetection.isRunningAsTest());
    }
}
