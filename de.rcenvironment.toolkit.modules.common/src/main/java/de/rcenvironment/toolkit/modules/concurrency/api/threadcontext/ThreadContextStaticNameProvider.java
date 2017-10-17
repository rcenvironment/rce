/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

/**
 * A simple holder for a {@link ThreadContext}'s static name.
 * 
 * @author Robert Mischke
 */
public final class ThreadContextStaticNameProvider implements ThreadContextNameProvider {

    private final String name;

    public ThreadContextStaticNameProvider(String name) {
        this.name = name;
    }

    @Override
    public String getName(ThreadContext context) {
        return name;
    }

}
