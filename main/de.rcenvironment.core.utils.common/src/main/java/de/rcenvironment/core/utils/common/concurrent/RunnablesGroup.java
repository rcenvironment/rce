/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

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
