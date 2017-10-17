/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api.threadcontext;

/**
 * A static holder to set or access the {@link ThreadContext} of the current thread.
 * 
 * @author Robert Mischke
 */
public final class ThreadContextHolder {

    private static final ThreadLocal<ThreadContext> sharedThreadLocal = new ThreadLocal<>();

    /**
     * Internal {@link ThreadContextMemento} implementation.
     * 
     * @author Robert Mischke
     */
    private static final class ThreadContextMementoImpl implements ThreadContextMemento {

        private final ThreadContext savedContext;

        private final ThreadContext newContext;

        ThreadContextMementoImpl(ThreadContext previous, ThreadContext newContext) {
            this.savedContext = previous;
            this.newContext = newContext;
        }

        @Override
        public void restore() {
            if (sharedThreadLocal.get() != newContext) {
                throw new IllegalStateException(
                    "Consistency violation: Found a different "
                        + ThreadContext.class.getSimpleName()
                        + " instance than the one that was set when this "
                        + ThreadContextMemento.class.getSimpleName()
                        + " was created. Most likely, a subsequent context change operation was not unwound "
                        + "after its nested operation was finished.");
            }
            sharedThreadLocal.set(savedContext);
        }

    }

    private ThreadContextHolder() {}

    /**
     * @return the {@link ThreadContext} of the current thread, or null if none exist
     */
    public static ThreadContext getCurrentContext() {
        return sharedThreadLocal.get(); // may be null
    }

    /**
     * Convenience method equivalent to {@link #getCurrentContext()}.{@link #getAspect(Class)}.
     * 
     * @param <T> the type of the aspect object to retrieve
     * @param aspectClass the type of the aspect object to retrieve
     * @return the selected aspect of the current thread's {@link ThreadContext}, or null if no context exists at all, or if it does not
     *         contain the selected aspect
     */
    public static <T> T getCurrentContextAspect(Class<T> aspectClass) {
        final ThreadContext threadContext = sharedThreadLocal.get();
        if (threadContext == null) {
            return null;
        }
        return threadContext.getAspect(aspectClass); // may be null
    }

    /**
     * @param newContext the new {@link ThreadContext} to attach to the current thread
     * @return the previous {@link ThreadContext} of the current thread, or null if none existed
     */
    public static ThreadContextMemento setCurrentContext(ThreadContext newContext) {
        final ThreadContext savedContext = sharedThreadLocal.get(); // may be null
        sharedThreadLocal.set(newContext);
        return new ThreadContextMementoImpl(savedContext, newContext);
    }

}
