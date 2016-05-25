/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.concurrent.Future;

import de.rcenvironment.core.component.execution.api.Component;

/**
 * Handles permits for execution of components. This allows the limitation of the count of parallel component executions.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionPermitsService {

    /**
     * Acquires a permit to execute given component, blocking until one is available, or the thread is interrupted. A permit must be
     * acquired before calling {@link Component#start(de.rcenvironment.core.component.execution.api.ComponentContext)} or
     * {@link Component#processInputs()}.
     * 
     * @param componentIdentifier identifier of affected component
     * @param executionIdentifier TODO
     * @return {@link Future} which blocks until permit is available; returns <code>true</code> if permission was acutally acquired,
     *         otherwise <code>false</code> (e.g. if thread was interrupted)
     */
    Future<Boolean> acquire(String componentIdentifier, String executionIdentifier);

    /**
     * Releases a permit to execute given component.
     * 
     * @param componentIdentifier identifier of affected component
     */
    void release(String componentIdentifier);
}
