/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
