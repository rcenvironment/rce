/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
