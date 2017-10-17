/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.utils.common;

/**
 * Default {@link System#currentTimeMillis()} implementation of {@link TimeSource}.
 *
 * @author Robert Mischke
 */
public final class DefaultTimeSource implements TimeSource {

    @Override
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
