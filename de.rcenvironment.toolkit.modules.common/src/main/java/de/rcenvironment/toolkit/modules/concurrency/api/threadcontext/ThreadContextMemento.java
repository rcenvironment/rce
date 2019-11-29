/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

/**
 * Abstract wrapper around the current thread's former {@link ThreadContext} when it is replaced. The {@link #restore()} method restores the
 * original {@link ThreadContext}, and may also perform internal consistency checks.
 * 
 * @author Robert Mischke
 * 
 */
public interface ThreadContextMemento {

    /**
     * Restores the original {@link ThreadContext}; may also perform internal consistency checks.
     */
    void restore();
}
