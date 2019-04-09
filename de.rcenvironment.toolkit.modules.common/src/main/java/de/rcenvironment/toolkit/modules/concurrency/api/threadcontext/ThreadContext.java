/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

/**
 * An interface representing a generic context that can be attached to threads, and is typically inherited by threads spawned from that
 * thread.
 * <p>
 * Additionally, a single context object is able to transport arbitrary and independent objects (which must, however, be thread-safe to
 * use). These data entries are referred to as the "aspects" of a {@link ThreadContext}. Conceptually, each context acts like a
 * class-to-object map where objects can be registered for one of their super-types (typically, an implemented interface).
 * <p>
 * To make the context transfer between threads efficient, all {@link ThreadContext} implementations must be immutable. To create a
 * {@link ThreadContext}, or to derive one from an existing one, see the {@link ThreadContextBuilder} class.
 * 
 * @author Robert Mischke
 */
public interface ThreadContext {

    /**
     * @param <T> the type of the aspect object to retrieve
     * @param aspectClass the type of the aspect object to retrieve
     * @return the aspect object matching the interface if one exist, otherwise null
     */
    <T> T getAspect(Class<T> aspectClass);
}
