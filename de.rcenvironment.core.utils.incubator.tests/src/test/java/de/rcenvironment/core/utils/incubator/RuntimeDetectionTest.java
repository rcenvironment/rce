/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
