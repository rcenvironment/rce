/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
