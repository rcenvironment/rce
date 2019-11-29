/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

/**
 * Simple mock {@link TimeSource}. Note that this class is intentionally NOT synchronized to avoid introducing threading side effects into
 * tests.
 *
 * @author Robert Mischke
 */
public class MockTimeSource implements TimeSource {

    private long currentMockTime;

    public MockTimeSource() {
        this.currentMockTime = 0L;
    }

    public MockTimeSource(long currentMockTime) {
        this.currentMockTime = currentMockTime;
    }

    @Override
    public long getCurrentTimeMillis() {
        return currentMockTime;
    }

    /**
     * Sets/replaces the internal mock time..
     * 
     * @param currentMockTime the new time to set
     */
    public void setCurrentMockTime(long currentMockTime) {
        this.currentMockTime = currentMockTime;
    }

    /**
     * Adds the given time delta the the internal mock time.
     * 
     * @param delta the milliseconds value to add to the current mock time
     */
    public void adjustMockTime(long delta) {
        this.currentMockTime += delta;
    }
}
