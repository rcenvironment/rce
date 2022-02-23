/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

/**
 * A simple interface to use in place of direct calls to {@link System#currentTimeMillis()} to allow exchanging/mocking the time source
 * during unit testing.
 *
 * @author Robert Mischke
 */
public interface TimeSource {

    /**
     * @return the epoch time in milliseconds, as {@link System#currentTimeMillis()}
     */
    long getCurrentTimeMillis();
}
