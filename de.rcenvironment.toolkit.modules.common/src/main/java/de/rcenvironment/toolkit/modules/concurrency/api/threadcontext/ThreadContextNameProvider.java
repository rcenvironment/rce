/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;


/**
 * Defines the name of a {@link ThreadContext}. The returned name may be static (see {@link ThreadContextStaticNameProvider}), or generated
 * on the fly from context data. If the context's name contains dynamic aspects, and is not guaranteed to be used during the context's
 * lifetime, it is usually advisable to generate it on the fly.
 * 
 * @author Robert Mischke
 */
public interface ThreadContextNameProvider {

    /**
     * @param context the context to get the name for
     * @return the context's name
     */
    String getName(ThreadContext context);
}
