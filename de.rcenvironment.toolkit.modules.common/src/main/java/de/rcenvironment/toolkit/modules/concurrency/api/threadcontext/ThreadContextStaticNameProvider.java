/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
