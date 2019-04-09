/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

import java.util.List;

/**
 * Utility interface to execute a group of {@link Runnable}s in parallel.
 * 
 * @author Robert Mischke
 */
public interface RunnablesGroup {

    /**
     * Adds a {@link Runnable} to execute when {@link #executeParallel()} is called.
     * 
     * @param task the new {@link Runnable}
     */
    void add(Runnable task);

    /**
     * Executes all previously added {@link Runnable}.
     * 
     * @return a list of outcomes; on normal termination, "null" is added, or the {@link RuntimeException} if one occurred
     */
    List<RuntimeException> executeParallel();
}
